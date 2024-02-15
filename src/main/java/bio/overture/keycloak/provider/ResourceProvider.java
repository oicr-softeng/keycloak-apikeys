package bio.overture.keycloak.provider;

import bio.overture.keycloak.resource.ApiKeyResource;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

@RequiredArgsConstructor
public class ResourceProvider implements RealmResourceProvider {

  private final KeycloakSession session;

  @Override
  public Object getResource() {
    return new ApiKeyResource(session);
  }

  @Override
  public void close() {}
}
