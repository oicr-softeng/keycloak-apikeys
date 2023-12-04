package bio.overture.keycloak.params;

import static java.lang.String.format;

import bio.overture.keycloak.model.enums.AccessLevel;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.ws.rs.BadRequestException;
import lombok.Data;

@Data
public class ScopeName {
  private String scopeName;

  public ScopeName(String name) {
    if (!name.contains(".")) {
      throw new BadRequestException(
          format("Bad scope name '%s'. Must be of the form \"<policyName>.<permission>\"", name));
    }
    scopeName = name;
  }

  public AccessLevel getAccessLevel() {
    return AccessLevel.fromValue(scopeName.substring(scopeName.lastIndexOf(".") + 1));
  }

  public String getName() {
    return scopeName.substring(0, scopeName.lastIndexOf("."));
  }

  @Override
  @JsonValue
  public String toString() {
    return scopeName;
  }
}
