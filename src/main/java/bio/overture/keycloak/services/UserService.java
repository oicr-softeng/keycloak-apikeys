package bio.overture.keycloak.services;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.Optional;

@RequiredArgsConstructor
public class UserService {

  private final KeycloakSession session;

  private static final Logger logger = Logger.getLogger(UserService.class);

  public UserModel getUserById(String user_id){
    RealmModel realm = this.session.getContext().getRealm();

    UserModel user = this.session.users().getUserById(realm, user_id);

    if(user == null) {
      throw new BadRequestException("User not valid");
    }
    return user;
  }

  public AuthenticationManager.AuthResult checkAuth() {
    AuthenticationManager.AuthResult auth = new AppAuthManager
        .BearerTokenAuthenticator(session)
        .authenticate();

    if (auth == null) {
      throw new NotAuthorizedException("Bearer is not valid");
    } else if (auth.getToken().getIssuedFor() == null
        || !auth.getToken().isActive()
        || auth.getToken().isExpired()) {
      throw new ForbiddenException("Token is not valid");
    }
    return auth;
  }

  public void validateIsSameUser(AuthenticationManager.AuthResult auth, UserModel userModel){
    if(auth.getUser() != userModel){
      throw new ForbiddenException("apiKeys are only visible for it's owner");
    }
  }

  public void validateIsSameUserOrAdmin(AuthenticationManager.AuthResult auth, UserModel userModel){
    validateIsSameUser(auth, userModel);

    Optional<RoleModel> isAdmin = userModel
        .getRoleMappingsStream()
        .filter(roleModel -> roleModel.getName().equals("ADMIN"))
        .findFirst();

    if(isAdmin.isEmpty()){
      throw new ForbiddenException("apiKeys are only visible for it's owner or an Admin");
    }
  }
}
