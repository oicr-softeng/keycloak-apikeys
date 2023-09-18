package bio.overture.keycloak;

import bio.overture.keycloak.provider.ResourceProviderFactory;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@ExtendWith(MockitoExtension.class)
public class ApiKeyResourceTest {

  @Container
  private static final KeycloakContainer keycloak = new KeycloakContainer()
        .withProviderClassesFrom("target/classes");

  @Test
  public void testKeycloakContainer(){
    assertTrue(keycloak.isRunning());
  }

  @Test
  public void listApiKeys_withInvalidUserId_shouldThrowBadRequestError() {
    givenSpec()
        .when()
        .queryParams(Map.of("user_id", "0000"))
        .get("api_key")
        .peek()
        .then()
        .statusCode(400)
        .log();
  }

  @Test
  public void issueApiKey_withNoBearerToken_shouldReturnUnauthorizedError() {
    givenSpec()
        .when()
        .post("api_key")
        .peek()
        .then()
        .statusCode(401);
  }

  private RequestSpecification givenSpec() {
    return given()
        .baseUri(keycloak.getAuthServerUrl())
        .basePath("/realms/master/" + ResourceProviderFactory.PROVIDER_ID);
  }
}
