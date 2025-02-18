package iudx.resource.server.common;

import io.vertx.core.json.JsonObject;

public class ResponseUtil {

  public static JsonObject generateResponse(HttpStatusCode statusCode, ResponseUrn urn) {
    return generateResponse(statusCode, urn, statusCode.getDescription());
  }

  public static JsonObject generateResponse(
      HttpStatusCode statusCode, ResponseUrn urn, String message) {
    String type = urn.getUrn();
    return new RestResponse.Builder()
        .withType(type)
        .withTitle(statusCode.getDescription())
        .withMessage(message)
        .build()
        .toJson();
  }
}
