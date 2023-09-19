package bio.overture.keycloak.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;

public class Converters {

  public static <T> T jsonStringToClass(String jsonString, Class<T> tClass) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      return mapper.readValue(jsonString, tClass);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
