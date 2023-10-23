package bio.overture.keycloak.model;

import bio.overture.keycloak.params.ScopeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiKey {
  private String name;
  private Set<ScopeName> scope;
  private OffsetDateTime expiryDate;
  private OffsetDateTime issueDate;
  private Boolean isRevoked;
  private String description;

  @SneakyThrows
  @Override
  public String toString(){
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    return ow.writeValueAsString(this);
  }

  public static Comparator<ApiKey> byName = Comparator.comparing(ApiKey::getName);
  public static Comparator<ApiKey> byExpiryDate = Comparator.comparing(ApiKey::getExpiryDate);
  public static Comparator<ApiKey> byIssueDate = Comparator.comparing(ApiKey::getIssueDate);
  public static Comparator<ApiKey> byRevoked = Comparator.comparing(ApiKey::getIsRevoked);
  public static Comparator<ApiKey> byDescription = Comparator.comparing(ApiKey::getDescription);
}
