package bio.overture.keycloak.model.dto;

import bio.overture.keycloak.model.ApiKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import lombok.*;

@Data
@Builder
public class ApiKeyResponse {

  private int limit;

  private int offset;

  private int count;

  private List<ApiKey> resultSet;

  @SneakyThrows
  @Override
  public String toString() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    return new ObjectMapper().writer(df).writeValueAsString(this);
  }
}
