package bio.overture.keycloak.services;

import bio.overture.keycloak.model.ApiKey;
import bio.overture.keycloak.params.ScopeName;
import jakarta.ws.rs.BadRequestException;
import lombok.NonNull;
import org.jboss.logging.Logger;
import org.keycloak.models.UserModel;

import java.util.*;

import static bio.overture.keycloak.utils.Converters.jsonStringToClass;
import static bio.overture.keycloak.utils.Dates.keyExpirationDate;
import static java.util.stream.Collectors.toSet;

public class ApiKeyService {

  private static final Logger logger = Logger.getLogger(ApiKeyService.class);

  private static final String API_KEYS_ATTRIBUTE = "api-keys";

  public Set<ApiKey> getApiKeys(@NonNull UserModel user){

    if(user.getAttributes() == null
        || user.getAttributes().get(API_KEYS_ATTRIBUTE) == null) {
      return Collections.emptySet();
    }

    return user
        .getAttributeStream(API_KEYS_ATTRIBUTE)
        .map(key -> jsonStringToClass(key, ApiKey.class))
        .collect(toSet());
  }

  public ApiKey issueApiKey(@NonNull UserModel user ,
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

    setApiKey(user, apiKey);


    return apiKey;
  }

  public ApiKey revokeApiKey(@NonNull UserModel user, String apiKeyName){

    validateApiKey(apiKeyName);

    Optional<String> foundApiKey = user
        .getAttributeStream(API_KEYS_ATTRIBUTE)
        .filter(keyValueString -> jsonStringToClass(keyValueString, ApiKey.class).getName().equals(apiKeyName))
        .findFirst();

    if(foundApiKey.isEmpty()){
      throw new BadRequestException("No ApiKey found");
    }

    ApiKey parsedApiKey = jsonStringToClass(foundApiKey.get(), ApiKey.class);

    parsedApiKey.setIsRevoked(true);
    removeApiKey(user, foundApiKey.get());
    setApiKey(user, parsedApiKey);

    return parsedApiKey;
  }
  public Optional<ApiKey> findApiKey(UserModel user, String apiKey){
    return user
        .getAttributeStream(API_KEYS_ATTRIBUTE)
        .map(key -> jsonStringToClass(key, ApiKey.class))
        .filter(k -> k.getName().equals(apiKey))
        .findFirst();
  }

  private void setApiKey(UserModel user, ApiKey apiKey){
    user.getAttributes().get(API_KEYS_ATTRIBUTE).add(apiKey.toString());
  }

  private void removeApiKey(UserModel user, String apiKeyString){
    user.getAttributes().get(API_KEYS_ATTRIBUTE).remove(apiKeyString);
  }

  private void validateApiKey(String apiKey){

    if (apiKey == null || apiKey.isEmpty()) {
      throw new BadRequestException("ApiKey cannot be empty.");
    }

    if (apiKey.length() > 2048) {
      throw new BadRequestException(
          "Invalid apiKey, the maximum length for an apiKey is 2048.");
    }
  }
}
