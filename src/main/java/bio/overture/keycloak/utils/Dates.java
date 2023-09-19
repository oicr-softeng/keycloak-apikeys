package bio.overture.keycloak.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static java.lang.System.getenv;

public class Dates {


  private static final String APIKEY_DURATION_DAYS = "APIKEY_DURATION_DAYS";

  public static Date keyExpirationDate(){
    int durationDays = Integer.parseInt(getenv().getOrDefault(APIKEY_DURATION_DAYS, "365"));

    LocalDateTime localDate = LocalDateTime.now().plusDays(durationDays);
    return Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
  }
}
