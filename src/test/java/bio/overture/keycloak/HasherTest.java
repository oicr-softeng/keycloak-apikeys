package bio.overture.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import bio.overture.keycloak.utils.Hasher;
import org.junit.jupiter.api.Test;

public class HasherTest {

  @Test
  public void hash_should_match() {
    Hasher hasher = new Hasher();
    String inputText = "someSampleText";
    String hashedText = "6B8AD6565FC5FA7C6D906AE3A78E12FDA9B2DA0CE101CDCE1A49417A2801E8D2";

    // same hash function should always produce the same hash
    assertEquals(hashedText, hasher.generateHash(inputText));
  }

  @Test
  public void hash_should_not_match() {
    Hasher hasher = new Hasher();
    String inputText = "SOMESAMPLETEXT";
    String hashedText = "6B8AD6565FC5FA7C6D906AE3A78E12FDA9B2DA0CE101CDCE1A49417A2801E8D2";

    // same hash function should always produce the same hash
    assertNotEquals(hashedText, hasher.generateHash(inputText));
  }
}
