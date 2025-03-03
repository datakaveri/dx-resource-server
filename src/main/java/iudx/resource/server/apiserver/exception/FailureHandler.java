package iudx.resource.server.apiserver.exception;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.JSON_DETAIL;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.RestResponse;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FailureHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(FailureHandler.class);

  private static void conflictError(RoutingContext context) {
    JsonObject response =
        new RestResponse.Builder()
            .withType(HttpStatusCode.CONFLICT.getUrn())
            .withTitle(HttpStatusCode.CONFLICT.getDescription())
            .withMessage(HttpStatusCode.CONFLICT.getDescription())
            .build()
            .toJson();
    LOGGER.error(response.toString());
    context
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(HttpStatusCode.CONFLICT.getValue())
        .end(response.toString());
  }

  private static void badRequestError(RoutingContext context, ServiceException serviceException) {
    JsonObject response =
        new RestResponse.Builder()
            .withType(HttpStatusCode.BAD_REQUEST.getUrn())
            .withTitle(HttpStatusCode.BAD_REQUEST.getDescription())
            .withMessage(serviceException.getMessage())
            .build()
            .toJson();
    LOGGER.error(response.toString());
    context
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(HttpStatusCode.BAD_REQUEST.getValue())
        .end(response.toString());
  }

  private static void internalServerError(RoutingContext context) {
    JsonObject response =
        new RestResponse.Builder()
            .withType(HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn())
            .withTitle(HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription())
            .withMessage(HttpStatusCode.INTERNAL_SERVER_ERROR.getDescription())
            .build()
            .toJson();
    LOGGER.error(response.toString());
    context
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR.getValue())
        .end(response.toString());
  }

  @Override
  public void handle(RoutingContext context) {
    LOGGER.trace("FailureHandler.handle()");
    Throwable failure = context.failure();
    if (context.response().ended()) {
      LOGGER.debug("Already ended");
      return;
    }
    if (failure instanceof DxRuntimeException exception) {
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
    } else if (failure instanceof ServiceException serviceException) {
      LOGGER.error(failure.toString());
      if (serviceException.failureCode() == 9) {
        conflictError(context);
      } else if (serviceException.failureCode() == 8) {
        badRequestError(context, serviceException);
      } else if (serviceException.failureCode() == 7) {

      } else if (serviceException.failureCode() == 6) {

      } else if (serviceException.failureCode() == 5) {
      } else if (serviceException.failureCode() == 4) {
        notFoundError(context, serviceException);
      } else if (serviceException.failureCode() == 3) {
      } else if (serviceException.failureCode() == 2) {
      } else if (serviceException.failureCode() == 1) {
      } else {
        internalServerError(context);
      }
    } else if (failure instanceof RuntimeException) {
      String validationErrorMessage = MSG_BAD_QUERY;
      context
          .response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(HttpStatus.SC_BAD_REQUEST)
          .end(validationFailureReponse(validationErrorMessage).toString());
    } else {
      LOGGER.error("failure");
    }
    context.next();
  }

  private void notFoundError(RoutingContext context, ServiceException serviceException) {
    JsonObject response =
        new RestResponse.Builder()
            .withType(HttpStatusCode.NOT_FOUND.getUrn())
            .withTitle(HttpStatusCode.NOT_FOUND.getDescription())
            .withMessage(serviceException.getMessage())
            .build()
            .toJson();
    LOGGER.error(response.toString());
    context
        .response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(HttpStatusCode.NOT_FOUND.getValue())
        .end(response.toString());
  }

  private JsonObject validationFailureReponse(String message) {
    return new JsonObject()
        .put(JSON_TYPE, HttpStatus.SC_BAD_REQUEST)
        .put(JSON_TITLE, "Bad Request")
        .put(JSON_DETAIL, message);
  }
}
