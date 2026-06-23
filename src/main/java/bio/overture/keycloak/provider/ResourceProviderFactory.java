package bio.overture.keycloak.provider;

import java.io.InputStream;
import java.util.Properties;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class ResourceProviderFactory implements RealmResourceProviderFactory {

  public static final String PROVIDER_ID = "apikey";

  private static final Logger logger = Logger.getLogger(ResourceProviderFactory.class);

  @Override
  public RealmResourceProvider create(KeycloakSession keycloakSession) {
    return new ResourceProvider(keycloakSession);
  }

  @Override
  public void init(Config.Scope scope) {
    String version = "unknown";
    String commit = "unknown";
    try (InputStream is = getClass().getClassLoader().getResourceAsStream("build.properties")) {
      if (is != null) {
        Properties props = new Properties();
        props.load(is);
        version = props.getProperty("version", "unknown");
        commit = props.getProperty("commit", "unknown");
      }
    } catch (Exception e) {
      logger.debug("Could not read plugin build info from build.properties", e);
    }
    logger.infof("keycloak-apikeys plugin loaded: version %s, commit %s", version, commit);
  }

  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory) {}

  @Override
  public void close() {}

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}
