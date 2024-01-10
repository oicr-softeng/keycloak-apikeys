package bio.overture.keycloak.services;

import bio.overture.keycloak.params.ScopeName;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.admin.PolicyEvaluationService;
import org.keycloak.authorization.common.DefaultEvaluationContext;
import org.keycloak.authorization.common.UserModelIdentity;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

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
    AuthenticationManager.AuthResult auth =
        new AppAuthManager.BearerTokenAuthenticator(session).authenticate();

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

  public ClientModel checkBasicAuth(String authorizationHeader) {
    String base64Credentials = authorizationHeader.substring(BASIC_PREFIX.length()).trim();
    String credentials = new String(Base64.getDecoder().decode(base64Credentials));

    // Split credentials into username and password
    String[] parts = credentials.split(":", 2);
    String username = parts[0];
    String password = parts[1];

    return validateClientCredentials(session, username, password)
        .orElseThrow(() -> new NotAuthorizedException("Invalid credentials"));
  }

  public Object checkBearerOrBasicAuth() {
    String authorizationHeader =
        session.getContext().getRequestHeaders().getHeaderString(AUTHORIZATION_HEADER_KEY);

    if (authorizationHeader != null) {
      if (authorizationHeader.startsWith(BASIC_PREFIX)) {
        return checkBasicAuth(authorizationHeader);
      } else if (authorizationHeader.startsWith(BEARER_PREFIX)) {
        return checkBearerAuth();
      }
    }

    throw new NotAuthorizedException("No Authentication provided");
  }

  public void validateIsSameUser(AuthenticationManager.AuthResult auth, UserEntity user) {
    if (!auth.getUser().getId().equals(user.getId())) {
      throw new ForbiddenException("apiKeys are only visible for it's owner");
    }
  }

  public void validateIsSameUserOrAdmin(AuthenticationManager.AuthResult auth, UserEntity user) {
    if (auth.getUser().getGroupsCountByNameContaining(ROLE_ADMIN) == 0) {
      // Authentication is not an Admin,
      // check if it is the same user making the request
      try {
        validateIsSameUser(auth, user);
      } catch (Exception e) {
        throw new ForbiddenException("apiKeys are only visible for it's owner or an Admin");
      }
    }
  }

  private Optional<ClientModel> validateClientCredentials(
      KeycloakSession session, String username, String password) {
    return session
        .clients()
        .getClientsStream(session.getContext().getRealm())
        .filter(
            clientModel -> clientModel.isEnabled() && clientModel.getClientId().equals(username))
        .filter(clientModel -> clientModel.validateSecret(password))
        .peek(
            clientModel ->
                logger.info(
                    "AuthService - Valid auth using client credentials, clientId:"
                        + clientModel.getClientId()))
        .findFirst();
  }

  public void validatePermissions(AuthenticationManager.AuthResult auth, List<ScopeName> scopes) {

    AuthorizationProvider authorizationProvider = session.getProvider(AuthorizationProvider.class);

    UserModelIdentity identity = new UserModelIdentity(auth.getClient().getRealm(), auth.getUser());

    boolean scopesAreValid =
        scopes.stream()
            .allMatch(
                scopeName ->
                    getClientsStream()
                        .anyMatch(
                            client -> {
                              ResourceServer resourceServer =
                                  getResourceServer(authorizationProvider, client);

                              Resource resource =
                                  getResource(authorizationProvider, resourceServer, scopeName);

                              if (resource == null) return false;

                              Scope scopeResource = getScope(resource, scopeName);

                              if (scopeResource == null) return false;

                              List<ResourcePermission> permissions =
                                  getPermissions(resource, scopeResource, resourceServer);

                              return evaluatePermissions(
                                  authorizationProvider, permissions, identity, resourceServer);
                            }));

    if (!scopesAreValid || scopes.size() == 0) {
      throw new ForbiddenException("Invalid Scope");
    }
  }

  private boolean evaluatePermissions(
      AuthorizationProvider authorizationProvider,
      List<ResourcePermission> permissions,
      Identity identity,
      ResourceServer resourceServer) {
    return authorizationProvider
        .evaluators()
        .from(permissions, new DefaultEvaluationContext(identity, session))
        .evaluate(
            new PolicyEvaluationService.EvaluationDecisionCollector(
                authorizationProvider, resourceServer, new AuthorizationRequest()))
        .getResults()
        .stream()
        .anyMatch(result -> result.getEffect().equals(Decision.Effect.PERMIT));
  }

  private Stream<ClientModel> getClientsStream() {
    return session
        .clients()
        .getClientsStream(session.getContext().getRealm())
        .filter(ClientModel::isEnabled)
        .filter(ClientModel::isFullScopeAllowed)
        .filter(ClientModel::isDirectAccessGrantsEnabled);
  }

  private ResourceServer getResourceServer(
      AuthorizationProvider authorizationProvider, ClientModel client) {
    return authorizationProvider.getStoreFactory().getResourceServerStore().findByClient(client);
  }

  private Resource getResource(
      AuthorizationProvider authorizationProvider,
      ResourceServer resourceServer,
      ScopeName scopeName) {
    if (resourceServer == null) return null;
    return authorizationProvider
        .getStoreFactory()
        .getResourceStore()
        .findByName(resourceServer, scopeName.getName());
  }

  private Scope getScope(Resource resource, ScopeName scopeName) {
    return resource.getScopes().stream()
        .filter(
            scope -> {
              try {
                return scope.getName().equals(scopeName.getAccessLevel().toString());
              } catch (Exception e) {
                e.printStackTrace();
                return false;
              }
            })
        .findFirst()
        .orElse(null);
  }

  private List<ResourcePermission> getPermissions(
      Resource resource, Scope scopeResource, ResourceServer resourceServer) {
    return Arrays.asList(
        new ResourcePermission(
            resource, Stream.of(scopeResource).collect(Collectors.toSet()), resourceServer));
  }
}
