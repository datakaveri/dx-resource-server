package org.cdpg.dx.rs.subscription.dao.impl;

import static org.cdpg.dx.common.ErrorCode.ERROR_INTERNAL_SERVER;
import static org.cdpg.dx.common.ErrorCode.ERROR_NOT_FOUND;
import static org.cdpg.dx.common.ErrorMessage.INTERNAL_SERVER_ERROR;
import static org.cdpg.dx.common.ErrorMessage.NOT_FOUND_ERROR;
import static org.cdpg.dx.rs.subscription.util.Constants.SUBSCRIPTION_TABLE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.serviceproxy.ServiceException;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.rs.subscription.dao.SubscriptionServiceDAO;
import org.cdpg.dx.rs.subscription.model.SubscriptionDTO;

public class SubscriptionServiceDAOImpl implements SubscriptionServiceDAO {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionServiceDAOImpl.class);
  private final PostgresService postgresService;
  String subs_table = SUBSCRIPTION_TABLE;

  public SubscriptionServiceDAOImpl(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  @Override
  public Future<String> getEntityIdByQueueName(String subscriptionId) {
    Promise<String> promise = Promise.promise();
    Condition conditionComponent =
        new Condition("queue_name", Condition.Operator.EQUALS, List.of(subscriptionId));
    SelectQuery selectQuery =
        new SelectQuery(subs_table, List.of("entity"), conditionComponent, null, null, null, null);
    postgresService
        .select(selectQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                JsonArray rows = pgHandler.result().getRows();
                if (rows.isEmpty()) {
                  LOGGER.info("No rows found. EntityId not found");
                  promise.complete("Not Found");
                } else {
                  Optional<String> entityId =
                      Optional.ofNullable(rows.getJsonObject(0).getString("entity"));
                  LOGGER.debug("EntityId {}", entityId.orElse("Not Found"));
                  promise.complete(entityId.orElse("Not Found"));
                }
              } else {
                LOGGER.info("Empty response from database. EntityId not found");
                promise.fail(new ServiceException(ERROR_NOT_FOUND, NOT_FOUND_ERROR));
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonArray> getSubscriptionByUserId(String userId) {
    Promise<JsonArray> promise = Promise.promise();
    List<String> columns = List.of("queue_name as queueName", "entity", "dataset_json as catItem");
    Condition conditionComponent =
        new Condition("user_id", Condition.Operator.EQUALS, List.of(userId));
    SelectQuery selectQuery =
        new SelectQuery(subs_table, columns, conditionComponent, null, null, null, null);
    postgresService
        .select(selectQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                JsonArray result = pgHandler.result().getRows();
                promise.complete(result);
              } else {
                promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
              }
            });
    return promise.future();
  }

  @Override
  public Future<Void> deleteSubscriptionBySubId(String subscriptionId) {
    Promise<Void> promise = Promise.promise();
    Condition condition =
        new Condition("queue_name", Condition.Operator.EQUALS, List.of(subscriptionId));
    DeleteQuery deleteQuery = new DeleteQuery(subs_table, condition, null, null);

    postgresService
        .delete(deleteQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                LOGGER.debug("deleted from postgres");
                promise.complete();
              } else {
                LOGGER.error("fail :: {}", pgHandler.cause().getMessage());
                promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonArray> getSubscriptionByQueueNameAndEntityId(
      String subscriptionId, String entityId) {
    Promise<JsonArray> promise = Promise.promise();
    SelectQuery selectQuery = getSelectQueryForQueueNameAndEntityId(subscriptionId, entityId);
    postgresService
        .select(selectQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                JsonArray result = pgHandler.result().getRows();
                promise.complete(result);
              } else {
                promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
              }
            });
    return promise.future();
  }

  private SelectQuery getSelectQueryForQueueNameAndEntityId(
      String subscriptionId, String entityId) {
    Condition queueNameCondition =
        new Condition("queue_name", Condition.Operator.EQUALS, List.of(subscriptionId));
    Condition entityCondition =
        new Condition("entity", Condition.Operator.EQUALS, List.of(entityId));
    Condition condition =
        new Condition(List.of(queueNameCondition, entityCondition), Condition.LogicalOperator.AND);
    return new SelectQuery(subs_table, List.of("*"), condition, null, null, null, null);
  }

  @Override
  public Future<Void> updateSubscriptionExpiryByQueueNameAndEntityId(
      String queueName, String entityId, String expiry) {
    Promise<Void> promise = Promise.promise();
    Condition queueNameCondition =
        new Condition("queue_name", Condition.Operator.EQUALS, List.of(queueName));
    Condition entityCondition =
        new Condition("entity", Condition.Operator.EQUALS, List.of(entityId));

    Condition condition =
        new Condition(List.of(queueNameCondition, entityCondition), Condition.LogicalOperator.AND);
    UpdateQuery updateQuery =
        new UpdateQuery(subs_table, List.of("expiry"), List.of(expiry), condition, null, null);

    postgresService
        .update(updateQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                LOGGER.info("Success updated expiry");
                promise.complete();
              } else {
                LOGGER.error("failed to update expiry");
                promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
              }
            });

    return promise.future();
  }

  @Override
  public Future<Void> insertSubscription(SubscriptionDTO subscriptionDTO) {
    Promise<Void> promise = Promise.promise();
    List<String> columns =
        List.of(
            "_id",
            "_type",
            "queue_name",
            "entity",
            "expiry",
            "dataset_name",
            "dataset_json",
            "user_id",
            "resource_group",
            "provider_id",
            "delegator_id",
            "item_type");
    List<Object> values =
        List.of(
            subscriptionDTO._id(),
            subscriptionDTO._type(),
            subscriptionDTO.queue_name(),
            subscriptionDTO.entityId(),
            subscriptionDTO.expiry(),
            subscriptionDTO.dataset_name(),
            subscriptionDTO.dataset_json(),
            subscriptionDTO.user_id(),
            subscriptionDTO.resource_group(),
            subscriptionDTO.provider_id(),
            subscriptionDTO.delegator_id(),
            subscriptionDTO.item_type());

    postgresService
        .insert(new InsertQuery(subs_table, columns, values))
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                LOGGER.info("Success inserted subs data");
                promise.complete();
              } else {
                LOGGER.error("failed to insert subs data");
                promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
              }
            });

    return promise.future();
  }
}
