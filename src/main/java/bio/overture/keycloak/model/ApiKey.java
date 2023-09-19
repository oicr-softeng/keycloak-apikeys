package bio.overture.keycloak.model;

import bio.overture.keycloak.params.ScopeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.*;

import java.util.Date;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiKey {
  private String name;
  private Set<ScopeName> scope;
  private Date expiryDate;
  private Date issueDate;
  private Boolean isRevoked;
  private String description;

  @SneakyThrows
  @Override
  public String toString(){
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    return ow.writeValueAsString(this);
  }
}
