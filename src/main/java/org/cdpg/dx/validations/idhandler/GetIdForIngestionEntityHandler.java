package org.cdpg.dx.validations.idhandler;

import static org.cdpg.dx.util.ResponseUrn.*;
import static org.cdpg.dx.validations.util.Constants.*;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.util.HttpStatusCode;
import org.cdpg.dx.util.ResponseUrn;
import org.cdpg.dx.util.RoutingContextHelper;

public class GetIdForIngestionEntityHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(GetIdForIngestionEntityHandler.class);

  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.debug("Info : path {}", RoutingContextHelper.getRequestPath(routingContext));
    RequestBody requestBody = routingContext.body();
    try {
      JsonArray requestJsonArray = requestBody.asJsonArray();
      Set<String> entityIds = new HashSet<>();

      for (int i = 0; i < requestJsonArray.size(); i++) {
        JsonObject entity = requestJsonArray.getJsonObject(i);
        entityIds.add(entity.getString("entities"));
      }

      if (entityIds.size() == 1) {
        LOGGER.debug("All entity IDs match: " + entityIds.iterator().next());
        JsonObject body = routingContext.body().asJsonArray().getJsonObject(0);
        String id = body.getJsonArray(JSON_ENTITIES).getString(0);
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
      } else {
        LOGGER.error("Entity IDs do not match: " + entityIds);
        processAuthFailure(routingContext, "Entity IDs do not match");
      }
    } catch (Exception e) {
      processAuthFailure(routingContext, "Error processing the request body");
    }
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }

  private void processAuthFailure(RoutingContext ctx, String result) {
    if (result.contains("Entity IDs do not match")
        || result.contains("Error processing the request body")) {
      LOGGER.error("Entity IDs do not match");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(400);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(BAD_REQUEST_URN, statusCode).toString());
    } else {
      LOGGER.error("Error : Authentication Failure");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(INVALID_TOKEN_URN, statusCode).toString());
    }
  }
}
