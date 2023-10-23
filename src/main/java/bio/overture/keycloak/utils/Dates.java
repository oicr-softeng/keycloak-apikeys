package bio.overture.keycloak.utils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static java.lang.System.getenv;

public class Dates {


  private static final String APIKEY_DURATION_DAYS = "APIKEY_DURATION_DAYS";

  public static OffsetDateTime keyExpirationDate(){
    int durationDays = Integer.parseInt(getenv().getOrDefault(APIKEY_DURATION_DAYS, "365"));

    LocalDateTime localDate = LocalDateTime.now().plusDays(durationDays);
    return OffsetDateTime.of(localDate, ZoneOffset.UTC);
  }

  public static boolean isExpired(OffsetDateTime expirationDate){
    return expirationDate.isBefore(OffsetDateTime.now());
  }
}
