package iudx.resource.server.apiserver.exception;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.JSON_DETAIL;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.RestResponse;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FailureHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(FailureHandler.class);

  @Override
  public void handle(RoutingContext context) {
    LOGGER.debug("FailureHandler.handle()--------------------------");
    Throwable failure = context.failure();
    if (context.response().ended()) {
      LOGGER.debug("Already ended");
      return;
    }
    LOGGER.debug("exception caught " + (failure instanceof DxRuntimeException));
    if (failure instanceof DxRuntimeException) {
      LOGGER.debug("---- syaya");
      DxRuntimeException exception = (DxRuntimeException) failure;
      LOGGER.error(exception.toString());

      HttpStatusCode code = HttpStatusCode.getByValue(exception.getStatusCode());

      JsonObject response =
          new RestResponse.Builder()
              .withType(exception.getUrn().getUrn())
              .withTitle(code.getDescription())
              .withMessage(exception.getMessage())
              .build()
              .toJson();

      context
          .response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(exception.getStatusCode())
          .end(response.toString());
    }

    else if (failure instanceof RuntimeException) {

      String validationErrorMessage = MSG_BAD_QUERY;
      context
          .response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(HttpStatus.SC_BAD_REQUEST)
          .end(validationFailureReponse(validationErrorMessage).toString());
    }
    else{
      LOGGER.error("failure");
    }

    context.next();
  }

  private JsonObject validationFailureReponse(String message) {
    return new JsonObject()
        .put(JSON_TYPE, HttpStatus.SC_BAD_REQUEST)
        .put(JSON_TITLE, "Bad Request")
        .put(JSON_DETAIL, message);
  }
}
