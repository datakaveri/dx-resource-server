package org.cdpg.dx.rs.listners;

import static org.cdpg.dx.common.ErrorCode.ERROR_INTERNAL_SERVER;
import static org.cdpg.dx.common.ErrorMessage.INTERNAL_SERVER_ERROR;
import static org.cdpg.dx.rs.listners.util.Constans.TOKEN_INVALID_Q;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.serviceproxy.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.revoked.service.RevokedService;

public class RevokeClientQlistener {
  private static final Logger LOGGER = LogManager.getLogger(RevokeClientQlistener.class);
  private final QueueOptions options =
      new QueueOptions().setMaxInternalQueueSize(1000).setKeepMostRecent(true);
  private final DataBrokerService dataBrokerService;
  private final RevokedService revokedService;

  public RevokeClientQlistener(DataBrokerService dataBrokerService, RevokedService revokedService) {
    this.dataBrokerService = dataBrokerService;
    this.revokedService = revokedService;
  }

  public Future<Void> start() {
    Promise<Void> promise = Promise.promise();
    LOGGER.info("Revoked Q listener started");

    dataBrokerService
        .basicConsumeInternal(TOKEN_INVALID_Q, options)
        .onSuccess(
            revokedHandler -> {
              revokedHandler.handler(
                  message -> {
                    Buffer body = message.body();
                    if (body != null) {
                      JsonObject invalidClientJson = new JsonObject(body);
                      LOGGER.debug("received message from revoked-client Q :" + invalidClientJson);
                      String userId = invalidClientJson.getString("sub");
                      String value = invalidClientJson.getString("expiry");
                      revokedService
                          .putRevokedInCache(userId, value)
                          .onSuccess(
                              successHandler -> {
                                LOGGER.debug("revoked client message published");
                              })
                          .onFailure(
                              failureHandler -> {
                                LOGGER.debug("revoked client message fail");
                              });
                    } else {
                      LOGGER.error("Empty json received from revoke_token queue");
                    }
                  });
            })
        .onFailure(
            failure -> {
              LOGGER.error("failed ::" + failure.getMessage());
              promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
            });

    return promise.future();
  }
}
