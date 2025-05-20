package org.cdpg.dx.rs.usermanagement.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.rs.usermanagement.model.ResetPassword;

public class UserManagementServiceImpl implements UserManagementService {
  private static final Logger LOGGER = LogManager.getLogger(UserManagementServiceImpl.class);
  private final DataBrokerService dataBrokerService;

  public UserManagementServiceImpl(DataBrokerService dataBrokerService) {
    this.dataBrokerService = dataBrokerService;
  }

  @Override
  public Future<ResetPassword> resetPassword(String userId) {
    LOGGER.info("Info: resetPassword method started");
    Promise<ResetPassword> promise = Promise.promise();
    dataBrokerService
        .resetPassword(userId)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete(new ResetPassword(userId, handler.result()));
                LOGGER.info("Successfully changed the password");
              } else {
                LOGGER.error("Error: resetPassword", handler.cause());
                promise.fail(handler.cause());
              }
            });
    return promise.future();
  }
}
