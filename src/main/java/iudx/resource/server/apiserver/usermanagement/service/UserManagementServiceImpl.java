package iudx.resource.server.apiserver.usermanagement.service;

import static iudx.resource.server.database.util.Constants.ERROR;
import static iudx.resource.server.databroker.util.Constants.INTERNAL_ERROR_CODE;
import static iudx.resource.server.databroker.util.Constants.QUEUE_LIST_ERROR;
import static iudx.resource.server.databroker.util.Util.getResponseJson;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserManagementServiceImpl {
  private static final Logger LOGGER = LogManager.getLogger(UserManagementServiceImpl.class);
  private DataBrokerService dataBrokerService;

  public UserManagementServiceImpl(DataBrokerService dataBrokerService) {
    this.dataBrokerService = dataBrokerService;
  }

  public Future<JsonObject> resetPassword(String userId) {
    LOGGER.trace("Info: resetPassword method started");
    Promise<JsonObject> promise = Promise.promise();
    dataBrokerService
        .resetPassword(userId)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete(handler.result());
              } else {
                promise.fail(
                    getResponseJson(
                            HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                            INTERNAL_ERROR_CODE,
                            ERROR,
                            QUEUE_LIST_ERROR)
                        .toString());
              }
            });
    return promise.future();
  }
}
