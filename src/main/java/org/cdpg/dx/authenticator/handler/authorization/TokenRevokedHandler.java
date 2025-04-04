package org.cdpg.dx.authenticator.handler.authorization;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.exception.DxRuntimeException;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.cache.util.CacheType;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.RoutingContextHelper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.authenticator.model.JwtData;

public class TokenRevokedHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(TokenRevokedHandler.class);
  final CacheService cache;

  public TokenRevokedHandler(CacheService cache) {
    this.cache = cache;
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
    JwtData jwtData = RoutingContextHelper.getJwtData(event);
    LOGGER.trace("isRevokedClientToken started param : " + jwtData);
    if (!jwtData.getIss().equals(jwtData.getSub())) {
      CacheType cacheType = CacheType.REVOKED_CLIENT;
      String subId = jwtData.getSub();
      JsonObject requestJson = new JsonObject().put("type", cacheType).put("key", subId);

      Future<JsonObject> cacheCallFuture = cache.get(requestJson);
      cacheCallFuture
          .onSuccess(
              successhandler -> {
                JsonObject responseJson = successhandler;
                LOGGER.debug("responseJson : " + responseJson);
                String timestamp = responseJson.getString("value");

                LocalDateTime revokedAt = LocalDateTime.parse(timestamp);
                LocalDateTime jwtIssuedAt =
                    LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(jwtData.getIat()), ZoneId.systemDefault());

                if (jwtIssuedAt.isBefore(revokedAt)) {
                  LOGGER.info("jwt issued at : " + jwtIssuedAt + " revokedAt : " + revokedAt);
                  LOGGER.error("Privileges for client are revoked.");
                  event.fail(
                      new DxRuntimeException(
                          HttpStatusCode.getByValue(401).getValue(),
                          ResponseUrn.INVALID_TOKEN_URN,
                          "revoked token passes"));
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
