package bio.overture.keycloak.utils;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public class CollectionUtils {

  public static <T, U> List<U> mapToList(Collection<T> collection, Function<T, U> mapper) {
    return collection.stream().map(mapper).collect(toList());
  }
}
