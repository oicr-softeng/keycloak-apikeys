package bio.overture.keycloak.model;

import bio.overture.keycloak.params.ScopeName;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import lombok.*;

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
  public String toString() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    ObjectWriter ow = new ObjectMapper().writer(df).withDefaultPrettyPrinter();
    return ow.writeValueAsString(this);
  }

  @SneakyThrows
  public String toJsonMinimal() {
    // parses this instance into a simplified json with no indentation and date format as timestamp
    return new ObjectMapper().writer(new MinimalPrettyPrinter()).writeValueAsString(this);
  }

  public static Comparator<ApiKey> byName = Comparator.comparing(ApiKey::getName);
  public static Comparator<ApiKey> byExpiryDate = Comparator.comparing(ApiKey::getExpiryDate);
  public static Comparator<ApiKey> byIssueDate = Comparator.comparing(ApiKey::getIssueDate);
  public static Comparator<ApiKey> byRevoked = Comparator.comparing(ApiKey::getIsRevoked);
  public static Comparator<ApiKey> byDescription = Comparator.comparing(ApiKey::getDescription);
}
