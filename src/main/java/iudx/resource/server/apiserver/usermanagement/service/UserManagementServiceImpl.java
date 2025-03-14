package iudx.resource.server.apiserver.usermanagement.service;

import static iudx.resource.server.databroker.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import iudx.resource.server.apiserver.usermanagement.model.ResetResponseModel;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserManagementServiceImpl {
  private static final Logger LOGGER = LogManager.getLogger(UserManagementServiceImpl.class);
  private final DataBrokerService dataBrokerService;

  public UserManagementServiceImpl(DataBrokerService dataBrokerService) {
    this.dataBrokerService = dataBrokerService;
  }

  public Future<ResetResponseModel> resetPassword(String userId) {
    LOGGER.trace("Info: resetPassword method started");
    Promise<ResetResponseModel> promise = Promise.promise();
    dataBrokerService
        .resetPassword(userId)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                /*ResetResponseModel resetResponseModel = new ResetResponseModel(handler.result());*/
                /*promise.complete(resetResponseModel);*/
              } else {
                promise.fail(handler.cause());
              }
            });
    return promise.future();
  }
}
