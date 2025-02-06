package iudx.resource.server.databrokernew.service;

import static iudx.resource.server.apiserver.util.Constants.ID;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_GROUP;
import static iudx.resource.server.cache.cachelmpl.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.databroker.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.Vhosts;
import iudx.resource.server.databroker.RabbitClient;
import iudx.resource.server.databroker.util.Util;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Data Broker Service Implementation.
 *
 * <h1>Data Broker Service Implementation</h1>
 *
 * <p>The Data Broker Service implementation in the DX Resource Server implements the definitions
 * of the {@link DataBrokerService}.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DataBrokerServiceImpl implements DataBrokerService {

  private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceImpl.class);
  static SubscriptionService subscriptionService;
  JsonObject finalResponse =
      Util.getResponseJson(BAD_REQUEST_CODE, BAD_REQUEST_DATA, BAD_REQUEST_DATA);
  CacheService cacheService;
  /*RabbitMQOptions iudxRabbitMQOptions;*/
  private JsonObject config;
  private RabbitClient webClient;
  private RabbitMQClient iudxRabbitMqClient;

  public DataBrokerServiceImpl(
      RabbitClient webClient,
      PostgresClient pgClient,
      JsonObject config,
      CacheService cacheService,
      /*RabbitMQOptions iudxConfig,
      Vertx vertx,*/
      RabbitMQClient iudxRabbitMqClient) {
    this.webClient = webClient;
    this.config = config;
    /*this.iudxRabbitMQOptions = iudxConfig;
    this.iudxRabbitMqClient = RabbitMQClient.create(vertx,iudxConfig);*/

    this.iudxRabbitMqClient = iudxRabbitMqClient;
    this.subscriptionService =
        new SubscriptionService(this.webClient, pgClient, config, cacheService);
    this.cacheService = cacheService;
  }

  /**
   * This method creates user, declares exchange and bind with predefined queues.
   *
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
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

  /**
   * It retrieves exchange is exist
   *
   * @param request which is of type JsonObject
   * @return response which is a Future object of promise of Json type
   */
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

  /*
   * overridden method
   */

  /**
   * The deleteAdaptor implements deletion feature for an adaptor(exchange).
   *
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
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

  /**
   * The listAdaptor implements the list of bindings for an exchange (source). This method has
   * similar functionality as listExchangeSubscribers(JsonObject) method
   *
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
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

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> registerStreamingSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.registerStreamingSubscription(request);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error(
                  "registerStreamingSubscription - resultHandler failed : "
                      + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    } else {
      promise.fail(finalResponse.toString());
    }
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> updateStreamingSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.updateStreamingSubscription(request);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error(
                  "updateStreamingSubscription - resultHandler failed : " + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    } else {
      promise.fail(finalResponse.toString());
    }

    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> appendStreamingSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.appendStreamingSubscription(request);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error(
                  "appendStreamingSubscription - resultHandler failed : " + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    } else {
      promise.fail(finalResponse.toString());
    }
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> deleteStreamingSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.deleteStreamingSubscription(request);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error(
                  "deleteStreamingSubscription - resultHandler failed : "
                      + resultHandler.cause().getMessage());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    } else {
      promise.fail(finalResponse.toString());
    }
    return promise.future();
  }

  /** {@inheritDoc} */
  @Override
  public Future<JsonObject> listStreamingSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.listStreamingSubscriptions(request);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {
              promise.complete(resultHandler.result());
            }
            if (resultHandler.failed()) {
              LOGGER.error(
                  "listStreamingSubscription - resultHandler failed : " + resultHandler.cause());
              promise.fail(resultHandler.cause().getMessage());
            }
          });
    } else {
      promise.fail(finalResponse.toString());
    }
    return promise.future();
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** This method will only publish messages to internal-communication exchanges. */
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
}
