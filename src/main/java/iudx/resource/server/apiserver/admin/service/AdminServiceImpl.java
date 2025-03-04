package iudx.resource.server.apiserver.admin.service;

import static iudx.resource.server.apiserver.admin.util.Constants.*;
import static iudx.resource.server.common.HttpStatusCode.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.admin.model.ResultModel;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import iudx.resource.server.databroker.util.BroadcastEventType;
import java.time.LocalDateTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdminServiceImpl implements AdminService {
  private static final Logger LOGGER = LogManager.getLogger(AdminServiceImpl.class);
  private final PostgresService postgresService;
  private final DataBrokerService dataBrokerService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  JsonObject result = new JsonObject();

  public AdminServiceImpl(PostgresService postgresService, DataBrokerService dataBrokerService) {
    this.postgresService = postgresService;
    this.dataBrokerService = dataBrokerService;
  }

  @Override
  public Future<ResultModel> revokedTokenRequest(String userid) {
    Promise<ResultModel> promise = Promise.promise();
    StringBuilder query =
        new StringBuilder(
            INSERT_REVOKE_TOKEN_SQL
                .replace("$1", userid)
                .replace("$2", LocalDateTime.now().toString()));

    LOGGER.trace("query : " + query);

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("sub", userid);
    rmqMessage.put("expiry", LocalDateTime.now().toString());

    postgresService
        .executeQuery(query.toString())
        .compose(
            pgHandler -> {
              return dataBrokerService.publishMessage(
                  rmqMessage, TOKEN_INVALID_EX, TOKEN_INVALID_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              LOGGER.info("Successfully revoked");
              /*Future.future(fu -> updateAuditTable(context));*/
              promise.complete(new ResultModel());
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });
    return promise.future();
  }

  @Override
  public Future<ResultModel> createUniqueAttribute(String id, String attribute) {
    Promise<ResultModel> promise = Promise.promise();
    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", attribute);
    rmqMessage.put("eventType", BroadcastEventType.CREATE);

    StringBuilder query =
        new StringBuilder(INSERT_UNIQUE_ATTR_SQL.replace("$1", id).replace("$2", attribute));
    LOGGER.debug("query : " + query);

    postgresService
        .executeQuery(query.toString())
        .compose(
            pgHandler -> {
              return dataBrokerService.publishMessage(
                  rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              /*Future.future(fu -> updateAuditTable(context));*/
              promise.complete(new ResultModel());
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });
    return promise.future();
  }

  @Override
  public Future<ResultModel> updateUniqueAttribute(String id, String attribute) {
    Promise<ResultModel> promise = Promise.promise();
    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", attribute);
    rmqMessage.put("eventType", BroadcastEventType.UPDATE);
    String query = UPDATE_UNIQUE_ATTR_SQL.replace("$2", id).replace("$1", attribute);
    LOGGER.debug("query : " + query);

    postgresService
        .executeQuery(query)
        .compose(
            pgHandler -> {
              return dataBrokerService.publishMessage(
                  rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              /*Future.future(fu -> updateAuditTable(context));*/
              promise.complete(new ResultModel());
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });
    return promise.future();
  }

  @Override
  public Future<ResultModel> deleteUniqueAttribute(String id) {
    Promise<ResultModel> promise = Promise.promise();
    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", "dummy_attribute");
    rmqMessage.put("eventType", BroadcastEventType.DELETE);

    String query = DELETE_UNIQUE_ATTR_SQL.replace("$1", id);
    LOGGER.trace("query : " + query);

    JsonObject queryparams = new JsonObject();
    postgresService
        .executePreparedQuery(query, queryparams)
        .compose(
            pgHandler -> {
              return dataBrokerService.publishMessage(
                  rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              /*Future.future(fu -> updateAuditTable(context));*/
              promise.complete(new ResultModel());
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });
    return promise.future();
  }
}
