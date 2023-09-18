package bio.overture.keycloak.services;

import bio.overture.keycloak.model.ApiKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.Collections;
import java.util.Set;

@RequiredArgsConstructor
public class UserService {

  private final KeycloakSession session;

  private static final Logger logger = Logger.getLogger(UserService.class);

  public UserModel getUserById(String user_id){
    logger.info("GET api_key - user_id:" + user_id);
    RealmModel realm = this.session.getContext().getRealm();

    UserProvider users = this.session.users();
    UserModel user = users.getUserById(realm, user_id);

    if(user == null) {
      throw new BadRequestException("User not valid");
    }
    return user;
  }

  public Set<ApiKey> getApiKeys(UserModel user){
    ObjectMapper mapper = new ObjectMapper();
    try {
      Set<ApiKey> apiKeys = mapper
          .readValue(user
                  .getAttributes()
                  .get("api-keys")
                  .toString(),
              Set.class);
      return apiKeys;
    } catch (JsonProcessingException e) {
      return Collections.emptySet();
    }
  }

  public AuthenticationManager.AuthResult checkAuth() {
    AuthenticationManager.AuthResult auth = new AppAuthManager
        .BearerTokenAuthenticator(session)
        .authenticate();

    if (auth == null) {
      throw new NotAuthorizedException("Bearer");
    } else if (auth.getToken().getIssuedFor() == null
        || !auth.getToken().isActive()
        || auth.getToken().isExpired()) {
      throw new ForbiddenException("Token is not valid");
    }
    return auth;
  }
}
