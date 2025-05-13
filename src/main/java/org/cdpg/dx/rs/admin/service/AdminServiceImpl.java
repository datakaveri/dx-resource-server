package org.cdpg.dx.rs.admin.service;

import static org.cdpg.dx.common.ErrorCode.ERROR_INTERNAL_SERVER;
import static org.cdpg.dx.common.ErrorMessage.INTERNAL_SERVER_ERROR;
import static org.cdpg.dx.rs.admin.util.Constants.TOKEN_INVALID_EX;
import static org.cdpg.dx.rs.admin.util.Constants.TOKEN_INVALID_EX_ROUTING_KEY;
import static org.cdpg.dx.rs.admin.util.Constants.UNIQUE_ATTR_EX;
import static org.cdpg.dx.rs.admin.util.Constants.UNIQUE_ATTR_EX_ROUTING_KEY;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.rs.admin.util.BroadcastEventType;

public class AdminServiceImpl implements AdminService {
  private static final Logger LOGGER = LogManager.getLogger(AdminServiceImpl.class);
  private final PostgresService postgresService;
  private final DataBrokerService dataBrokerService;

  public AdminServiceImpl(PostgresService postgresService, DataBrokerService dataBrokerService) {
    this.postgresService = postgresService;
    this.dataBrokerService = dataBrokerService;
  }

  private static SelectQuery selectRevokedTokenQuery(String userId) {
    Condition condition = new Condition("_id", Condition.Operator.EQUALS, List.of(userId));
    return new SelectQuery("revoked_tokens", List.of("*"), condition, null, null, null, null);
  }

  private static UpdateQuery getUpdateRevokeTokenQuery(String userId) {
    Condition condition = new Condition("_id", Condition.Operator.EQUALS, List.of(userId));
    return new UpdateQuery(
        "revoked_tokens",
        List.of("expiry"),
        List.of(LocalDateTime.now().toString()),
        condition,
        null,
        null);
  }

  @Override
  public Future<Void> revokedTokenRequest(String userId) {
      LOGGER.debug("Inside revoked Token request");
    Promise<Void> promise = Promise.promise();
    SelectQuery selectQuery = selectRevokedTokenQuery(userId);
    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("sub", userId);
    rmqMessage.put("expiry", LocalDateTime.now().toString());
    postgresService
        .select(selectQuery)
        .compose(
            pgSuccess -> {
              if (pgSuccess.getRows().isEmpty()) {
                InsertQuery insertQuery =
                    new InsertQuery(
                        "revoked_tokens",
                        List.of("_id", "expiry"),
                        List.of(userId, LocalDateTime.now().toString()));
                return postgresService
                    .insert(insertQuery)
                    .onSuccess(
                        success -> {
                          LOGGER.info("inserted successfully");
                        });
              } else {
                UpdateQuery updateQuery = getUpdateRevokeTokenQuery(userId);
                return postgresService
                    .update(updateQuery)
                    .onSuccess(
                        success -> {
                          LOGGER.info("updated successfully");
                        });
              }
            })
        .compose(
            pgHandler -> {
              LOGGER.debug("postgres success");
              return dataBrokerService.publishMessageInternal(
                  rmqMessage, TOKEN_INVALID_EX, TOKEN_INVALID_EX_ROUTING_KEY);
            })
        .onSuccess(
            success -> {
              LOGGER.info("Successfully revoked");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error("Failed to query" + failure);
              promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
            });

    return promise.future();
  }

  @Override
  public Future<Void> createUniqueAttribute(String id, String attribute) {
    Promise<Void> promise = Promise.promise();
    InsertQuery insertQuery =
        new InsertQuery(
            "unique_attributes",
            List.of("resource_id", "unique_attribute"),
            List.of(id, attribute));
    postgresService
        .insert(insertQuery)
        .compose(
            pgHandler -> {
              JsonObject rmqMessage = new JsonObject();
              rmqMessage.put("id", id);
              rmqMessage.put("unique-attribute", attribute);
              rmqMessage.put("eventType", BroadcastEventType.CREATE);
              return dataBrokerService.publishMessageInternal(
                  rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              LOGGER.info("Successfully created");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });
    return promise.future();
  }

  @Override
  public Future<Void> updateUniqueAttribute(String id, String attribute) {
    Promise<Void> promise = Promise.promise();
    Condition condition = new Condition("resource_id", Condition.Operator.EQUALS, List.of(id));
    UpdateQuery updateQuery =
        new UpdateQuery(
            "unique_attributes",
            List.of("unique_attribute"),
            List.of(attribute),
            condition,
            null,
            null);
    postgresService
        .update(updateQuery)
        .compose(
            pgHandler -> {
              JsonObject rmqMessage = new JsonObject();
              rmqMessage.put("id", id);
              rmqMessage.put("unique-attribute", attribute);
              rmqMessage.put("eventType", BroadcastEventType.UPDATE);
              return dataBrokerService.publishMessageInternal(
                  rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              LOGGER.info("Successfully updated");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });
    return promise.future();
  }

  @Override
  public Future<Void> deleteUniqueAttribute(String id) {
    Promise<Void> promise = Promise.promise();
    Condition condition = new Condition("resource_id", Condition.Operator.EQUALS, List.of(id));
    DeleteQuery deleteQuery = new DeleteQuery("unique_attributes", condition, null, null);
    postgresService
        .delete(deleteQuery)
        .compose(
            pgHandler -> {
              JsonObject rmqMessage = new JsonObject();
              rmqMessage.put("id", id);
              rmqMessage.put("unique-attribute", "dummy_attribute");
              rmqMessage.put("eventType", BroadcastEventType.DELETE);
              return dataBrokerService.publishMessageInternal(
                  rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              LOGGER.info("Successfully deleted");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });
    return promise.future();
  }
}
