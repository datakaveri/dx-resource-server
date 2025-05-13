package org.cdpg.dx.validations.idhandler;

import static org.cdpg.dx.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;
import static org.cdpg.dx.validations.util.Constants.*;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.HttpStatusCode;
import org.cdpg.dx.common.ResponseUrn;
import org.cdpg.dx.util.RoutingContextHelper;

public class GetIdFromParams implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(GetIdFromParams.class);

  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.debug("Info : path {}", RoutingContextHelper.getRequestPath(routingContext));
    String id = getIdFromParam(routingContext);
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

  private String getIdFromParam(RoutingContext routingContext) {
    return routingContext.request().getParam(ID);
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }
}
