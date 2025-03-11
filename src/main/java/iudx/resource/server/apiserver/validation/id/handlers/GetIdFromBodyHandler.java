package iudx.resource.server.apiserver.validation.id.handlers;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.RoutingContextHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetIdFromBodyHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(GetIdFromBodyHandler.class);

  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.debug("Info : path {}", RoutingContextHelper.getRequestPath(routingContext));
    String id = getIdFromBody(routingContext);
    if (id != null) {
      LOGGER.info("id :" + id);
      RoutingContextHelper.setId(routingContext, id);
      routingContext.next();
    } else {
      LOGGER.error("Error : Id not Found");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(404);
      routingContext
          .response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(RESOURCE_NOT_FOUND_URN, statusCode).toString());
    }
  }

  private String getIdFromBody(RoutingContext routingContext) {
    JsonObject body;
    body = routingContext.body().asJsonObject();

    String id = null;
    if (body != null) {
      JsonArray array = body.getJsonArray(JSON_ENTITIES);
      if (array != null) {
        id = array.getString(0);
      } else {
        JsonObject json = array.getJsonObject(0);
        if (json != null) {
          id = json.getString(ID);
          /*for something like this : "entities": [
          {
          "id": "UUID"
          }
          ]*/
        }
      }
    }
    return id;
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }
}
