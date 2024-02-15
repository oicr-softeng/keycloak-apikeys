package bio.overture.keycloak.services;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.BadRequestException;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.UserEntity;

public class UserService {

  private final KeycloakSession session;
  private EntityManager entityManager;

  private static final Logger logger = Logger.getLogger(UserService.class);

  public UserService(KeycloakSession session) {
    this.session = session;
    this.entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
  }

  public UserEntity getUserById(String user_id) {

    if (user_id == null) {
      throw new BadRequestException("user_id is required");
    }

    UserEntity userEntity = entityManager.find(UserEntity.class, user_id);

    if (userEntity == null) {
      throw new BadRequestException("User not valid");
    }
    return userEntity;
  }
}
