package iudx.resource.server.databroker.service;

import static iudx.resource.server.apiserver.subscription.util.Constants.RESULTS;
import static iudx.resource.server.apiserver.util.Constants.SUBSCRIPTION_ID;
import static iudx.resource.server.apiserver.util.Constants.USER_ID;
import static iudx.resource.server.databroker.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.subscription.model.SubscriptionImplModel;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.databroker.util.PermissionOpType;
import iudx.resource.server.databroker.util.RabbitClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataBrokerServiceImpl implements DataBrokerService{
    private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceImpl.class);
    private final String amqpUrl;
    private final int amqpPort;
    CacheService cacheService;
    private String vhostProd;
    private String iudxInternalVhost;
    private String externalVhost;
    private RabbitClient rabbitClient;

    public DataBrokerServiceImpl(RabbitClient client, String amqpUrl, int amqpPort, CacheService cacheService, String iudxInternalVhost,String prodVhost,String externalVhost) {
        this.rabbitClient = client;
        this.amqpUrl = amqpUrl;
        this.amqpPort = amqpPort;
        this.cacheService = cacheService;
        this.vhostProd = prodVhost;
        this.iudxInternalVhost = iudxInternalVhost;
        this.externalVhost = externalVhost;
    }

    @Override
    public Future<JsonObject> deleteStreamingSubscription(JsonObject request) {
        LOGGER.trace("Info : SubscriptionService#deleteStreamingSubscription() started");
        Promise<JsonObject> promise = Promise.promise();
        JsonObject deleteStreamingSubscription = new JsonObject();
        if (request != null && !request.isEmpty()) {
            //TODO: To create Model
            String queueName = request.getString(SUBSCRIPTION_ID);
            String userid = request.getString("USER_ID");
            JsonObject requestBody = new JsonObject();
            requestBody.put(QUEUE_NAME, queueName);
            Future<JsonObject> result = rabbitClient.deleteQueue(requestBody,vhostProd);
            result.onComplete(
                    resultHandler -> {
                        if (resultHandler.succeeded()) {
                            JsonObject deleteQueueResponse = (JsonObject) resultHandler.result();
                            if (deleteQueueResponse.containsKey("TITLE")
                                    && deleteQueueResponse.getString("TITLE").equalsIgnoreCase("FAILURE")) {
                                LOGGER.debug("failed :: Response is " + deleteQueueResponse);
                                promise.fail(deleteQueueResponse.toString());
                            } else {
                               /* deleteStreamingSubscription.mergeIn(
                                        getResponseJson(
                                                ResponseUrn.SUCCESS_URN.getUrn(),
                                                HttpStatus.SC_OK,
                                                SUCCESS,
                                                "Subscription deleted Successfully"));*/
                                Future.future(
                                        fu ->
                                                rabbitClient.updateUserPermissions(
                                                        vhostProd, userid, PermissionOpType.DELETE_READ, queueName));
                                promise.complete(deleteStreamingSubscription);
                            }
                        }

                        if (resultHandler.failed()) {
                            LOGGER.error("failed ::" + resultHandler.cause());

                            /*promise.fail(
                                    getResponseJson(INTERNAL_ERROR_CODE, ERROR, QUEUE_DELETE_ERROR).toString());*/

                        }
                    });
        } else {
            /*promise.fail(finalResponse.toString());*/
        }
        return promise.future();
    }

    @Override
    public Future<JsonObject> registerStreamingSubscription(SubscriptionImplModel subscriptionImplModel) {
        LOGGER.trace("Info : SubscriptionService#registerStreamingSubscription() started");
        Promise<JsonObject> promise = Promise.promise();
        JsonObject registerStreamingSubscriptionResponse = new JsonObject();
        JsonObject requestjson = new JsonObject();
        ResultContainer resultContainer = new ResultContainer();
        if (subscriptionImplModel != null ) {
            String userid = subscriptionImplModel.getControllerModel().getUserId();
            String queueName = userid + "/" + subscriptionImplModel.getControllerModel().getName();
            /*JsonArray entitites = request.getJsonArray("ENTITIES");*/
            String entities  = subscriptionImplModel.getControllerModel().getEntities();
            if(entities==null)
            {
                if(entities.isEmpty() || entities.isBlank()){

                }
                return null;
                // TODO:throw error or move this check to somewhere else
            }
            /*requestjson.put(QUEUE_NAME, queueName);*/
            LOGGER.debug("queue name is databroker subscription  = {}", queueName);

            Future<JsonObject> resultCreateUser = rabbitClient.createUserIfNotExist(userid, vhostProd).compose(
                    checkUserExist->{
                        LOGGER.debug("success :: createUserIfNotExist " + checkUserExist);

                        resultContainer.apiKey = checkUserExist.getString(APIKEY);
                        resultContainer.userId = checkUserExist.getString(USER_ID);

                        /*return rabbitClient.createQueue(requestjson,vhostProd);*/
                        return rabbitClient.createQueue(queueName, vhostProd);
                    }).compose(createQueue->{
                     resultContainer.isQueueCreated = true;

                    String routingKey = entities;
                    LOGGER.debug("Info : routingKey is " + routingKey);

                    JsonArray array = new JsonArray();
                    String exchangeName;
                    if (isGroupResource(new JsonObject().put("type", subscriptionImplModel.getResourcegroup()))) {
                        exchangeName = routingKey;
                        array.add(exchangeName + DATA_WILDCARD_ROUTINGKEY);
                    } else {
                        exchangeName = subscriptionImplModel.getResourcegroup();
                        array.add(exchangeName + "/." + routingKey);
                    }
                    LOGGER.debug(" Exchange name = {}", exchangeName);
                    JsonObject json = new JsonObject();
                    json.put(EXCHANGE_NAME, exchangeName);
                    json.put(QUEUE_NAME, queueName);
                    json.put(/*"ENTITIES"*/"entities", array);
                    return rabbitClient.bindQueue(json,vhostProd);
            }).compose(bindQueueSuccess->{
                if(bindQueueSuccess.containsKey("TITLE")
                        && bindQueueSuccess.getString("TITLE").equalsIgnoreCase("FAILURE")){
                    LOGGER.error("failed ::" + bindQueueSuccess.toString());
                    return rabbitClient.deleteQueue(requestjson,vhostProd);
                }
                else{
                    LOGGER.debug("binding Queue successful");
                    return rabbitClient.updateUserPermissions(vhostProd,userid,PermissionOpType.ADD_READ,queueName);
                }

            }).onComplete(updateUserPermissionHandler->{
                if(updateUserPermissionHandler.succeeded()){
                    registerStreamingSubscriptionResponse.put(
                            USER_NAME, resultContainer.userId);
                    registerStreamingSubscriptionResponse.put(
                            APIKEY, resultContainer.apiKey);
                    registerStreamingSubscriptionResponse.put(
                            ID, queueName);
                    registerStreamingSubscriptionResponse.put(
                            URL, this.amqpUrl);
                    registerStreamingSubscriptionResponse.put(
                            PORT, this.amqpPort);
                    registerStreamingSubscriptionResponse.put(
                            VHOST, vhostProd);

                    JsonObject response = new JsonObject();
                    response.put(
                            "TYPE", ResponseUrn.SUCCESS_URN.getUrn());
                    response.put("TITLE", "success");
                    response.put(
                            RESULTS,
                            new JsonArray()
                                    .add(
                                            registerStreamingSubscriptionResponse));
                    LOGGER.debug("--- Response "+ response);
                    promise.complete(response);
                }
                else{
                    LOGGER.error("failed ::" + updateUserPermissionHandler.cause());
                    Future<JsonObject> resultDeletequeue =
                            rabbitClient.deleteQueue(
                                    requestjson, vhostProd);
                    resultDeletequeue.onComplete(
                            resultHandlerDeletequeue -> {
                                if (resultHandlerDeletequeue
                                        .succeeded()) {
                                        /*promise.fail(
                                                getResponseJson(
                                                        BAD_REQUEST_CODE,
                                                        BAD_REQUEST_DATA,
                                                        BINDING_FAILED)
                                                        .toString());*/
                                }
                            });
                }
            });
        }
        return promise.future();
    }

    /*@Override
    public Future<JsonObject> registerStreamingSubscription(JsonObject request) {
        LOGGER.trace("Info : SubscriptionService#registerStreamingSubscription() started");
        Promise<JsonObject> promise = Promise.promise();
        JsonObject registerStreamingSubscriptionResponse = new JsonObject();
        JsonObject requestjson = new JsonObject();
        ResultContainer resultContainer = new ResultContainer();
        if (request != null && !request.isEmpty()) {
            String userid = request.getString("USER_ID");
            String queueName = userid + "/" + request.getString("name");
            *//*JsonArray entitites = request.getJsonArray("ENTITIES");*//*
            String entities  = request.getJsonArray("ENTITIES").getString(0);
            if(entities==null)
            {
                if(entities.isEmpty() || entities.isBlank()){

                }
                return null;
                // TODO:throw error or move this check to somewhere else
            }
            requestjson.put(QUEUE_NAME, queueName);
            LOGGER.debug("queue name is databroker subscription  = {}", queueName);

            Future<JsonObject> resultCreateUser = rabbitClient.createUserIfNotExist(userid, vhostProd).compose(
                    checkUserExist->{
                        LOGGER.debug("success :: createUserIfNotExist " + checkUserExist);

                        resultContainer.apiKey = checkUserExist.getString(APIKEY);
                        resultContainer.userId = checkUserExist.getString("USER_ID");

                        return rabbitClient.createQueue(requestjson,vhostProd);
                    }).compose(createQueue->{
                        if(createQueue.containsKey("TITLE")
                                && createQueue.getString("TITLE").equalsIgnoreCase("FAILURE")){
                            LOGGER.error("failed ::" + createQueue);
                            promise.fail(createQueue.toString());
                            return rabbitClient.deleteQueue(requestjson,vhostProd);
                        }else{
                            String routingKey = entities;
                            LOGGER.debug("Info : routingKey is " + routingKey);

                                    JsonArray array = new JsonArray();
                                    String exchangeName;
                                    if (isGroupResource(request)) {
                                        exchangeName = routingKey;
                                        array.add(exchangeName + DATA_WILDCARD_ROUTINGKEY);
                                    } else {
                                        exchangeName = request.getString("resourcegroup");
                                        LOGGER.debug("exchange name  = {} ", exchangeName);
                                        array.add(exchangeName + "/." + routingKey);
                                    }
                                    LOGGER.debug(" Exchange name = {}", exchangeName);
                                    JsonObject json = new JsonObject();
                                    json.put(EXCHANGE_NAME, exchangeName);
                                    json.put(QUEUE_NAME, queueName);
                                    json.put("ENTITIES", array);
                                    return rabbitClient.bindQueue(json,vhostProd);
                            }
                    }).compose(bindQueueSuccess->{
                        if(bindQueueSuccess.containsKey("TITLE")
                                && bindQueueSuccess.getString("TITLE").equalsIgnoreCase("FAILURE")){
                            LOGGER.error("failed ::" + bindQueueSuccess.toString());
                            return rabbitClient.deleteQueue(requestjson,vhostProd);
                        }
                        else{
                                LOGGER.debug("binding Queue successful");
                                return rabbitClient.updateUserPermissions(vhostProd,userid,PermissionOpType.ADD_READ,queueName);
                        }

                    }).onComplete(updateUserPermissionHandler->{
                            if(updateUserPermissionHandler.succeeded()){
                                registerStreamingSubscriptionResponse.put(
                                        USER_NAME, resultContainer.userId);
                                registerStreamingSubscriptionResponse.put(
                                        APIKEY, resultContainer.apiKey);
                                registerStreamingSubscriptionResponse.put(
                                        ID, queueName);
                                registerStreamingSubscriptionResponse.put(
                                        URL, this.amqpUrl);
                                registerStreamingSubscriptionResponse.put(
                                        PORT, this.amqpPort);
                                registerStreamingSubscriptionResponse.put(
                                        VHOST, vhostProd);

                                JsonObject response = new JsonObject();
                                response.put(
                                        "TYPE", ResponseUrn.SUCCESS_URN.getUrn());
                                response.put("TITLE", "success");
                                response.put(
                                        "RESULTS",
                                        new JsonArray()
                                                .add(
                                                        registerStreamingSubscriptionResponse));

                                promise.complete(response);
                    }
                    else{
                        LOGGER.error("failed ::" + updateUserPermissionHandler.cause());
                        Future<JsonObject> resultDeletequeue =
                                rabbitClient.deleteQueue(
                                        requestjson, vhostProd);
                        resultDeletequeue.onComplete(
                                resultHandlerDeletequeue -> {
                                    if (resultHandlerDeletequeue
                                            .succeeded()) {
                                        *//*promise.fail(
                                                getResponseJson(
                                                        BAD_REQUEST_CODE,
                                                        BAD_REQUEST_DATA,
                                                        BINDING_FAILED)
                                                        .toString());*//*
                                    }
                                });
                        }
                 });
            }
        return promise.future();
    }*/

    @Override
    public Future<JsonObject> registerAdaptor(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> deleteAdaptor(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> listAdaptor(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> updateStreamingSubscription(JsonObject request) {
        return null;
    }

    @Override
    public Future<JsonObject> appendStreamingSubscription(JsonObject request) {
        return null;
    }

    @Override
    public Future<JsonObject> listStreamingSubscription(String subscriptionID) {
        LOGGER.trace("Info : SubscriptionService#listStreamingSubscriptions() started");
        String queueName = subscriptionID;

        Promise<JsonObject> promise = Promise.promise();
        if (subscriptionID != null && !subscriptionID.isEmpty()) {
            Future<JsonObject> result = rabbitClient.listQueueSubscribers(queueName,vhostProd).onComplete(
                    resultHandler -> {
                        if (resultHandler.succeeded()) {
                            JsonObject listQueueResponse = (JsonObject) resultHandler.result();
                            if (listQueueResponse.containsKey("TITLE")
                                    && listQueueResponse.getString("TITLE").equalsIgnoreCase("FAILURE")) {
                                LOGGER.error("failed :: Response is " + listQueueResponse);
                                promise.fail(listQueueResponse.toString());
                            } else {
                                LOGGER.debug(listQueueResponse);
                                JsonObject response = new JsonObject();
                                response.put("TYPE", ResponseUrn.SUCCESS_URN.getUrn());
                                response.put("TITLE", "success");
                                response.put(RESULTS, new JsonArray().add(listQueueResponse));

                                promise.complete(response);
                            }
                        }
                        if (resultHandler.failed()) {
                            LOGGER.error("failed ::" + resultHandler.cause());
                            promise.fail(/*getResponseJson(BAD_REQUEST_CODE, ERROR, QUEUE_LIST_ERROR).toString()*/"");
                        }
                    });
        } else {
            /*handler.handle(Future.failedFuture(finalResponse.toString()));*/
            promise.fail(/*getResponseJson(BAD_REQUEST_CODE, ERROR, QUEUE_LIST_ERROR).toString()*/"");
        }
        return promise.future();
    }

    @Override
    public Future<JsonObject> createExchange(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> deleteExchange(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> listExchangeSubscribers(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> createQueue(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> deleteQueue(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> bindQueue(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> unbindQueue(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> createvHost(JsonObject request) {
        return null;
    }

    @Override
    public Future<JsonObject> deletevHost(JsonObject request) {
        return null;
    }

    @Override
    public Future<JsonObject> listvHost(JsonObject request) {
        return null;
    }

    @Override
    public Future<JsonObject> listQueueSubscribers(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> publishFromAdaptor(JsonArray request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> resetPassword(JsonObject request) {
        return null;
    }

    @Override
    public Future<JsonObject> getExchange(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> publishHeartbeat(JsonObject request, String vhost) {
        return null;
    }

    @Override
    public Future<JsonObject> publishMessage(JsonObject body, String toExchange, String routingKey) {
        return null;
    }

    private boolean isGroupResource(JsonObject jsonObject) {
        return jsonObject.getString("type").equalsIgnoreCase("resourceGroup");
    }

    public class ResultContainer {
        public String apiKey;
        public String userId;
        public boolean isQueueCreated = false;
    }
}


 /*

  *//**
   * This method creates user, declares exchange and bind with predefined queues.
   *
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   *//*
  @Override
  public Future<JsonObject> registerAdaptor(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.registerAdapter(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("registerAdaptor resultHandler failed : " + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }
    return promise.future();
  }

  *//**
   * It retrieves exchange is exist
   *
   * @param request which is of type JsonObject
   * @return response which is a Future object of promise of Json type
   *//*
  @Override
  public Future<JsonObject> getExchange(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.getExchange(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("getExchange resultHandler failed : " + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }
    return promise.future();
  }

  *//*
   * overridden method
   *//*

  *//**
   * The deleteAdaptor implements deletion feature for an adaptor(exchange).
   *
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   *//*
  @Override
  public Future<JsonObject> deleteAdaptor(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.deleteAdapter(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("getExchange resultHandler failed : " + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }
    return promise.future();
  }

  *//**
   * The listAdaptor implements the list of bindings for an exchange (source). This method has
   * similar functionality as listExchangeSubscribers(JsonObject) method
   *
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   *//*
  @Override
  public Future<JsonObject> listAdaptor(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.listExchangeSubscribers(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("deleteAdaptor - resultHandler failed : " + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    } else {
      promise.fail(finalResponse.toString());
    }

    return promise.future();
  }

  *//** {@inheritDoc} *//*
  @Override
  public Future<JsonObject> createExchange(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.createExchange(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }
    return promise.future();
  }

  *//** {@inheritDoc} *//*
  @Override
  public Future<JsonObject> deleteExchange(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.deleteExchange(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }
    return null;
  }

  *//** {@inheritDoc} *//*
  @Override
  public Future<JsonObject> listExchangeSubscribers(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.listExchangeSubscribers(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }
    return null;
  }

  *//** {@inheritDoc} *//*
  @Override
  public Future<JsonObject> createQueue(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.createQueue(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }
    return null;
  }

  *//** {@inheritDoc} *//*
  @Override
  public Future<JsonObject> deleteQueue(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.deleteQueue(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }
    return null;
  }

  *//** {@inheritDoc} *//*
  @Override
  public Future<JsonObject> bindQueue(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.bindQueue(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }

    return null;
  }

  *//** {@inheritDoc} *//*
  @Override
  public Future<JsonObject> unbindQueue(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.unbindQueue(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {

              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }

    return null;
  }

  *//** {@inheritDoc} *//*
  @Override
  public Future<JsonObject> createvHost(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.createvHost(request);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }

    return null;
  }

  *//** {@inheritDoc} *//*
  @Override
  public Future<JsonObject> deletevHost(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.deletevHost(request);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }

    return null;
  }

  *//** {@inheritDoc} *//*
  @Override
  public Future<JsonObject> listvHost(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    if (request != null) {
      Future<JsonObject> result = webClient.listvHost(request);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }
    return null;
  }

  *//** {@inheritDoc} *//*
  @Override
  public Future<JsonObject> listQueueSubscribers(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.listQueueSubscribers(request, virtualHost);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error("failed ::" + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    }
    return null;
  }

  *//** {@inheritDoc} *//*
  @Override
  public Future<JsonObject> publishFromAdaptor(JsonArray request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    LOGGER.debug("request JsonArray " + request);

    if (request != null && !request.isEmpty()) {

      JsonObject cacheRequestJson = new JsonObject();
      cacheRequestJson.put("type", CATALOGUE_CACHE);
      cacheRequestJson.put("key", request.getJsonObject(0).getJsonArray("entities").getValue(0));
      cacheService
          .get(cacheRequestJson)
          .onComplete(
              cacheHandler -> {
                if (cacheHandler.succeeded()) {
                  JsonObject cacheResult = cacheHandler.result();
                  String resourceGroupId =
                      cacheResult.containsKey(RESOURCE_GROUP)
                          ? cacheResult.getString(RESOURCE_GROUP)
                          : cacheResult.getString(ID);
                  LOGGER.debug("Info : resourceGroupId  " + resourceGroupId);
                  String id =
                      request.getJsonObject(0).getJsonArray("entities").getValue(0).toString();
                  String routingKey = resourceGroupId + "/." + id;
                  request.remove("entities");

                  for (int i = 0; i < request.size(); i++) {
                    JsonObject jsonObject = request.getJsonObject(i);
                    jsonObject.remove("entities");
                    jsonObject.put("id", id);
                  }
                  LOGGER.trace(request);
                  if (resourceGroupId != null && !resourceGroupId.isBlank()) {
                    LOGGER.debug("Info : routingKey  " + routingKey);
                    Buffer buffer = Buffer.buffer(request.encode());
                    iudxRabbitMqClient.basicPublish(
                        resourceGroupId,
                        routingKey,
                        buffer,
                        resultHandler -> {
                          if (resultHandler.succeeded()) {
                            finalResponse.put(STATUS, HttpStatus.SC_OK);
                            LOGGER.info("Success : Message published to queue");
                            promise.complete(finalResponse);
                          } else {
                            finalResponse.put(TYPE, HttpStatus.SC_BAD_REQUEST);
                            LOGGER.error("Fail : " + resultHandler.cause().toString());
                            promise.fail(resultHandler.cause().getMessage());
                          }
                        });
                  }
                } else {
                  LOGGER.error("Item not found");
                }
              });
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> publishHeartbeat(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    String virtualHost = getVhost(vhost);
    if (request != null && !request.isEmpty()) {
      String adaptor = request.getString(ID);
      String routingKey = request.getString("status");
      if (adaptor != null && !adaptor.isEmpty() && routingKey != null && !routingKey.isEmpty()) {
        JsonObject json = new JsonObject();
        Future<JsonObject> future1 = webClient.getExchange(json.put("id", adaptor), virtualHost);
        future1.onComplete(
            ar -> {
              if (ar.result().getInteger("type") == HttpStatus.SC_OK) {
                json.put("exchangeName", adaptor);
                // exchange found, now get list of all queues which are bound with this exchange
                Future<JsonObject> future2 = webClient.listExchangeSubscribers(json, virtualHost);
                future2.onComplete(
                    rh -> {
                      JsonObject queueList = rh.result();
                      if (queueList != null && queueList.size() > 0) {
                        // now find queues which are bound with given routingKey and publish message
                        queueList.forEach(
                            queue -> {
                              // JsonObject obj = new JsonObject();
                              Map.Entry<String, Object> map = queue;
                              String queueName = map.getKey();
                              JsonArray array = (JsonArray) map.getValue();
                              array.forEach(
                                  rk -> {
                                    if (rk.toString().contains(routingKey)) {
                                      // routingKey matched. now publish message
                                      JsonObject message = new JsonObject();
                                      message.put("body", request.toString());
                                      Buffer buffer = Buffer.buffer(message.toString());
                                      webClient
                                          .getRabbitmqClient()
                                          .basicPublish(
                                              adaptor,
                                              routingKey,
                                              buffer,
                                              resultHandler -> {
                                                if (resultHandler.succeeded()) {
                                                  LOGGER.debug(
                                                      "publishHeartbeat - message "
                                                          + "published to queue [ "
                                                          + queueName
                                                          + " ] for routingKey [ "
                                                          + routingKey
                                                          + " ]");
                                                  response.put("type", "success");
                                                  response.put("queueName", queueName);
                                                  response.put("routingKey", rk.toString());
                                                  response.put("detail", "routingKey matched");
                                                  promise.complete(response);
                                                } else {
                                                  LOGGER.error(
                                                      "publishHeartbeat - some error in publishing "
                                                          + "message to queue [ "
                                                          + queueName
                                                          + " ]. cause : "
                                                          + resultHandler.cause());
                                                  response.put("messagePublished", "failed");
                                                  response.put("type", "error");
                                                  response.put("detail", "routingKey not matched");
                                                  promise.fail(response.toString());
                                                }
                                              });
                                    } else {
                                      LOGGER.error(
                                          "publishHeartbeat - routingKey [ "
                                              + routingKey
                                              + " ] not matched with [ "
                                              + rk.toString()
                                              + " ] for queue [ "
                                              + queueName
                                              + " ]");
                                      promise.fail(
                                          "publishHeartbeat - routingKey [ "
                                              + routingKey
                                              + " ] not matched with [ "
                                              + rk.toString()
                                              + " ] for queue [ "
                                              + queueName
                                              + " ]");
                                    }
                                  });
                            });

                      } else {
                        LOGGER.error(
                            "publishHeartbeat method - Oops !! None queue "
                                + "bound with given exchange");
                        promise.fail(
                            "publishHeartbeat method - Oops !! "
                                + "None queue bound with given exchange");
                      }
                    });

              } else {
                LOGGER.error(
                    "Either adaptor does not exist or some other error to publish message");
                promise.fail(
                    "Either adaptor does not exist or some other error to publish message");
              }
            });
      } else {
        LOGGER.error("publishHeartbeat - adaptor and routingKey not provided to publish message");
        promise.fail("publishHeartbeat - adaptor and routingKey not provided to publish message");
      }

    } else {
      LOGGER.error("publishHeartbeat - request is null to publish message");
      promise.fail("publishHeartbeat - request is null to publish message");
    }

    return promise.future();
  }

  @Override
  public Future<JsonObject> resetPassword(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    JsonObject response = new JsonObject();
    String password = Util.randomPassword.get();
    String userid = request.getString(USER_ID);
    Future<JsonObject> userFuture = webClient.getUserInDb(userid);
    userFuture
        .compose(
            checkUserFut -> {
              return webClient.resetPasswordInRmq(userid, password);
            })
        .compose(
            rmqResetFut -> {
              return webClient.resetPwdInDb(userid, Util.getSha(password));
            })
        .onSuccess(
            successHandler -> {
              response.put("type", ResponseUrn.SUCCESS_URN.getUrn());
              response.put(TITLE, "successful");
              response.put(DETAIL, "Successfully changed the password");
              JsonArray result =
                  new JsonArray()
                      .add(new JsonObject().put("username", userid).put("apiKey", password));
              response.put("result", result);
              promise.complete(response);
            })
        .onFailure(
            failurehandler -> {
              JsonObject failureResponse = new JsonObject();
              failureResponse
                  .put("type", 401)
                  .put("title", "not authorized")
                  .put("detail", "not authorized");
              promise.fail(failureResponse.toString());
            });

    return promise.future();
  }

  *//** This method will only publish messages to internal-communication exchanges. *//*
  @Override
  public Future<JsonObject> publishMessage(
      JsonObject request, String toExchange, String routingKey) {
    Promise<JsonObject> promise = Promise.promise();

    Future<Void> rabbitMqClientStartFuture;

    Buffer buffer = Buffer.buffer(request.toString());

    RabbitMQClient client = webClient.getRabbitmqClient();
    if (!client.isConnected()) {
      rabbitMqClientStartFuture = client.start();
    } else {
      rabbitMqClientStartFuture = Future.succeededFuture();
    }

    rabbitMqClientStartFuture
        .compose(
            rabbitstartupFuture -> {
              return client.basicPublish(toExchange, routingKey, buffer);
            })
        .onSuccess(
            successHandler -> {
              JsonObject json = new JsonObject();
              json.put("type", ResponseUrn.SUCCESS_URN.getUrn());
              promise.complete(json);
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(failureHandler);
              Response response =
                  new Response.Builder()
                      .withUrn(ResponseUrn.QUEUE_ERROR_URN.getUrn())
                      .withStatus(HttpStatus.SC_BAD_REQUEST)
                      .withDetail(failureHandler.getLocalizedMessage())
                      .build();
              promise.fail(response.toJson().toString());
            });
    return promise.future();
  }

  private String getVhost(String vhost) {
    String vhostKey = Vhosts.valueOf(vhost).value;
    return config.getString(vhostKey);
  }
}*/
