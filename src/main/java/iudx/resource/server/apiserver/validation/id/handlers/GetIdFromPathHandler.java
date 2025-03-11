package iudx.resource.server.apiserver.validation.id.handlers;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.RoutingContextHelper;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetIdFromPathHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(GetIdFromPathHandler.class);

  @Override
  public void handle(RoutingContext routingContext) {
    LOGGER.debug("Info : path {}", RoutingContextHelper.getRequestPath(routingContext));
    String id = getIdFromPath(routingContext);
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

  private String getIdFromPath(RoutingContext routingContext) {
    StringBuilder id = null;
    Map<String, String> pathParams = routingContext.pathParams();
    LOGGER.debug("path params :" + pathParams);
    if (pathParams != null && !pathParams.isEmpty()) {
      if (pathParams.containsKey("UUID")) {
        id = new StringBuilder(pathParams.get("UUID"));
        LOGGER.info(
            "API is : {} and path param is : {}",
            RoutingContextHelper.getRequestPath(routingContext),
            pathParams);
        LOGGER.debug("id :" + id);
      } else if (pathParams.containsKey(USER_ID) && pathParams.containsKey(JSON_ALIAS)) {
        id = new StringBuilder();
        LOGGER.info(
            "User id and alias name are present : {}, {}",
            routingContext.request().path(),
            pathParams);
        id.append(pathParams.get(USER_ID)).append("/").append(pathParams.get(JSON_ALIAS));
      }
    }
    return id != null ? id.toString() : null;
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }
}
