package bio.overture.keycloak.services;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.Set;

public class UserService {

  private final KeycloakSession session;
  private EntityManager entityManager;

  private static final Logger logger = Logger.getLogger(UserService.class);

  public UserService(KeycloakSession session) {
    this.session = session;
    this.entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
  }

  public UserEntity getUserById(String user_id){

    UserEntity userEntity = entityManager.find(UserEntity.class, user_id);

    if(userEntity == null) {
      throw new BadRequestException("User not valid");
    }
    return userEntity;
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

  public void validateIsSameUser(AuthenticationManager.AuthResult auth, UserEntity user){
    if(!auth.getUser().getId().equals(user.getId())){
      throw new ForbiddenException("apiKeys are only visible for it's owner");
    }
  }

  public void validateIsSameUserOrAdmin(AuthenticationManager.AuthResult auth, UserEntity user){
    Set<RoleModel> roles = RoleUtils.getDeepUserRoleMappings(auth.getUser());

    if(roles.stream().noneMatch(roleModel -> roleModel.getName().equals("ADMIN"))){
      // Authentication is not an Admin,
      // check if it is the same user making the request
      validateIsSameUser(auth, user);

      throw new ForbiddenException("apiKeys are only visible for it's owner or an Admin");
    }
  }
}
