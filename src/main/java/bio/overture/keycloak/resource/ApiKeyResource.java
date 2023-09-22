package bio.overture.keycloak.resource;

import bio.overture.keycloak.model.ApiKey;
import bio.overture.keycloak.model.dto.ApiKeyResponse;
import bio.overture.keycloak.model.dto.CheckApiKeyResponse;
import bio.overture.keycloak.services.ApiKeyService;
import bio.overture.keycloak.services.UserService;
import bio.overture.keycloak.params.ScopeName;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.*;

import static bio.overture.keycloak.utils.CollectionUtils.mapToList;

public class ApiKeyResource {

  private final KeycloakSession session;
  private final UserService userService;
  private final ApiKeyService apiKeyService;

  public ApiKeyResource(KeycloakSession session){
    this.session = session;
    this.userService = new UserService(session);
    this.apiKeyService = new ApiKeyService(session);
  }
  private static final Logger logger = Logger.getLogger(ApiKeyResource.class);

  @GET
  @Path("api_key")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listApiKeys(
      @QueryParam("user_id") String userId,
      @DefaultValue("") @QueryParam("query") String query,
      @DefaultValue("20") @QueryParam("limit") int limit,
      @DefaultValue("0") @QueryParam("offset") int offset,
      @DefaultValue("name") @QueryParam("sort") String sort,
      @DefaultValue("ASC") @QueryParam("sortOrder") String sortOrder
  ){
    logger.info("GET /api_key  user_id:" + userId + ", query:" + query);

    AuthenticationManager.AuthResult auth = userService.checkAuth();

    UserEntity user = userService.getUserById(userId);

    userService.validateIsSameUser(auth, user);

    List<ApiKey> keys = apiKeyService.getApiKeys(user, query, limit, offset, sort, sortOrder);

    return Response
        .ok(ApiKeyResponse
            .builder()
            .limit(limit)
            .offset(offset)
            .count(keys.size())
            .resultSet(keys)
            .build()
        ).build();
  }


  @POST
  @Path("api_key")
  @Produces(MediaType.APPLICATION_JSON)
  public Response issueApiKey(
      @QueryParam(value="user_id") String userId,
      @QueryParam(value="scopes") ArrayList<String> scopes,
      @QueryParam(value="description") String description
  ){
    logger.info("POST /api_key  user_id:" + userId + ", scopes:" + scopes);

    AuthenticationManager.AuthResult auth = userService.checkAuth();

    UserEntity user = userService.getUserById(userId);

    userService.validateIsSameUserOrAdmin(auth, user);

    List<ScopeName> scopeNames = mapToList(scopes, ScopeName::new);

    ApiKey apiKey = apiKeyService.issueApiKey(userId, scopeNames, description);

    return Response
        .ok(apiKey)
        .build();
  }

  @DELETE
  @Path("api_key")
  @Produces(MediaType.APPLICATION_JSON)
  public Response revokeApiKey(@QueryParam(value="apiKey") String apiKey){
    logger.info("DELETE /api_key  apiKey:" + apiKey);

    AuthenticationManager.AuthResult auth = userService.checkAuth();

    Optional<UserAttributeEntity> foundApiKey = apiKeyService.findByApiKeyAttribute(apiKey);

    if(foundApiKey.isEmpty()){
      throw new BadRequestException("ApiKey not found");
    }

    UserEntity ownerApiKey = foundApiKey.get().getUser();

    userService.validateIsSameUserOrAdmin(auth, ownerApiKey);

    ApiKey revokedApiKey = apiKeyService.revokeApiKey(ownerApiKey, apiKey);

    return Response
        .ok(revokedApiKey)
        .build();
  }

  @POST
  @Path("check_api_key")
  @Produces(MediaType.APPLICATION_JSON)
  public Response checkApiKey(
      @QueryParam(value="apiKey") String apiKey
  ){
    logger.info("POST /check_api_key  apiKey:" + apiKey);

    AuthenticationManager.AuthResult auth = userService.checkAuth();

    Optional<UserAttributeEntity> foundApiKey = apiKeyService.findByApiKeyAttribute(apiKey);

    if(foundApiKey.isEmpty()){
      throw new BadRequestException("ApiKey not found");
    }

    UserEntity ownerApiKey = foundApiKey.get().getUser();

    userService.validateIsSameUser(auth, ownerApiKey);

    ApiKey parsedApiKey = apiKeyService.parseApiKey(foundApiKey.get());

    return Response
        .ok(CheckApiKeyResponse
            .builder()
            .user_id(ownerApiKey.getId())
            .exp(parsedApiKey.getExpiryDate().getTime())
            .isValid(apiKeyService.isValidApiKey(parsedApiKey))
            .message(apiKeyService.checkApiResponseMessage(parsedApiKey))
            .isRevoked(parsedApiKey.getIsRevoked())
            .scope(apiKeyService.isValidApiKey(parsedApiKey) ? parsedApiKey.getScope() : null)
            .build())
        .build();
  }

}
