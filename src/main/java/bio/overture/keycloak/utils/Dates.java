package bio.overture.keycloak.utils;

import static java.lang.System.getenv;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class Dates {

  private static final String APIKEY_DURATION_DAYS = "APIKEY_DURATION_DAYS";

  public static Date keyExpirationDate() {
    int durationDays = Integer.parseInt(getenv().getOrDefault(APIKEY_DURATION_DAYS, "365"));

    LocalDateTime localDate = LocalDateTime.now().plusDays(durationDays);
    return Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
  }

  public static boolean isExpired(Date expirationDate) {
    return expirationDate.before(new Date());
  }
}
