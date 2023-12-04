package bio.overture.keycloak.model.enums;

import java.util.Arrays;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public enum AccessLevel {
  READ("READ"),
  WRITE("WRITE"),
  DENY("DENY");

  @NonNull private final String value;

  public static AccessLevel fromValue(String value) {
    for (val policyMask : values()) {
      if (policyMask.value.equalsIgnoreCase(value)) {
        return policyMask;
      }
    }
    throw new IllegalArgumentException(
        "Invalid enum value '" + value + "', Allowed values are " + Arrays.toString(values()));
  }

  @Override
  public String toString() {
    return value;
  }
}
