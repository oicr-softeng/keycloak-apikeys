package bio.overture.keycloak.resource;

import static bio.overture.keycloak.utils.CollectionUtils.mapToList;

import bio.overture.keycloak.model.ApiKey;
import bio.overture.keycloak.model.dto.ApiKeyResponse;
import bio.overture.keycloak.model.dto.CheckApiKeyResponse;
import bio.overture.keycloak.params.ScopeName;
import bio.overture.keycloak.services.ApiKeyService;
import bio.overture.keycloak.services.AuthService;
import bio.overture.keycloak.services.UserService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import lombok.SneakyThrows;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.services.managers.AuthenticationManager;

public class ApiKeyResource {

  private final KeycloakSession session;
  private final UserService userService;
  private final ApiKeyService apiKeyService;
  private final AuthService authService;

  public ApiKeyResource(KeycloakSession session) {
    this.session = session;
    this.userService = new UserService(session);
    this.apiKeyService = new ApiKeyService(session);
    this.authService = new AuthService(session);
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
      @DefaultValue("ASC") @QueryParam("sortOrder") String sortOrder) {
    logger.info("GET /api_key  user_id:" + userId + ", query:" + query);

    AuthenticationManager.AuthResult auth = authService.checkBearerAuth();

    UserEntity user = userService.getUserById(userId);

    authService.validateIsSameUser(auth, user);

    List<ApiKey> keys = apiKeyService.getApiKeys(user, query, limit, offset, sort, sortOrder);

    return Response.ok(
            ApiKeyResponse.builder()
                .limit(limit)
                .offset(offset)
                .count(keys.size())
                .resultSet(keys)
                .build()
                .toString())
        .build();
  }

  @POST
  @Path("api_key")
  @Produces(MediaType.APPLICATION_JSON)
  public Response issueApiKey(
      @QueryParam(value = "user_id") String userId,
      @QueryParam(value = "scopes") ArrayList<String> scopes,
      @QueryParam(value = "description") String description) {
    logger.info("POST /api_key  user_id:" + userId + ", scopes:" + scopes);

    AuthenticationManager.AuthResult auth = authService.checkBearerAuth();

    UserEntity user = userService.getUserById(userId);

    authService.validateIsSameUserOrAdmin(auth, user);

    List<ScopeName> scopeNames = mapToList(scopes, ScopeName::new);

    authService.validatePermissions(auth, scopeNames);

    ApiKey apiKey = apiKeyService.issueApiKey(userId, scopeNames, description);

    return Response.ok(apiKey.toString()).build();
  }

  @DELETE
  @Path("api_key")
  @Produces(MediaType.APPLICATION_JSON)
  public Response revokeApiKey(@QueryParam(value = "apiKey") String apiKey) {
    logger.info("DELETE /api_key  apiKey:" + apiKey);

    AuthenticationManager.AuthResult auth = authService.checkBearerAuth();

    Optional<UserAttributeEntity> foundApiKey = apiKeyService.findByApiKeyAttribute(apiKey);

    if (foundApiKey.isEmpty()) {
      throw new BadRequestException("ApiKey not found");
    }

    UserEntity ownerApiKey = foundApiKey.get().getUser();

    authService.validateIsSameUserOrAdmin(auth, ownerApiKey);

    ApiKey revokedApiKey = apiKeyService.revokeApiKey(ownerApiKey, apiKey);

    return Response.ok(revokedApiKey.toString()).build();
  }

  @SneakyThrows
  @POST
  @Path("check_api_key")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response checkApiKey(MultipartFormDataInput formDataInput) {
    String apiKey = formDataInput.getFormDataPart("apiKey", String.class, null);
    logger.info("POST /check_api_key  apiKey:" + apiKey);

    Object authObject = authService.checkBearerOrBasicAuth();
    if (authObject == null) {
      throw new NotAuthorizedException("Authentication not valid");
    }

    Optional<UserAttributeEntity> foundApiKey = apiKeyService.findByApiKeyAttribute(apiKey);
    if (foundApiKey.isEmpty()) {
      throw new BadRequestException("ApiKey not found");
    }

    UserEntity ownerApiKey = foundApiKey.get().getUser();

    if (authObject instanceof AuthenticationManager.AuthResult) {
      authService.validateIsSameUser((AuthenticationManager.AuthResult) authObject, ownerApiKey);
    }

    ApiKey parsedApiKey = apiKeyService.parseApiKey(foundApiKey.get());

    return Response.status(207, "Multi-Status")
        .entity(
            CheckApiKeyResponse.builder()
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
