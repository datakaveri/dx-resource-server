package org.cdpg.dx.rs.subscription.dao.impl;
import static org.cdpg.dx.rs.subscription.util.Constants.SUBSCRIPTION_TABLE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.base.dao.AbstractBaseDAO;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.rs.subscription.dao.SubscriptionServiceDAO;
import org.cdpg.dx.rs.subscription.model.SubscriptionDTO;

public class SubscriptionServiceDAOImpl extends AbstractBaseDAO<SubscriptionDTO>
    implements SubscriptionServiceDAO {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionServiceDAOImpl.class);

  public SubscriptionServiceDAOImpl(PostgresService postgresService) {
    super(postgresService, SUBSCRIPTION_TABLE, "_id", SubscriptionDTO::fromJson);
  }

  @Override
  public Future<String> getEntityIdByQueueName(String subscriptionId) {
    Promise<String> promise = Promise.promise();
    Condition conditionComponent =
        new Condition("queue_name", Condition.Operator.EQUALS, List.of(subscriptionId));
    SelectQuery selectQuery =
        new SelectQuery(tableName, List.of("entity"), conditionComponent, null, null, null, null);
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
                promise.fail(pgHandler.cause());
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
        new SelectQuery(tableName, columns, conditionComponent, null, null, null, null);
    postgresService
        .select(selectQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                JsonArray result = pgHandler.result().getRows();
                promise.complete(result);
              } else {
                promise.fail(pgHandler.cause());
              }
            });
    return promise.future();
  }

  @Override
  public Future<Void> deleteSubscriptionBySubId(String subscriptionId) {
    Promise<Void> promise = Promise.promise();
    Condition condition =
        new Condition("queue_name", Condition.Operator.EQUALS, List.of(subscriptionId));
    DeleteQuery deleteQuery = new DeleteQuery(tableName, condition, null, null);

    postgresService
        .delete(deleteQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                LOGGER.debug("deleted from postgres");
                promise.complete();
              } else {
                LOGGER.error("fail :: {}", pgHandler.cause().getMessage());
                promise.fail(pgHandler.cause());
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
                promise.fail(pgHandler.cause());
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
    return new SelectQuery(tableName, List.of("*"), condition, null, null, null, null);
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
        new UpdateQuery(tableName, List.of("expiry"), List.of(expiry), condition, null, null);

    postgresService
        .update(updateQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                LOGGER.info("Success updated expiry");
                promise.complete();
              } else {
                LOGGER.error("failed to update expiry");
                promise.fail(pgHandler.cause());
              }
            });

    return promise.future();
  }

  @Override
  public Future<Void> insertSubscription(SubscriptionDTO subscriptionDTO) {
    Promise<Void> promise = Promise.promise();
    var columnNameAndValues = subscriptionDTO.toNonEmptyFieldsMap();

    postgresService
        .insert(
            new InsertQuery(
                tableName,
                List.copyOf(columnNameAndValues.keySet()),
                List.copyOf(columnNameAndValues.values())))
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                LOGGER.info("Success inserted subs data");
                promise.complete();
              } else {
                LOGGER.error("failed to insert subs data");
                promise.fail(pgHandler.cause());
              }
            });

    return promise.future();
  }
}
