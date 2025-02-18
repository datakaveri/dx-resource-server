package iudx.resource.server.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Function;

public class Util {

  public static Function<String, URI> toUriFunction =
      (value) -> {
        URI uri = null;
        try {
          uri = new URI(value);
        } catch (URISyntaxException e) {
          e.printStackTrace();
        }
        return uri;
      };

  public static <T> List<T> toList(JsonArray arr) {
    if (arr == null) {
      return null;
    } else {
      return (List<T>) arr.getList();
    }
  }

  public static String errorResponse(HttpStatusCode code) {
    return new JsonObject()
        .put("type", code.getUrn())
        .put("title", code.getDescription())
        .put("detail", code.getDescription())
        .toString();
  }
}
