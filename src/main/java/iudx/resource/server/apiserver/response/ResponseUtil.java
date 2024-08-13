package iudx.resource.server.apiserver.response;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;

public class ResponseUtil {

  public static JsonObject generateResponse(HttpStatusCode statusCode, ResponseUrn urn) {
    return generateResponse(statusCode, urn, statusCode.getUrn());
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
