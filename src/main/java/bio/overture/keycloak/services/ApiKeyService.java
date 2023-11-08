package bio.overture.keycloak.services;

import bio.overture.keycloak.model.ApiKey;
import bio.overture.keycloak.params.ScopeName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.ws.rs.BadRequestException;
import lombok.NonNull;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;

import java.util.*;

import static bio.overture.keycloak.utils.Constants.SORT_ORDER_ASC;
import static bio.overture.keycloak.utils.Converters.jsonStringToClass;
import static bio.overture.keycloak.utils.Dates.isExpired;
import static bio.overture.keycloak.utils.Dates.keyExpirationDate;
import static java.util.stream.Collectors.toList;

public class ApiKeyService {

  private KeycloakSession session;
  private EntityManager entityManager;

  private static final Logger logger = Logger.getLogger(ApiKeyService.class);

  private static final String API_KEYS_ATTRIBUTE = "api-keys";

  public ApiKeyService(KeycloakSession session) {
    this.session = session;
    this.entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
  }

  public List<ApiKey> getApiKeys(@NonNull UserEntity user, String query, int limit, int offset, String sort, String sortOrder){

    if(user.getAttributes() == null
        || user.getAttributes().stream().noneMatch(attribute -> attribute.getName().equals(API_KEYS_ATTRIBUTE))) {
      return Collections.emptyList();
    }

    return user
        .getAttributes()
        .stream()
        .filter(attribute -> attribute.getName().equals(API_KEYS_ATTRIBUTE))
        .map(this::parseApiKey)
        .filter(value -> !query.isEmpty() ? value.getName().equals(query) : true)
        .sorted(findComparator(sort, sortOrder))
        .skip(offset)
        .limit(limit)
        .collect(toList());
  }

  public Comparator<ApiKey> findComparator(String sort, String sortOrder){
    Comparator<ApiKey> comparator;
    switch (sort.toUpperCase()) {
      case  "EXPIRYDATE":
        comparator = sortOrder.equalsIgnoreCase(SORT_ORDER_ASC) ? ApiKey.byExpiryDate : ApiKey.byExpiryDate.reversed();
        break;
      case  "ISSUEDATE":
        comparator = sortOrder.equalsIgnoreCase(SORT_ORDER_ASC) ? ApiKey.byIssueDate : ApiKey.byIssueDate.reversed();
        break;
      case  "ISREVOKED":
        comparator = sortOrder.equalsIgnoreCase(SORT_ORDER_ASC) ? ApiKey.byRevoked : ApiKey.byRevoked.reversed();
        break;
      case  "DESCRIPTION":
        comparator = sortOrder.equalsIgnoreCase(SORT_ORDER_ASC) ? ApiKey.byDescription : ApiKey.byDescription.reversed();
        break;
      default:
        comparator = sortOrder.equalsIgnoreCase(SORT_ORDER_ASC) ? ApiKey.byName : ApiKey.byName.reversed();
        break;
    }
    return comparator;
  }

  public ApiKey issueApiKey(@NonNull String userId ,
                            @NonNull List<ScopeName> scopes,
                            String description){

    ApiKey apiKey = ApiKey
        .builder()
        .name(UUID.randomUUID().toString())
        .scope(new HashSet<>(scopes))
        .description(description)
        .issueDate(new Date())
        .expiryDate(keyExpirationDate())
        .isRevoked(false)
        .build();

    setApiKey(userId, apiKey);

    return apiKey;
  }

  public ApiKey revokeApiKey(@NonNull UserEntity user, String apiKeyName){

    validFormatApiKey(apiKeyName);

    Optional<UserAttributeEntity> foundAttribute = entityManager
        .find(UserEntity.class, user.getId())
        .getAttributes()
        .stream()
        .filter(attribute -> parseApiKey(attribute).getName().equals(apiKeyName))
        .findFirst();

    if(foundAttribute.isEmpty()){
      throw new BadRequestException("No ApiKey found");
    }

    return revokeApiKeyAttribute(foundAttribute.get());
  }

  public Optional<UserAttributeEntity> findByApiKeyAttribute(String apiKeyName){
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<UserAttributeEntity> cq = cb.createQuery(UserAttributeEntity.class);
    Root<UserAttributeEntity> root = cq.from(UserAttributeEntity.class);

    Predicate searchName = cb.equal(root.get("name"), API_KEYS_ATTRIBUTE);

    cq.where(cb.and(searchName));

    TypedQuery<UserAttributeEntity> query = entityManager.createQuery(cq);

    return query
        .getResultList()
        .stream()
        .filter(attribute -> parseApiKey(attribute).getName().equals(apiKeyName))
        .findFirst();

  }

  public ApiKey parseApiKey(UserAttributeEntity attributeApiKey){
    return jsonStringToClass(attributeApiKey.getValue(), ApiKey.class);
  }

  public String checkApiResponseMessage(ApiKey apiKey){
    String message = null;

    if(isExpired(apiKey.getExpiryDate())){
      message = "ApiKey is expired";
    } else if (apiKey.getIsRevoked()) {
      message = "ApiKey is revoked";
    }
    return message;
  }

  public boolean isValidApiKey(ApiKey apiKey){
    return !isExpired(apiKey.getExpiryDate()) && !apiKey.getIsRevoked();
  }

  private ApiKey setApiKey(String userId, ApiKey apiKey){

    UserEntity userEntity = entityManager.find(UserEntity.class, userId);
    UserAttributeEntity attributeEntity = new UserAttributeEntity();
    attributeEntity.setName(API_KEYS_ATTRIBUTE);
    attributeEntity.setValue(apiKey.toJsonMinimal());
    attributeEntity.setUser(userEntity);
    attributeEntity.setId(UUID.randomUUID().toString());
    entityManager.persist(attributeEntity);

    return apiKey;
  }

  private ApiKey revokeApiKeyAttribute(UserAttributeEntity attribute){

    ApiKey editApiKey = parseApiKey(attribute);
    editApiKey.setIsRevoked(true);

    attribute.setValue(editApiKey.toJsonMinimal());

    entityManager.persist(attribute);

    return editApiKey;
  }

  private void validFormatApiKey(String apiKey){

    if (apiKey == null || apiKey.isEmpty()) {
      throw new BadRequestException("ApiKey cannot be empty.");
    }

    if (apiKey.length() > 2048) {
      throw new BadRequestException(
          "Invalid apiKey, the maximum length for an apiKey is 2048.");
    }
  }
}
