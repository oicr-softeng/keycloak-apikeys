package bio.overture.keycloak.resource;

import bio.overture.keycloak.model.ApiKey;
import bio.overture.keycloak.model.dto.ApiKeyResponse;
import bio.overture.keycloak.model.dto.CheckApiKeyResponse;
import bio.overture.keycloak.services.UserService;
import bio.overture.keycloak.params.ScopeName;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.NonNull;
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.*;

import static bio.overture.keycloak.utils.CollectionUtils.mapToList;

public class ApiKeyResource {

  private final KeycloakSession session;
  private final UserService userService;

  public ApiKeyResource(KeycloakSession session){
    this.session = session;
    this.userService = new UserService(session);
  }
  private static final Logger logger = Logger.getLogger(ApiKeyResource.class);

  @GET
  @Path("api_key")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listApiKeys(
      @NonNull @QueryParam("user_id") String userId,
      @QueryParam("query") String query,
      @DefaultValue("20") @QueryParam("limit") int limit,
      @DefaultValue("0") @QueryParam("offset") int offset,
      @QueryParam("sort") String sort,
      @QueryParam("sortOrder") String sortOrder
  ){
    logger.info("GET /api_key  user_id:" + userId + ", query:" + query);

    AuthenticationManager.AuthResult auth = userService.checkAuth();

    UserModel user = userService.getUserById(userId);
    Set<ApiKey> keys = userService.getApiKeys(user);

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

    List<ScopeName> scopeNames = mapToList(scopes, ScopeName::new);

    UserModel user = userService.getUserById(userId);

    ApiKey apiKey = userService.issueApiKey(user, scopeNames, description);

    return Response
        .ok(apiKey)
        .build();
  }

  @DELETE
  @Path("api_key")
  @Produces(MediaType.APPLICATION_JSON)
  public Response revokeApiKey(@QueryParam(value="apiKey") String apiKey){
    logger.info("DELETE /api_key  apiKey:" + apiKey);

    UserModel user = userService.checkAuth().getUser();

    ApiKey revokedApiKey = userService.revokeApiKey(user, apiKey);

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

    UserModel user = userService.checkAuth().getUser();

    Optional<ApiKey> foundApiKey = userService.findApiKey(user, apiKey);

    userService.isApiKeyValid(foundApiKey);

    return Response
        .ok(CheckApiKeyResponse
            .builder()
            .user_id(user.getId())
            .exp(foundApiKey.get().getExpiryDate().getTime())
            .scope(foundApiKey.get().getScope())
            .build())
        .build();
  }

}
