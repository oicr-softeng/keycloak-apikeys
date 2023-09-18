package bio.overture.keycloak.resource;

import bio.overture.keycloak.model.ApiKey;
import bio.overture.keycloak.model.dto.ApiKeyResponse;
import bio.overture.keycloak.services.UserService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.Set;
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
      @QueryParam("user_id") String user_id,
      @QueryParam("query") String query,
      @DefaultValue("20") @QueryParam("limit") int limit,
      @DefaultValue("0") @QueryParam("offset") int offset,
      @QueryParam("sort") String sort,
      @QueryParam("sortOrder") String sortOrder
  ){
    logger.info("listApiKeys for user_id:" + user_id);
    UserModel user = userService.getUserById(user_id);
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
  public Response issueApiKey(){
    logger.info("issueApiKey");
    AuthenticationManager.AuthResult auth = userService.checkAuth();

    return Response
        .ok()
        .build();
  }

}
