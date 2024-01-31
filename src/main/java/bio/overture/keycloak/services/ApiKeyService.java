package bio.overture.keycloak.services;

import static bio.overture.keycloak.utils.Constants.SORT_ORDER_ASC;
import static bio.overture.keycloak.utils.Converters.jsonStringToClass;
import static bio.overture.keycloak.utils.Dates.isExpired;
import static bio.overture.keycloak.utils.Dates.keyExpirationDate;
import static java.util.stream.Collectors.toList;
import static org.keycloak.common.util.ObjectUtil.isBlank;

import bio.overture.keycloak.model.ApiKey;
import bio.overture.keycloak.params.ScopeName;
import bio.overture.keycloak.utils.Hasher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.ws.rs.BadRequestException;
import java.util.*;
import lombok.NonNull;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;

public class ApiKeyService {

  private KeycloakSession session;
  private EntityManager entityManager;

  private Hasher hasher;

  private static final Logger logger = Logger.getLogger(ApiKeyService.class);

  private static final String API_KEYS_ATTRIBUTE = "api-keys";

  public ApiKeyService(KeycloakSession session) {
    this.session = session;
    this.entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    this.hasher = new Hasher();
  }

  public List<ApiKey> getApiKeys(
      @NonNull UserEntity user,
      String query,
      int limit,
      int offset,
      String sort,
      String sortOrder) {

    if (user.getAttributes() == null
        || user.getAttributes().stream()
            .noneMatch(attribute -> attribute.getName().equals(API_KEYS_ATTRIBUTE))) {
      return Collections.emptyList();
    }

    if (!isBlank(query)) {
      validFormatApiKey(query);
    }

    return user.getAttributes().stream()
        .filter(attribute -> attribute.getName().equals(API_KEYS_ATTRIBUTE))
        .map(this::parseApiKey)
        .filter(filterByApiKeyName(query))
        .sorted(findComparator(sort, sortOrder))
        .map(this::hideApiKeyvalue)
        .skip(offset)
        .limit(limit)
        .collect(toList());
  }

  private java.util.function.Predicate<ApiKey> filterByApiKeyName(String query) {
    return value -> !query.isEmpty() ? value.getName().equals(hasher.generateHash(query)) : true;
  }

  private Comparator<ApiKey> findComparator(String sort, String sortOrder) {
    Comparator<ApiKey> comparator;
    switch (sort.toUpperCase()) {
      case "EXPIRYDATE":
        comparator =
            sortOrder.equalsIgnoreCase(SORT_ORDER_ASC)
                ? ApiKey.byExpiryDate
                : ApiKey.byExpiryDate.reversed();
        break;
      case "ISSUEDATE":
        comparator =
            sortOrder.equalsIgnoreCase(SORT_ORDER_ASC)
                ? ApiKey.byIssueDate
                : ApiKey.byIssueDate.reversed();
        break;
      case "ISREVOKED":
        comparator =
            sortOrder.equalsIgnoreCase(SORT_ORDER_ASC)
                ? ApiKey.byRevoked
                : ApiKey.byRevoked.reversed();
        break;
      case "DESCRIPTION":
        comparator =
            sortOrder.equalsIgnoreCase(SORT_ORDER_ASC)
                ? ApiKey.byDescription
                : ApiKey.byDescription.reversed();
        break;
      default:
        comparator =
            sortOrder.equalsIgnoreCase(SORT_ORDER_ASC) ? ApiKey.byName : ApiKey.byName.reversed();
        break;
    }
    return comparator;
  }

  public ApiKey issueApiKey(
      @NonNull String userId, @NonNull List<ScopeName> scopes, String description) {

    // generate the apiKey value
    String apiKeyName = UUID.randomUUID().toString();

    // hash the apiKey value
    String hashedApiKeyName = hasher.generateHash(apiKeyName);

    ApiKey apiKey =
        ApiKey.builder()
            .name(hashedApiKeyName)
            .scope(new HashSet<>(scopes))
            .description(description)
            .issueDate(new Date())
            .expiryDate(keyExpirationDate())
            .isRevoked(false)
            .build();

    setApiKey(userId, apiKey);

    // return the non-hashed apiKey name
    apiKey.setName(apiKeyName);

    return apiKey;
  }

  public ApiKey revokeApiKey(@NonNull UserEntity user, String apiKeyName) {

    validFormatApiKey(apiKeyName);

    Optional<UserAttributeEntity> foundAttribute =
        entityManager.find(UserEntity.class, user.getId()).getAttributes().stream()
            .filter(
                attribute ->
                    parseApiKey(attribute).getName().equals(hasher.generateHash(apiKeyName)))
            .findFirst();

    if (foundAttribute.isEmpty()) {
      throw new BadRequestException("No ApiKey found");
    }

    return revokeApiKeyAttribute(foundAttribute.get());
  }

  public Optional<UserAttributeEntity> findByApiKeyAttribute(String apiKeyName) {

    validFormatApiKey(apiKeyName);

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<UserAttributeEntity> cq = cb.createQuery(UserAttributeEntity.class);
    Root<UserAttributeEntity> root = cq.from(UserAttributeEntity.class);

    Predicate searchName = cb.equal(root.get("name"), API_KEYS_ATTRIBUTE);

    cq.where(cb.and(searchName));

    TypedQuery<UserAttributeEntity> query = entityManager.createQuery(cq);

    return query.getResultList().stream()
        .filter(
            attribute -> parseApiKey(attribute).getName().equals(hasher.generateHash(apiKeyName)))
        .findFirst();
  }

  public ApiKey parseApiKey(UserAttributeEntity attributeApiKey) {
    return jsonStringToClass(attributeApiKey.getValue(), ApiKey.class);
  }

  private ApiKey hideApiKeyvalue(ApiKey apiKey) {
    apiKey.setName(null);
    return apiKey;
  }

  public String checkApiResponseMessage(ApiKey apiKey) {
    String message = null;

    if (isExpired(apiKey.getExpiryDate())) {
      message = "ApiKey is expired";
    } else if (apiKey.getIsRevoked()) {
      message = "ApiKey is revoked";
    }
    return message;
  }

  public boolean isValidApiKey(ApiKey apiKey) {
    return !isExpired(apiKey.getExpiryDate()) && !apiKey.getIsRevoked();
  }

  private ApiKey setApiKey(String userId, ApiKey apiKey) {

    UserEntity userEntity = entityManager.find(UserEntity.class, userId);
    UserAttributeEntity attributeEntity = new UserAttributeEntity();
    attributeEntity.setName(API_KEYS_ATTRIBUTE);
    attributeEntity.setValue(apiKey.toJsonMinimal());
    attributeEntity.setUser(userEntity);
    attributeEntity.setId(UUID.randomUUID().toString());
    entityManager.persist(attributeEntity);

    return apiKey;
  }

  private ApiKey revokeApiKeyAttribute(UserAttributeEntity attribute) {

    ApiKey editApiKey = parseApiKey(attribute);
    editApiKey.setIsRevoked(true);

    attribute.setValue(editApiKey.toJsonMinimal());

    entityManager.persist(attribute);

    // return null apiKey Name
    editApiKey.setName(null);

    return editApiKey;
  }

  private void validFormatApiKey(String apiKey) {

    if (apiKey == null || apiKey.isEmpty()) {
      throw new BadRequestException("ApiKey cannot be empty.");
    }

    try {
      UUID.fromString(apiKey);
      // able to parse as UUID, so this is a valid UUID
    } catch (IllegalArgumentException e) {
      // unable to parse as UUID
      throw new BadRequestException("Invalid apiKey format");
    }
  }
}
