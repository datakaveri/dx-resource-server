package iudx.resource.server.apiserver.subscription.service;

import static iudx.resource.server.apiserver.subscription.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_GROUP;
import static iudx.resource.server.cache.util.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static iudx.resource.server.common.HttpStatusCode.NOT_FOUND;
import static iudx.resource.server.databroker.util.Util.getResponseJson;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import iudx.resource.server.apiserver.subscription.model.*;
import iudx.resource.server.apiserver.subscription.util.SubsType;
import iudx.resource.server.cache.service.CacheService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.model.RegisterQueueModel;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.databroker.util.PermissionOpType;
import org.cdpg.dx.databroker.util.Vhosts;

public class SubscriptionServiceImpl implements SubscriptionService {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionServiceImpl.class);
  private final PostgresService postgresService;
  private final DataBrokerService dataBrokerService;
  private final CacheService cacheService;

  public SubscriptionServiceImpl(
      PostgresService postgresService,
      DataBrokerService dataBrokerService,
      CacheService cacheService) {
    this.postgresService = postgresService;
    this.dataBrokerService = dataBrokerService;
    this.cacheService = cacheService;
  }
//TODO: Need to correct postgresmodel
  private static InsertQuery createSubQuery(
      PostSubscriptionModel postSubscriptionModel,
      SubscriptionData subscriptionDataResult,
      SubsType subsType,
      String type,
      String queueName) {

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
            queueName,
            subsType.type,
            queueName,
            postSubscriptionModel.getEntities(),
            postSubscriptionModel.getExpiry(),
            subscriptionDataResult.cacheResult().getString("name"),
            subscriptionDataResult.cacheResult().toString(),
            postSubscriptionModel.getUserId(),
            subscriptionDataResult.cacheResult().getString(RESOURCE_GROUP),
            subscriptionDataResult.cacheResult().getString("provider"),
            postSubscriptionModel.getDelegatorId(),
            type);
    return new InsertQuery("subscriptions", columns, values);
  }
    //TODO: Need to correct postgresmodel
  private static InsertQuery appendSubsQuery(
      String subId,
      String entities,
      JsonObject appendSubscriptionResult,
      SubsType subType,
      String type,
      PostSubscriptionModel postSubscriptionModel) {

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
            subId,
            subType.type,
            subId,
            entities,
            postSubscriptionModel.getExpiry(),
            appendSubscriptionResult.getString("name"),
            appendSubscriptionResult.toString(),
            postSubscriptionModel.getUserId(),
            appendSubscriptionResult.getString(RESOURCE_GROUP),
            appendSubscriptionResult.getString("provider"),
            postSubscriptionModel.getDelegatorId(),
            type);
    return new InsertQuery("subscriptions", columns, values);
  }

  @Override
  public Future<GetSubscriptionResult> getSubscription(String subscriptionId, String subType) {
    LOGGER.info("getSubscription() method started");
    Promise<GetSubscriptionResult> promise = Promise.promise();
    LOGGER.info("sub id :: " + subscriptionId);
    getEntityName(subscriptionId)
        .compose(
            postgresSuccess -> {
              LOGGER.debug("entities found {}", postgresSuccess);
              return dataBrokerService
                  .listQueue(subscriptionId, Vhosts.IUDX_PROD)
                  .map(listStream -> new GetSubscriptionResult(listStream, postgresSuccess));
            })
        .onComplete(
            getDataBroker -> {
              if (getDataBroker.succeeded()) {
                promise.complete(getDataBroker.result());
              } else {
                promise.fail(getDataBroker.cause());
              }
            });
    return promise.future();
  }

  @Override
  public Future<RegisterQueueModel> createSubscription(
      PostSubscriptionModel postSubscriptionModel) {
    LOGGER.info("createSubscription() method started");
    Promise<RegisterQueueModel> promise = Promise.promise();
    SubsType subType = SubsType.valueOf(postSubscriptionModel.getSubscriptionType());
    String entities = postSubscriptionModel.getEntities();
    JsonObject cacheJson = new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);

    String queueName = postSubscriptionModel.getUserId() + "/" + postSubscriptionModel.getName();

    dataBrokerService
        .registerQueue(postSubscriptionModel.getUserId(), queueName, Vhosts.IUDX_PROD)
        .compose(
            registerQueueHandler -> {
              LOGGER.debug("registerQueueHandler success");
              return cacheService
                  .get(cacheJson)
                  .map(cacheResult -> new SubscriptionData(cacheResult, registerQueueHandler));
            })
        .compose(
            subsDataResult -> {
              LOGGER.debug("subsDataResult success");
              Set<String> type =
                  new HashSet<String>(subsDataResult.cacheResult().getJsonArray("type").getList());
              Set<String> itemTypeSet =
                  type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
              itemTypeSet.retainAll(ITEM_TYPES);

              String resourceGroup;
              String routingKey;
              if (!itemTypeSet.contains("Resource")) {
                resourceGroup = subsDataResult.cacheResult().getString("id");
                routingKey = resourceGroup + DATA_WILDCARD_ROUTINGKEY;
              } else {
                resourceGroup = subsDataResult.cacheResult().getString("resourceGroup");
                routingKey = resourceGroup + "/." + postSubscriptionModel.getEntities();
              }
              return dataBrokerService
                  .queueBinding(resourceGroup, queueName, routingKey, Vhosts.IUDX_PROD)
                  .map(queueBinding -> subsDataResult);
            })
        .compose(
            queueBindingHandler -> {
              LOGGER.debug("binding Queue successful");
              return dataBrokerService
                  .updatePermission(
                      postSubscriptionModel.getUserId(),
                      queueName,
                      PermissionOpType.ADD_READ,
                      Vhosts.IUDX_PROD)
                  .map(updatePermission -> queueBindingHandler);
            })
        .compose(
            updateHandler -> {
              LOGGER.debug("update permission successful");
              String type =
                  updateHandler.cacheResult().containsKey(RESOURCE_GROUP)
                      ? "RESOURCE"
                      : "RESOURCE_GROUP";
              InsertQuery insertQuery =
                  createSubQuery(postSubscriptionModel, updateHandler, subType, type, queueName);
              return postgresService
                  .insert(insertQuery)
                  .map(postgres -> updateHandler.registerQueueModel());
            })
        .onSuccess(
            successHandler -> {
              LOGGER.debug("update permission successful");
              promise.complete(successHandler);
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("failed");
              promise.fail(failureHandler);
            });
    return promise.future();
  }
  //TODO: Need to correct postgresmodel
  @Override
  public Future<String> updateSubscription(String entities, String queueName, String expiry) {
    LOGGER.info("updateSubscription() method started");
    Promise<String> promise = Promise.promise();
    List<String> columns = List.of("*");
    List<String> conditionColumns = List.of("queue_name", "entity");
    ConditionComponent conditionComponent = new Condition(null, null, null);
    // ConditionComponent conditionComponent1 = new ConditionGroup(null,null);
    SelectQuery selectQuery =
        new SelectQuery("subscriptions", columns, null, null, null, null, null);

    postgresService
        .select(selectQuery)
        .compose(
            selectQueryHandler -> {
              List<JsonObject> resultRow = selectQueryHandler.rows();
              LOGGER.debug("selectQueryHandler   " + selectQueryHandler.rows());
              if (resultRow.isEmpty()) {
                JsonObject failJson =
                    getResponseJson(
                        NOT_FOUND.getUrn(),
                        NOT_FOUND.getValue(),
                        NOT_FOUND.getDescription(),
                        "Subscription not found for [queue,entity]");
                return Future.failedFuture(failJson.toString());
              }

              UpdateQuery updateQuery =
                  new UpdateQuery("subscriptions", null, null, null, null, null);
              return postgresService.update(updateQuery);
            })
        .onSuccess(
            pgHandler -> {
              /*List<String> resultEntities = new ArrayList<String>();
              resultEntities.add(entities);*/
              promise.complete(entities);
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });

    return promise.future();
  }
    //TODO: Need to correct postgresmodel
  @Override
  public Future<String> appendSubscription(
      PostSubscriptionModel postSubscriptionModel, String subsId) {
    LOGGER.info("appendSubscription() method started");
    String entities = postSubscriptionModel.getEntities();
    JsonObject cacheJson = new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);
    SubsType subType = SubsType.valueOf(postSubscriptionModel.getSubscriptionType());
    Promise<String> promise = Promise.promise();

    cacheService
        .get(cacheJson)
        .compose(
            cacheResult -> {
              Set<String> type = new HashSet<String>(cacheResult.getJsonArray("type").getList());
              Set<String> itemTypeSet =
                  type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
              itemTypeSet.retainAll(ITEM_TYPES);

              String resourceGroup;
              String routingKey;
              if (!itemTypeSet.contains("Resource")) {
                resourceGroup = cacheResult.getString("id");
                routingKey = resourceGroup + DATA_WILDCARD_ROUTINGKEY;
              } else {
                resourceGroup = cacheResult.getString("resourceGroup");
                routingKey = resourceGroup + "/." + postSubscriptionModel.getEntities();
              }
              return dataBrokerService
                  .queueBinding(resourceGroup, subsId, routingKey, Vhosts.IUDX_PROD)
                  .map(queueBind -> cacheResult);
            })
        .compose(
            queueBindingHandler -> {
              LOGGER.debug("binding Queue successful");
              return dataBrokerService
                  .updatePermission(
                      postSubscriptionModel.getUserId(),
                      subsId,
                      PermissionOpType.ADD_READ,
                      Vhosts.IUDX_PROD)
                  .map(updatePermission -> queueBindingHandler);
            })
        .compose(
            appendSubscriptionResult -> {
              LOGGER.trace("appendStreaming successful ");

              LOGGER.trace("cacheResult: " + appendSubscriptionResult.toString());
              String type =
                  appendSubscriptionResult.containsKey(RESOURCE_GROUP)
                      ? "RESOURCE"
                      : "RESOURCE_GROUP";

              InsertQuery insertQuery =
                  appendSubsQuery(
                      subsId,
                      entities,
                      appendSubscriptionResult,
                      subType,
                      type,
                      postSubscriptionModel);
              return postgresService.insert(insertQuery);
            })
        .onSuccess(
            subscriptionDataResultSuccess -> {
              promise.complete(postSubscriptionModel.getEntities());
            })
        .onFailure(
            failed -> {
              LOGGER.error("Failed :: " + failed);
              promise.fail(failed);
            });
    return promise.future();
  }

  @Override
  public Future<String> deleteSubscription(String subsId, String subscriptionType, String userid) {
    LOGGER.info("deleteSubscription() method started");
    LOGGER.info("queueName to delete :: " + subsId);
    Promise<String> promise = Promise.promise();
    getEntityName(subsId)
        .compose(
            foundId -> {
              LOGGER.debug("entities found {}", foundId);
              return deleteSubscriptionFromPg(subsId).map(result -> foundId);
            })
        .compose(
            postgresSuccess -> {
              return dataBrokerService
                  .deleteQueue(subsId, userid, Vhosts.IUDX_PROD)
                  .map(result -> postgresSuccess);
            })
        .onComplete(
            deleteDataBroker -> {
              if (deleteDataBroker.succeeded()) {
                promise.complete(deleteDataBroker.result());
              } else {
                promise.fail(deleteDataBroker.cause());
              }
            });
    return promise.future();
  }
    //TODO: Need to correct postgresmodel
  private Future<Void> deleteSubscriptionFromPg(String subscriptionId) {
    Promise<Void> promise = Promise.promise();
    /*String deleteQueueQuery = DELETE_SUB_SQL.replace("$1", subscriptionId);
    LOGGER.trace("delete query- " + deleteQueueQuery);*/

    ConditionComponent conditionComponent =
        new Condition("queue_name", Condition.Operator.EQUALS, null);
    DeleteQuery deleteQuery = new DeleteQuery("subscriptions", conditionComponent, null, null);

    postgresService
        .delete(deleteQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                LOGGER.debug("deleted from postgres");
                promise.complete();
              } else {
                LOGGER.error("fail here");
                promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
              }
            });
    return promise.future();
  }
    //TODO: Need to correct postgresmodel
  @Override
  public Future<List<SubscriberDetails>> getAllSubscriptionQueueForUser(String userId) {
    LOGGER.info("getAllSubscriptionQueueForUser() method started");
    Promise<List<SubscriberDetails>> promise = Promise.promise();
    /*String query = GET_ALL_QUEUE.replace("$1", userId);*/
    List<String> columns = List.of();
    SelectQuery selectQuery = new SelectQuery("subscriptions", null, null, null, null, null, null);
    /*LOGGER.debug("query: " + query);*/
    postgresService
        .select(selectQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                List<JsonObject> result = pgHandler.result().rows();
                List<SubscriberDetails> subscriberDetails =
                    result.stream()
                        .map(obj -> new SubscriberDetails(obj))
                        .collect(Collectors.toList());
                promise.complete(subscriberDetails);
              } else {
                promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
              }
            });
    return promise.future();
  }
    //TODO: Need to correct postgresmodel
  private Future<String> getEntityName(String subscriptionID) {
    Promise<String> promise = Promise.promise();
    String getEntityNameQuery = ENTITY_QUERY.replace("$0", subscriptionID);
    LOGGER.trace("query- " + getEntityNameQuery);

    ConditionComponent conditionComponent =
        new Condition("queue_name", Condition.Operator.EQUALS, null);
    SelectQuery selectQuery =
        new SelectQuery(
            "subscriptions", List.of("entity"), conditionComponent, null, null, null, null);

    postgresService
        .select(selectQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded() && !pgHandler.result().rows().isEmpty()) {
                String entities = pgHandler.result().rows().get(0).getString("entity");
                LOGGER.debug("Entities: " + entities);
                promise.complete(entities);
              } else {
                LOGGER.info("Empty response from database. Entities not found");
                promise.fail(new ServiceException(4, NOT_FOUND.getDescription()));
              }
            });
    return promise.future();
  }
}
