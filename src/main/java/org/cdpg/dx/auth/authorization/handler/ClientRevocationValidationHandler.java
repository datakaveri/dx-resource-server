package org.cdpg.dx.auth.authorization.handler;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auth.authorization.exception.AuthorizationException;
import org.cdpg.dx.common.ResponseUrn;
import org.cdpg.dx.common.exception.DxAuthException;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.revoked.service.RevokedService;
import org.cdpg.dx.util.RoutingContextHelper;

public class ClientRevocationValidationHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER =
      LogManager.getLogger(ClientRevocationValidationHandler.class);
  private final RevokedService revokedService;

  public ClientRevocationValidationHandler(RevokedService revokedService) {
    this.revokedService = revokedService;
  }

  @Override
  public void handle(RoutingContext context) {
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(context);

    if (jwtData.isEmpty()) {
      LOGGER.error("JWT data not found in context.");
      logAndFail(context, "JWT data missing in request context", "JWT data not found");
      return;
    }

    isRevokedClientToken(jwtData.get())
        .onSuccess(
            unused -> {
              LOGGER.info("Client token is valid and not revoked.");
              context.next();
            })
        .onFailure(
            failure -> {
              LOGGER.error("Client token is revoked: {}", failure.getMessage());
              logAndFail(context, failure.getMessage(), "Client token is revoked");
            });
  }

  private Future<Void> isRevokedClientToken(JwtData jwtData) {
    if (jwtData.iss().equalsIgnoreCase(jwtData.sub())) {
      LOGGER.debug("Token is issued to a user, skipping revocation check.");
      return Future.succeededFuture();
    }

    return revokedService
        .fetchRevokedInfo(jwtData.sub())
        .compose(
            info -> {
              String timestamp = info.getString("value");

              if (timestamp == null) {
                LOGGER.debug("No revocation timestamp found for client: {}", jwtData.sub());
                return Future.<Void>succeededFuture();
              }

              LocalDateTime revokedAt = LocalDateTime.parse(timestamp);
              LocalDateTime jwtIssuedAt =
                  LocalDateTime.ofInstant(
                      Instant.ofEpochSecond(jwtData.iat()), ZoneId.systemDefault());

              LOGGER.debug("Client token issued at: {}, revoked at: {}", jwtIssuedAt, revokedAt);

              if (jwtIssuedAt.isBefore(revokedAt)) {
                LOGGER.warn("Client token was issued before revocation. Denying access.");
                return Future.failedFuture(new DxAuthException(ResponseUrn.INVALID_TOKEN_URN.getMessage()));
              }

              return Future.succeededFuture();
            })
        .recover(
            error -> {
              LOGGER.warn(
                  "Failed to fetch revocation info from cache for client {}: {}",
                  jwtData.sub(),
                  error.getMessage());
              return Future.succeededFuture();
            });
  }

  /**
   * Logs an unauthorized access attempt and triggers `context.fail()`.
   *
   * @param context Routing context
   * @param logMessage Log message for debugging
   * @param errorMessage Error message for the client
   */
  private void logAndFail(RoutingContext context, String logMessage, String errorMessage) {
    LOGGER.warn(logMessage);
    context.fail(new DxAuthException(errorMessage));
  }
}
