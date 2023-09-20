package bio.overture.keycloak.model.dto;

import bio.overture.keycloak.params.ScopeName;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class CheckApiKeyResponse {

  private String user_id;
  private long exp;
  private boolean isRevoked;
  private boolean isValid;
  private String message;
  private Set<ScopeName> scope;
}
