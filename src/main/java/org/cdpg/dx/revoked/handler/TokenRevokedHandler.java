package org.cdpg.dx.revoked.handler;

import static org.cdpg.dx.common.ErrorCode.ERROR_REVOKED_INVALID_TOKEN;
import static org.cdpg.dx.common.ErrorMessage.INVALID_REVOKED_TOKEN;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.revoked.service.RevokedService;
import org.cdpg.dx.util.RoutingContextHelper;

public class TokenRevokedHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(TokenRevokedHandler.class);
  private final RevokedService revokedService;
  public TokenRevokedHandler(RevokedService revokedService) {
    this.revokedService = revokedService;
  }

  /**
   * @param routingContext
   */
  @Override
  public void handle(RoutingContext routingContext) {
    routingContext.next();
  }

  public Handler<RoutingContext> isTokenRevoked() {
    return this::check;
  }

  void check(RoutingContext event) {
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(event);
    LOGGER.trace("isRevokedClientToken started param : " + jwtData);
    if (!jwtData.get().iss().equals(jwtData.get().sub())) {
      Future<JsonObject> cacheCallFuture = revokedService.fetchRevokedInfo(jwtData.get().sub());
      cacheCallFuture
          .onSuccess(
              successhandler -> {
                JsonObject responseJson = successhandler;
                LOGGER.debug("responseJson : " + responseJson);
                String timestamp = responseJson.getString("value");

                LocalDateTime revokedAt = LocalDateTime.parse(timestamp);
                LocalDateTime jwtIssuedAt =
                    LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(jwtData.get().iat()), ZoneId.systemDefault());

                if (jwtIssuedAt.isBefore(revokedAt)) {
                  LOGGER.info("jwt issued at : " + jwtIssuedAt + " revokedAt : " + revokedAt);
                  LOGGER.error("Privileges for client are revoked.");
                  event.fail(
                      new ServiceException(ERROR_REVOKED_INVALID_TOKEN, INVALID_REVOKED_TOKEN));
                } else {
                  event.next();
                }
              })
          .onFailure(
              failureHandler -> {
                LOGGER.info("cache call result : [fail] {}", String.valueOf(failureHandler));
                event.next();
              });
    } else {
      event.next();
    }
  }
}
