package org.cdpg.dx.common;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.response.DxErrorResponse;
import org.cdpg.dx.common.util.ExceptionHttpStatusMapper;
import org.cdpg.dx.common.util.ThrowableUtils;

public class FailureHandler {

  private static final Logger LOGGER = LogManager.getLogger(FailureHandler.class);

  public static void handle(RoutingContext context) {
    LOGGER.trace("FailureHandler.handle() started");
    Throwable failure = context.failure();
    if (failure == null) {
      failure = new RuntimeException("Unknown server error");
    }

    HttpStatusCode statusCode = ExceptionHttpStatusMapper.map(failure);

    // Log complete error with stack trace for diagnostics
    LOGGER.error("Unhandled error: {}, {}", failure.getMessage(), failure);

    // Avoid leaking internal exception messages
    String safeDetail = ThrowableUtils.isSafeToExpose(failure)
            ? failure.getMessage()
            : "An unexpected error occurred";

    DxErrorResponse errorResponse = new DxErrorResponse(
            statusCode.getUrn(),
            statusCode.getDescription(),
            safeDetail
    );

    if (!context.response().ended()) {
      int status = statusCode.getValue();
      if (status < 400 || status > 599) {
        status = 500;
      }

      context
              .response()
              .putHeader("Content-Type", "application/json")
              .setStatusCode(status)
              .end(errorResponse.toJson().encode());
    }
  }

}
