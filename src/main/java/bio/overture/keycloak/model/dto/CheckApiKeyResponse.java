package bio.overture.keycloak.model.dto;

import bio.overture.keycloak.params.ScopeName;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

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
