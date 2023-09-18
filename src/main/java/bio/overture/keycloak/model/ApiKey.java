package bio.overture.keycloak.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.Date;
import java.util.Set;

@Data
@Builder
public class ApiKey {
  @NonNull
  private String name;

  @NonNull
  private Set<String> scope;

  @NonNull
  private Date expiryDate;

  @NonNull
  private Date issueDate;

  @NonNull
  private Boolean isRevoked;

  private String description;
}
