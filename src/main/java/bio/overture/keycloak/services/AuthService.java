package bio.overture.keycloak.services;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.Base64;
import java.util.Optional;
import java.util.Set;

public class AuthService {

  private final KeycloakSession session;

  private static final Logger logger = Logger.getLogger(AuthService.class);

  private final String BEARER_PREFIX = "Bearer ";
  private final String BASIC_PREFIX = "Basic ";
  private final String AUTHORIZATION_HEADER_KEY = "Authorization";
  private final String ROLE_ADMIN = "ADMIN";

  public AuthService(KeycloakSession session) {
    this.session = session;
  }

  public AuthenticationManager.AuthResult checkBearerAuth() {
    AuthenticationManager.AuthResult auth = new AppAuthManager
        .BearerTokenAuthenticator(session)
        .authenticate();

    if (auth == null) {
      throw new NotAuthorizedException("Bearer token is not valid");
    } else if (auth.getToken().getIssuedFor() == null
        || !auth.getToken().isActive()
        || auth.getToken().isExpired()) {
      throw new ForbiddenException("Bearer Token is not valid");
    }

    logger.info("AuthService - Valid Auth using Bearer token userId: " + auth.getUser().getId());
    return auth;
  }

  public ClientModel checkBasicAuth(String authorizationHeader){
    String base64Credentials = authorizationHeader.substring(BASIC_PREFIX.length()).trim();
    String credentials = new String(Base64.getDecoder().decode(base64Credentials));

    // Split credentials into username and password
    String[] parts = credentials.split(":", 2);
    String username = parts[0];
    String password = parts[1];

    return validateClientCredentials(session, username, password)
        .orElseThrow(() -> new NotAuthorizedException("Invalid credentials"));
  }

  public Object checkBearerOrBasicAuth(){
    String authorizationHeader = session.getContext().getRequestHeaders().getHeaderString(AUTHORIZATION_HEADER_KEY);

    if(authorizationHeader != null) {
      if (authorizationHeader.startsWith(BASIC_PREFIX)) {
        return checkBasicAuth(authorizationHeader);
      } else if (authorizationHeader.startsWith(BEARER_PREFIX)) {
        return checkBearerAuth();
      }
    }

    throw new NotAuthorizedException("No Authentication provided");
  }

  public void validateIsSameUser(AuthenticationManager.AuthResult auth, UserEntity user){
    if(!auth.getUser().getId().equals(user.getId())){
      throw new ForbiddenException("apiKeys are only visible for it's owner");
    }
  }

  public void validateIsSameUserOrAdmin(AuthenticationManager.AuthResult auth, UserEntity user){
    Set<RoleModel> roles = RoleUtils.getDeepUserRoleMappings(auth.getUser());

    if(roles.stream().noneMatch(roleModel -> roleModel.getName().equals(ROLE_ADMIN))){
      // Authentication is not an Admin,
      // check if it is the same user making the request
      validateIsSameUser(auth, user);

      throw new ForbiddenException("apiKeys are only visible for it's owner or an Admin");
    }
  }

  private Optional<ClientModel> validateClientCredentials(KeycloakSession session, String username, String password){
    return session
        .clients()
        .getClientsStream(session.getContext().getRealm())
        .filter(clientModel -> clientModel.isEnabled() && clientModel.getClientId().equals(username))
        .filter(clientModel -> clientModel.validateSecret(password))
        .peek(clientModel -> logger.info("AuthService - Valid auth using client credentials, clientId:" +clientModel.getClientId()))
        .findFirst();
  }

}
