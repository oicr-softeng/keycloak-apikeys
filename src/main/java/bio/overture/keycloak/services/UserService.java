package bio.overture.keycloak.services;

import bio.overture.keycloak.model.ApiKey;
import bio.overture.keycloak.params.ScopeName;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.*;

import static bio.overture.keycloak.utils.Converters.jsonStringToClass;
import static bio.overture.keycloak.utils.Dates.keyExpirationDate;
import static java.util.stream.Collectors.toSet;

@RequiredArgsConstructor
public class UserService {

  private final KeycloakSession session;

  private static final Logger logger = Logger.getLogger(UserService.class);

  public UserModel getUserById(@NonNull String user_id){
    RealmModel realm = this.session.getContext().getRealm();

    UserProvider users = this.session.users();
    UserModel user = users.getUserById(realm, user_id);

    if(user == null) {
      throw new BadRequestException("User not valid");
    }
    return user;
  }

  public Set<ApiKey> getApiKeys(@NonNull UserModel user){

      if(user.getAttributes() == null
          || user.getAttributes().get("api-keys") == null) {
        return Collections.emptySet();
      }

      return user
          .getAttributeStream("api-keys")
          .map(key -> jsonStringToClass(key, ApiKey.class))
          .collect(toSet());
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

  private void setApiKey(UserModel user, ApiKey apiKey){
    user.getAttributes().get("api-keys").add(apiKey.toString());
  }
}
