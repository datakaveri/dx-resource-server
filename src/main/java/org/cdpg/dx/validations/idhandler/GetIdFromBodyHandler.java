package org.cdpg.dx.validations.idhandler;
import static org.cdpg.dx.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;
import static org.cdpg.dx.validations.util.Constants.*;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.HttpStatusCode;
import org.cdpg.dx.common.ResponseUrn;
import org.cdpg.dx.util.RoutingContextHelper;

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
//
//  private String getIdFromBody(RoutingContext routingContext) {
//    JsonObject body = routingContext.body().asJsonObject();
//    String id = null;
//    if (body != null) {
//      JsonArray array = body.getJsonArray(JSON_ENTITIES);
//      if (array != null) {
//        JsonObject json = array.getJsonObject(0);
//        if (json != null) {
//          id = json.getString(ID);
//        }else id= array.getString(0);
//      }
//    }
//    return id;
//  }
  public static String getIdFromBody(RoutingContext routingContext) {
    JsonObject body = routingContext.body().asJsonObject();
    JsonArray entities = body.getJsonArray("entities");
    if (entities == null || entities.isEmpty()) {
      return null;
    }

    Object firstEntity = entities.getValue(0);
    if (firstEntity instanceof JsonObject) {
      return ((JsonObject) firstEntity).getString("id");
    } else if (firstEntity instanceof String) {
      return (String) firstEntity;
    }

    return null;
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }
}
