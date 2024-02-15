package bio.overture.keycloak.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.SneakyThrows;

public class Hasher {

  // hashing algorithm SHA3_256, SHA_256, KECCAK_256, ...
  private final String ALGORITHM = "SHA-256";

  @SneakyThrows
  public String generateHash(String inputText) {
    MessageDigest md = MessageDigest.getInstance(ALGORITHM);
    byte[] digest = md.digest(inputText.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(digest);
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder out = new StringBuilder();
    for (byte b : bytes) {
      out.append(String.format("%02X", b));
    }
    return out.toString();
  }
}
