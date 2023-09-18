package bio.overture.keycloak.model.dto;

import bio.overture.keycloak.model.ApiKey;
import lombok.*;

import java.util.Set;

@Data
@Builder
public class ApiKeyResponse {

    private int limit;

    private int offset;

    private int count;

    private Set<ApiKey> resultSet;
}
