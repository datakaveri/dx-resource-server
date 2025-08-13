package iudx.resource.server.databroker;

import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.databroker.util.Util.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.Vhosts;
import iudx.resource.server.databroker.util.PermissionOpType;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RabbitClient {

  private static final Logger LOGGER = LogManager.getLogger(RabbitClient.class);

  private final RabbitMQClient client;
  private final RabbitWebClient webClient;
  private final PostgresClient pgSqlClient;
  private final String amqpUrl;
  private final int amqpPort;
  private final String vhost;

  public RabbitClient(
      Vertx vertx,
      RabbitMQOptions rabbitConfigs,
      RabbitWebClient webClient,
      PostgresClient pgSqlClient,
      JsonObject configs) {
    this.amqpUrl = configs.getString("brokerAmqpIp");
    this.amqpPort = configs.getInteger("brokerAmqpPort");
    this.vhost = configs.getString("dataBrokerVhost");

    String internalVhost = configs.getString(Vhosts.IUDX_INTERNAL.name());
    rabbitConfigs.setVirtualHost(internalVhost);
    this.client = getRabbitmqClient(vertx, rabbitConfigs);
    this.webClient = webClient;
    this.pgSqlClient = pgSqlClient;
    client.start(
        clientStartupHandler -> {
          if (clientStartupHandler.succeeded()) {
            LOGGER.info("Info : rabbit MQ client started");
          } else if (clientStartupHandler.failed()) {
            LOGGER.fatal("Fail : rabbit MQ client startup failed.");
          }
        });
  }

  private RabbitMQClient getRabbitmqClient(Vertx vertx, RabbitMQOptions rabbitConfigs) {
    return RabbitMQClient.create(vertx, rabbitConfigs);
  }

  public RabbitMQClient getRabbitmqClient() {
    return this.client;
  }

  /**
   * The createExchange implements the create exchange.
   *
   * @param request which is a Json object @Param vHost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  public Future<JsonObject> createExchange(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#createExchage() started");
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      JsonObject obj = new JsonObject();
      obj.put(TYPE, EXCHANGE_TYPE);
      obj.put(AUTO_DELETE, false);
      obj.put(DURABLE, true);
      String exchangeName = request.getString("exchangeName");
      String url = "/api/exchanges/" + vhost + "/" + encodeValue(exchangeName);
      webClient
          .requestAsync(REQUEST_PUT, url, obj)
          .onComplete(
              requestHandler -> {
                if (requestHandler.succeeded()) {
                  JsonObject responseJson = new JsonObject();
                  HttpResponse<Buffer> response = requestHandler.result();
                  int statusCode = response.statusCode();
                  if (statusCode == HttpStatus.SC_CREATED) {
                    responseJson.put(EXCHANGE, exchangeName);
                  } else if (statusCode == HttpStatus.SC_NO_CONTENT) {
                    responseJson =
                        getResponseJson(HttpStatus.SC_CONFLICT, FAILURE, EXCHANGE_EXISTS);
                  } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                    responseJson =
                        getResponseJson(
                            statusCode, FAILURE, EXCHANGE_EXISTS_WITH_DIFFERENT_PROPERTIES);
                  }
                  LOGGER.debug("Success : " + responseJson);
                  promise.complete(responseJson);
                } else {
                  JsonObject errorJson =
                      getResponseJson(
                          HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR, EXCHANGE_CREATE_ERROR);
                  LOGGER.error("Fail : " + requestHandler.cause());
                  promise.fail(errorJson.toString());
                }
              });
    }
    return promise.future();
  }

  Future<JsonObject> getExchangeDetails(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#getExchange() started");
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String url = "/api/exchanges/" + vhost + "/" + encodeValue(exchangeName);
      webClient
          .requestAsync(REQUEST_GET, url)
          .onComplete(
              requestHandler -> {
                if (requestHandler.succeeded()) {
                  JsonObject responseJson = new JsonObject();
                  HttpResponse<Buffer> response = requestHandler.result();
                  int statusCode = response.statusCode();
                  if (statusCode == HttpStatus.SC_OK) {
                    responseJson = new JsonObject(response.body().toString());
                    LOGGER.debug("Success : " + responseJson);
                    promise.complete(responseJson);
                  } else {
                    responseJson = getResponseJson(statusCode, FAILURE, EXCHANGE_NOT_FOUND);
                    promise.fail(responseJson.toString());
                  }
                } else {
                  JsonObject errorJson =
                      getResponseJson(
                          HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR, EXCHANGE_NOT_FOUND);
                  LOGGER.error("Error : " + requestHandler.cause());
                  promise.fail(errorJson.toString());
                }
              });
    }
    return promise.future();
  }

  Future<JsonObject> getExchange(JsonObject request, String vhost) {
    JsonObject response = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("id");
      String url;
      url = "/api/exchanges/" + vhost + "/" + encodeValue(exchangeName);
      webClient
          .requestAsync(REQUEST_GET, url)
          .onComplete(
              result -> {
                if (result.succeeded()) {
                  int status = result.result().statusCode();
                  response.put(TYPE, status);
                  if (status == HttpStatus.SC_OK) {
                    response.put(TITLE, SUCCESS);
                    response.put(DETAIL, EXCHANGE_FOUND);
                  } else if (status == HttpStatus.SC_NOT_FOUND) {
                    response.put(TITLE, FAILURE);
                    response.put(DETAIL, EXCHANGE_NOT_FOUND);
                  } else {
                    response.put("getExchange_status", status);
                    promise.fail("getExchange_status" + result.cause());
                  }
                } else {
                  response.put("getExchange_error", result.cause());
                  promise.fail("getExchange_error" + result.cause());
                }
                LOGGER.debug("getExchange method response : " + response);
                promise.tryComplete(response);
              });

    } else {
      promise.fail("exchangeName not provided");
    }
    return promise.future();
  }

  /**
   * The deleteExchange implements the delete exchange operation.
   *
   * @param request which is a Json object @Param VHost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> deleteExchange(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#deleteExchange() started");
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String url = "/api/exchanges/" + vhost + "/" + encodeValue(exchangeName);
      webClient
          .requestAsync(REQUEST_DELETE, url)
          .onComplete(
              requestHandler -> {
                if (requestHandler.succeeded()) {
                  JsonObject responseJson = new JsonObject();
                  HttpResponse<Buffer> response = requestHandler.result();
                  int statusCode = response.statusCode();
                  if (statusCode == HttpStatus.SC_NO_CONTENT) {
                    responseJson = new JsonObject();
                    responseJson.put(EXCHANGE, exchangeName);
                  } else {
                    responseJson = getResponseJson(statusCode, FAILURE, EXCHANGE_NOT_FOUND);
                    LOGGER.debug("Success : " + responseJson);
                  }
                  promise.complete(responseJson);
                } else {
                  JsonObject errorJson =
                      getResponseJson(
                          HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR, EXCHANGE_DELETE_ERROR);
                  LOGGER.error("Error : " + requestHandler.cause());
                  promise.fail(errorJson.toString());
                }
              });
    }
    return promise.future();
  }

  /**
   * The listExchangeSubscribers implements the list of bindings for an exchange (source).
   *
   * @param request which is a Json object
   * @param vhost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> listExchangeSubscribers(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#listExchangeSubscribers() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString(ID);
      String url = "/api/exchanges/" + vhost + "/" + encodeValue(exchangeName) + "/bindings/source";
      webClient
          .requestAsync(REQUEST_GET, url)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    if (status == HttpStatus.SC_OK) {
                      Buffer body = response.body();
                      if (body != null) {
                        JsonArray jsonBody = new JsonArray(body.toString());
                        Map res =
                            jsonBody.stream()
                                .map(JsonObject.class::cast)
                                .collect(
                                    Collectors.toMap(
                                        json -> json.getString("destination"),
                                        json -> new JsonArray().add(json.getString("routing_key")),
                                        bindingMergeOperator));
                        LOGGER.debug("Info : exchange subscribers : " + jsonBody);
                        finalResponse.clear().mergeIn(new JsonObject(res));
                        LOGGER.debug("Info : final Response : " + finalResponse);
                        if (finalResponse.isEmpty()) {
                          finalResponse
                              .clear()
                              .mergeIn(
                                  getResponseJson(
                                      HttpStatus.SC_NOT_FOUND, FAILURE, EXCHANGE_NOT_FOUND),
                                  true);
                        }
                      }
                    } else if (status == HttpStatus.SC_NOT_FOUND) {
                      finalResponse.mergeIn(
                          getResponseJson(HttpStatus.SC_NOT_FOUND, FAILURE, EXCHANGE_NOT_FOUND),
                          true);
                    }
                  }
                  promise.complete(finalResponse);
                  LOGGER.debug("Success :" + finalResponse);
                } else {
                  LOGGER.error("Fail : Listing of Exchange failed - ", ar.cause());
                  JsonObject error = getResponseJson(500, FAILURE, "Internal server error");
                  promise.fail(error.toString());
                }
              });
    }
    return promise.future();
  }

  /**
   * The createQueue implements the create queue operation.
   *
   * @param request which is a Json object
   * @param vhost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> createQueue(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#createQueue() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      JsonObject configProp = new JsonObject();
      JsonObject arguments = new JsonObject();
      arguments
          .put(X_MESSAGE_TTL_NAME, X_MESSAGE_TTL_VALUE)
          .put(X_MAXLENGTH_NAME, X_MAXLENGTH_VALUE)
          .put(X_QUEUE_MODE_NAME, X_QUEUE_MODE_VALUE);
      configProp.put(X_QUEUE_TYPE, true);
      configProp.put(X_QUEUE_ARGUMENTS, arguments);
      String queueName = request.getString("queueName");
      String url = "/api/queues/" + vhost + "/" + encodeValue(queueName); // "durable":true
      webClient
          .requestAsync(REQUEST_PUT, url, configProp)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    if (status == HttpStatus.SC_CREATED) {
                      finalResponse.put(QUEUE, queueName);
                    } else if (status == HttpStatus.SC_NO_CONTENT) {
                      finalResponse.mergeIn(
                          getResponseJson(HttpStatus.SC_CONFLICT, FAILURE, QUEUE_ALREADY_EXISTS),
                          true);
                    } else if (status == HttpStatus.SC_BAD_REQUEST) {
                      finalResponse.mergeIn(
                          getResponseJson(
                              status, FAILURE, QUEUE_ALREADY_EXISTS_WITH_DIFFERENT_PROPERTIES),
                          true);
                    }
                  }
                  promise.complete(finalResponse);
                } else {
                  LOGGER.error("Fail : Creation of Queue failed - ", ar.cause());
                  finalResponse.mergeIn(getResponseJson(500, FAILURE, QUEUE_CREATE_ERROR));
                  promise.fail(finalResponse.toString());
                }
              });
    }
    return promise.future();
  }

  /**
   * The deleteQueue implements the delete queue operation.
   *
   * @param request which is a Json object
   * @param vhost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> deleteQueue(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#deleteQueue() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String queueName = request.getString("queueName");
      LOGGER.debug("Info : queuName" + queueName);
      String url = "/api/queues/" + vhost + "/" + encodeValue(queueName);
      webClient
          .requestAsync(REQUEST_DELETE, url)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    if (status == HttpStatus.SC_NO_CONTENT) {
                      finalResponse.put(QUEUE, queueName);
                    } else if (status == HttpStatus.SC_NOT_FOUND) {
                      finalResponse.mergeIn(
                          getResponseJson(status, FAILURE, QUEUE_DOES_NOT_EXISTS));
                    }
                  }
                  LOGGER.info(finalResponse);
                  promise.complete(finalResponse);
                } else {
                  LOGGER.error("Fail : deletion of queue failed - ", ar.cause());
                  finalResponse.mergeIn(getResponseJson(500, FAILURE, QUEUE_DELETE_ERROR));
                  promise.fail(finalResponse.toString());
                }
              });
    }
    return promise.future();
  }

  /**
   * The bindQueue implements the bind queue to exchange by routing key.
   *
   * @param request which is a Json object
   * @param vhost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> bindQueue(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#bindQueue() started");
    JsonObject finalResponse = new JsonObject();
    JsonObject requestBody = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String queueName = request.getString("queueName");
      JsonArray entities = request.getJsonArray("entities");
      int arrayPos = entities.size() - 1;
      String url =
          "/api/bindings/"
              + vhost
              + "/e/"
              + encodeValue(exchangeName)
              + "/q/"
              + encodeValue(queueName);
      for (Object rkey : entities) {
        requestBody.put("routing_key", rkey.toString());
        webClient
            .requestAsync(REQUEST_POST, url, requestBody)
            .onComplete(
                ar -> {
                  if (ar.succeeded()) {
                    HttpResponse<Buffer> response = ar.result();
                    if (response != null && !response.equals(" ")) {
                      int status = response.statusCode();
                      LOGGER.info("Info : Binding " + rkey + "Success. Status is " + status);
                      if (status == HttpStatus.SC_CREATED) {
                        finalResponse.put(EXCHANGE, exchangeName);
                        finalResponse.put(QUEUE, queueName);
                        finalResponse.put(ENTITIES, entities);
                      } else if (status == HttpStatus.SC_NOT_FOUND) {
                        finalResponse.mergeIn(
                            getResponseJson(status, FAILURE, QUEUE_EXCHANGE_NOT_FOUND));
                      }
                    }
                    if (rkey == entities.getValue(arrayPos)) {
                      LOGGER.debug("Success : " + finalResponse);
                      promise.complete(finalResponse);
                    }
                  } else {
                    LOGGER.error("Fail : Binding of Queue failed - ", ar.cause());
                    finalResponse.mergeIn(getResponseJson(500, FAILURE, QUEUE_BIND_ERROR));
                    promise.fail(finalResponse.toString());
                  }
                });
      }
    }
    return promise.future();
  }

  Future<Void> bindQueue(String queue, String adaptorId, String topics, String vhost) {
    LOGGER.trace("Info : RabbitClient#bindQueue() started");
    LOGGER.debug("Info : data : " + queue + " adaptorID : " + adaptorId + " topics : " + topics);
    Promise<Void> promise = Promise.promise();
    String url =
        "/api/bindings/" + vhost + "/e/" + encodeValue(adaptorId) + "/q/" + encodeValue(queue);
    JsonObject bindRequest = new JsonObject();
    bindRequest.put("routing_key", topics);

    webClient
        .requestAsync(REQUEST_POST, url, bindRequest)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                promise.complete();
              } else {
                LOGGER.error("Error : Queue" + queue + " binding error : ", handler.cause());
                promise.fail(handler.cause());
              }
            });
    return promise.future();
  }

  /**
   * The unbindQueue implements the unbind queue to exchange by routing key.
   *
   * @param request which is a Json object
   * @param vhost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> unbindQueue(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#unbindQueue() started");
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String queueName = request.getString("queueName");
      JsonArray entities = request.getJsonArray("entities");
      int arrayPos = entities.size() - 1;
      for (Object rkey : entities) {
        String url =
            "/api/bindings/"
                + vhost
                + "/e/"
                + encodeValue(exchangeName)
                + "/q/"
                + encodeValue(queueName)
                + "/"
                + encodeValue((String) rkey);
        webClient
            .requestAsync(REQUEST_DELETE, url)
            .onComplete(
                ar -> {
                  if (ar.succeeded()) {
                    HttpResponse<Buffer> response = ar.result();
                    if (response != null && !response.equals(" ")) {
                      int status = response.statusCode();
                      if (status == HttpStatus.SC_NO_CONTENT) {
                        finalResponse.put(EXCHANGE, exchangeName);
                        finalResponse.put(QUEUE, queueName);
                        finalResponse.put(ENTITIES, entities);
                      } else if (status == HttpStatus.SC_NOT_FOUND) {
                        finalResponse.mergeIn(getResponseJson(status, FAILURE, ALL_NOT_FOUND));
                      }
                    }
                    if (rkey == entities.getValue(arrayPos)) {
                      LOGGER.debug("Success : " + finalResponse);
                      promise.complete(finalResponse);
                    }
                  } else {
                    LOGGER.error("Fail : Unbinding of Queue failed", ar.cause());
                    finalResponse.mergeIn(getResponseJson(500, FAILURE, QUEUE_BIND_ERROR));
                    promise.fail(finalResponse.toString());
                  }
                });
      }
    }
    return promise.future();
  }

  /**
   * The createvHost implements the create virtual host operation.
   *
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> createvHost(JsonObject request) {
    LOGGER.trace("Info : RabbitClient#createvHost() started");
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String vhost = request.getString("vHost");
      String url = "/api/vhosts/" + encodeValue(vhost);
      webClient
          .requestAsync(REQUEST_PUT, url)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    if (status == HttpStatus.SC_CREATED) {
                      finalResponse.put(VHOST, vhost);
                    } else if (status == HttpStatus.SC_NO_CONTENT) {
                      finalResponse.mergeIn(
                          getResponseJson(HttpStatus.SC_CONFLICT, FAILURE, VHOST_ALREADY_EXISTS));
                    }
                  }
                  promise.complete(finalResponse);
                  LOGGER.info("Successully created vhost : " + VHOST);
                } else {
                  LOGGER.error(" Fail : Creation of vHost failed", ar.cause());
                  finalResponse.mergeIn(getResponseJson(500, FAILURE, VHOST_CREATE_ERROR));
                  promise.fail(finalResponse.toString());
                }
              });
    }
    return promise.future();
  }

  /**
   * The deletevHost implements the delete virtual host operation.
   *
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> deletevHost(JsonObject request) {
    LOGGER.trace("Info : RabbitClient#deletevHost() started");
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String vhost = request.getString("vHost");
      String url = "/api/vhosts/" + encodeValue(vhost);
      webClient
          .requestAsync(REQUEST_DELETE, url)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    LOGGER.debug("Info : statusCode" + status);
                    if (status == HttpStatus.SC_NO_CONTENT) {
                      finalResponse.put(VHOST, vhost);
                    } else if (status == HttpStatus.SC_NOT_FOUND) {
                      finalResponse.mergeIn(getResponseJson(status, FAILURE, VHOST_NOT_FOUND));
                    }
                  }
                  promise.complete(finalResponse);
                  LOGGER.info("Successfully deleted vhost : " + VHOST);
                } else {
                  LOGGER.error("Fail : Deletion of vHost failed -", ar.cause());
                  finalResponse.mergeIn(getResponseJson(500, FAILURE, VHOST_DELETE_ERROR));
                  promise.fail(finalResponse.toString());
                }
              });
    }

    return promise.future();
  }

  /**
   * The listvHost implements the list of virtual hosts .
   *
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> listvHost(JsonObject request) {
    LOGGER.trace("Info : RabbitClient#listvHost() started");
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null) {
      JsonArray vhostList = new JsonArray();
      String url = "/api/vhosts";
      webClient
          .requestAsync(REQUEST_GET, url)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    LOGGER.debug("Info : statusCode" + status);
                    if (status == HttpStatus.SC_OK) {
                      Buffer body = response.body();
                      if (body != null) {
                        JsonArray jsonBody = new JsonArray(body.toString());
                        jsonBody.forEach(
                            current -> {
                              JsonObject currentJson = new JsonObject(current.toString());
                              String vhostName = currentJson.getString("name");
                              vhostList.add(vhostName);
                            });
                        if (vhostList != null && !vhostList.isEmpty()) {
                          finalResponse.put(VHOST, vhostList);
                        }
                      }
                    } else if (status == HttpStatus.SC_NOT_FOUND) {
                      finalResponse.mergeIn(getResponseJson(status, FAILURE, VHOST_NOT_FOUND));
                    }
                  }
                  LOGGER.debug("Success : " + finalResponse);
                  promise.complete(finalResponse);
                } else {
                  LOGGER.error("Fail : Listing of vHost failed - ", ar.cause());
                  finalResponse.mergeIn(getResponseJson(500, FAILURE, VHOST_LIST_ERROR));
                  promise.fail(finalResponse.toString());
                }
              });
    }
    return promise.future();
  }

  /**
   * The listQueueSubscribers implements the list of bindings for a queue.
   *
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> listQueueSubscribers(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#listQueueSubscribers() started");
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String queueName = request.getString("queueName");
      JsonArray oroutingKeys = new JsonArray();
      String url = "/api/queues/" + vhost + "/" + encodeValue(queueName) + "/bindings";
      webClient
          .requestAsync(REQUEST_GET, url)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    LOGGER.debug("Info : statusCode " + status);
                    if (status == HttpStatus.SC_OK) {
                      Buffer body = response.body();
                      if (body != null) {
                        JsonArray jsonBody = new JsonArray(body.toString());
                        jsonBody.forEach(
                            current -> {
                              JsonObject currentJson = new JsonObject(current.toString());
                              String rkeys = currentJson.getString("routing_key");
                              if (rkeys != null && !rkeys.equalsIgnoreCase(queueName)) {
                                oroutingKeys.add(rkeys);
                              }
                            });
                        finalResponse.put(ENTITIES, oroutingKeys);
                      }
                    } else if (status == HttpStatus.SC_NOT_FOUND) {
                      finalResponse
                          .clear()
                          .mergeIn(getResponseJson(status, FAILURE, QUEUE_DOES_NOT_EXISTS));
                    }
                  }
                  LOGGER.debug("Info : " + finalResponse);
                  promise.complete(finalResponse);
                } else {
                  LOGGER.error("Error : Listing of Queue failed - " + ar.cause());
                  finalResponse.mergeIn(getResponseJson(500, FAILURE, QUEUE_LIST_ERROR));
                  promise.fail(finalResponse.toString());
                }
              });
    }
    return promise.future();
  }

  public Future<JsonObject> registerAdapter(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#registerAdaptor() started");
    LOGGER.debug("Request :" + request + " vhost : " + vhost);
    Promise<JsonObject> promise = Promise.promise();
    String id = request.getJsonArray("entities").getString(0); // getting first and only id
    AdaptorResultContainer requestParams = new AdaptorResultContainer();
    requestParams.vhost = vhost;
    requestParams.id = request.getString("resourceGroup");
    requestParams.resourceServer = request.getString("resourceServer");
    requestParams.userid = request.getString(USER_ID);

    requestParams.adaptorId = id;
    requestParams.type = request.getString("types");
    if (isValidId.test(requestParams.adaptorId)) {
      if (requestParams.adaptorId != null
          && !requestParams.adaptorId.isEmpty()
          && !requestParams.adaptorId.isBlank()) {
        Future<JsonObject> userCreationFuture = createUserIfNotExist(requestParams.userid, vhost);
        userCreationFuture
            .compose(
                userCreationResult -> {
                  requestParams.apiKey = userCreationResult.getString("apiKey");
                  JsonObject json = new JsonObject();
                  json.put(EXCHANGE_NAME, requestParams.adaptorId);
                  LOGGER.debug("Success : User created/exist.");
                  return createExchange(json, vhost);
                })
            .compose(
                createExchangeResult -> {
                  if (createExchangeResult.containsKey("detail")) {
                    LOGGER.error("Error : Exchange creation failed. ");
                    return Future.failedFuture(createExchangeResult.toString());
                  }
                  LOGGER.debug("Success : Exchange created successfully.");
                  requestParams.isExchnageCreated = true;
                  return updateUserPermissions(
                      requestParams.vhost,
                      requestParams.userid,
                      PermissionOpType.ADD_WRITE,
                      requestParams.adaptorId);
                })
            .compose(
                userPermissionsResult -> {
                  LOGGER.debug("Success : user permissions set.");
                  return queueBinding(requestParams, vhost);
                })
            .onSuccess(
                success -> {
                  LOGGER.debug("Success : queue bindings done.");
                  JsonObject response =
                      new JsonObject()
                          .put(USER_NAME, requestParams.userid)
                          .put(APIKEY, requestParams.apiKey)
                          .put(ID, requestParams.adaptorId)
                          .put(URL, this.amqpUrl)
                          .put(PORT, this.amqpPort)
                          .put(VHOST, requestParams.vhost);
                  LOGGER.debug("Success : Adapter created successfully.");
                  promise.complete(response);
                })
            .onFailure(
                failure -> {
                  LOGGER.info("Error : ", failure);
                  // Compensating call, delete adaptor if created;
                  if (requestParams.isExchnageCreated) {
                    JsonObject deleteJson =
                        new JsonObject().put("exchangeName", requestParams.adaptorId);
                    Future.future(fu -> deleteExchange(deleteJson, vhost));
                  }
                  promise.fail(failure);
                });
      } else {
        promise.fail(
            getResponseJson(BAD_REQUEST_CODE, BAD_REQUEST_DATA, "Invalid/Missing Parameters")
                .toString());
      }
    } else {
      promise.fail(
          getResponseJson(BAD_REQUEST_CODE, BAD_REQUEST_DATA, "Invalid/Missing Parameters")
              .toString());
    }
    return promise.future();
  }

  Future<JsonObject> deleteAdapter(JsonObject json, String vhost) {
    LOGGER.trace("Info : RabbitClient#deleteAdapter() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    Future<JsonObject> result = getExchange(json, vhost);
    result.onComplete(
        resultHandler -> {
          if (resultHandler.succeeded()) {
            int status = resultHandler.result().getInteger("type");
            if (status == 200) {
              String exchangeId = json.getString("id");
              String userId = json.getString("userid");
              String url = "/api/exchanges/" + vhost + "/" + encodeValue(exchangeId);
              webClient
                  .requestAsync(REQUEST_DELETE, url)
                  .onComplete(
                      rh -> {
                        if (rh.succeeded()) {
                          LOGGER.debug("Info : " + exchangeId + " adaptor deleted successfully");
                          finalResponse.mergeIn(getResponseJson(200, "success", "adaptor deleted"));
                          Future.future(
                              fu ->
                                  updateUserPermissions(
                                      vhost, userId, PermissionOpType.DELETE_WRITE, exchangeId));
                        } else if (rh.failed()) {
                          finalResponse
                              .clear()
                              .mergeIn(
                                  getResponseJson(
                                      HttpStatus.SC_INTERNAL_SERVER_ERROR,
                                      "Adaptor deleted",
                                      rh.cause().toString()));
                          LOGGER.error("Error : Adaptor deletion failed cause - " + rh.cause());
                          promise.fail(finalResponse.toString());
                        } else {
                          LOGGER.error("Error : Something wrong in deleting adaptor" + rh.cause());
                          finalResponse.mergeIn(
                              getResponseJson(400, "bad request", "nothing to delete"));
                          promise.fail(finalResponse.toString());
                        }
                        promise.tryComplete(finalResponse);
                      });

            } else if (status == 404) { // exchange not found
              finalResponse
                  .clear()
                  .mergeIn(
                      getResponseJson(
                          status, "not found", resultHandler.result().getString("detail")));
              LOGGER.error("Error : Exchange not found cause ");
              promise.fail(finalResponse.toString());
            } else { // some other issue
              LOGGER.error("Error : Bad request");
              finalResponse.mergeIn(getResponseJson(400, "bad request", "nothing to delete"));
              promise.fail(finalResponse.toString());
            }
          }
          if (resultHandler.failed()) {
            LOGGER.error("Error : deleteAdaptor - resultHandler failed : " + resultHandler.cause());
            finalResponse.mergeIn(
                getResponseJson(INTERNAL_ERROR_CODE, "bad request", "nothing to delete"));
            promise.fail(finalResponse.toString());
          }
        });
    return promise.future();
  }

  /**
   * The createUserIfNotExist implements the create user if does not exist.
   *
   * @param userid which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> createUserIfNotExist(String userid, String vhost) {
    LOGGER.trace("Info : RabbitClient#createUserIfNotPresent() started");
    Promise<JsonObject> promise = Promise.promise();

    String password = randomPassword.get();
    String url = "/api/users/" + userid;
    /* Check if user exists */
    JsonObject response = new JsonObject();
    webClient
        .requestAsync(REQUEST_GET, url)
        .onComplete(
            reply -> {
              if (reply.succeeded()) {
                /* Check if user not found */
                if (reply.result().statusCode() == HttpStatus.SC_NOT_FOUND) {
                  LOGGER.debug("Success : User not found. creating user");
                  /* Create new user */
                  Future<JsonObject> userCreated = createUser(userid, password, vhost, url);
                  userCreated.onComplete(
                      handler -> {
                        if (handler.succeeded()) {
                          /* Handle the response */
                          JsonObject result = handler.result();
                          response.put(USER_ID, userid);
                          response.put(APIKEY, password);
                          response.put(TYPE, result.getInteger("type"));
                          response.put(TITLE, result.getString("title"));
                          response.put(DETAILS, result.getString("detail"));
                          response.put(VHOST_PERMISSIONS, vhost);
                          promise.complete(response);
                        } else {
                          LOGGER.error(
                              "Error : Error in user creation. Cause : " + handler.cause());
                          response.mergeIn(
                              getResponseJson(INTERNAL_ERROR_CODE, ERROR, USER_CREATION_ERROR));
                          promise.fail(response.toString());
                        }
                      });

                } else if (reply.result().statusCode() == HttpStatus.SC_OK) {
                  LOGGER.debug("DATABASE_READ_SUCCESS");
                  response.put(USER_ID, userid);
                  response.put(APIKEY, API_KEY_MESSAGE);
                  response.mergeIn(
                      getResponseJson(SUCCESS_CODE, DATABASE_READ_SUCCESS, DATABASE_READ_SUCCESS));
                  response.put(VHOST_PERMISSIONS, vhost);
                  promise.complete(response);
                }

              } else {
                /* Handle API error */
                LOGGER.error(
                    "Error : Something went wrong while finding user using mgmt API: "
                        + reply.cause());
                promise.fail(reply.cause().toString());
              }
            });
    return promise.future();
  }

  /**
   * CreateUserIfNotPresent's helper method which creates user if not present.
   *
   * @param userid which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> createUser(String userid, String password, String vhost, String url) {
    LOGGER.trace("Info : RabbitClient#createUser() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    JsonObject arg = new JsonObject();
    arg.put(PASSWORD, password);
    arg.put(TAGS, NONE);

    webClient
        .requestAsync(REQUEST_PUT, url, arg)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                /* Check if user is created */
                if (ar.result().statusCode() == HttpStatus.SC_CREATED) {
                  LOGGER.debug("createUserRequest success");
                  response.put(USER_ID, userid);
                  response.put(PASSWORD, password);
                  LOGGER.debug("Info : user created successfully");
                  // set permissions to vhost for newly created user
                  Future<JsonObject> vhostPermission = setVhostPermissions(userid, vhost);
                  vhostPermission.onComplete(
                      handler -> {
                        if (handler.succeeded()) {
                          response.mergeIn(
                              getResponseJson(
                                  SUCCESS_CODE,
                                  VHOST_PERMISSIONS,
                                  handler.result().getString(DETAIL)));
                          // Call the DB method to store username and password
                          Future<JsonObject> createUserinDb =
                              createUserInDb(userid, getSha(password));
                          createUserinDb.onComplete(
                              createUserinDbHandler -> {
                                if (createUserinDbHandler.succeeded()) {
                                  promise.complete(response);
                                } else {
                                  /* Handle error */
                                  LOGGER.error(
                                      "Error : error in saving credentials. Cause : ",
                                      createUserinDbHandler.cause());
                                  promise.fail("Error : error in saving credentials");
                                }
                              });
                        } else {
                          /* Handle error */
                          LOGGER.error(
                              "Error : error in setting vhostPermissions. Cause : ",
                              handler.cause());
                          promise.fail("Error : error in setting vhostPermissions");
                        }
                      });

                } else {
                  /* Handle error */
                  LOGGER.error("Error : createUser method - Some network error. cause", ar.cause());
                  response.put(FAILURE, NETWORK_ISSUE);
                  promise.fail(response.toString());
                }
              } else {
                /* Handle error */
                LOGGER.info(
                    "Error : Something went wrong while creating user using mgmt API :",
                    ar.cause());
                response.put(FAILURE, CHECK_CREDENTIALS);
                promise.fail(response.toString());
              }
            });
    return promise.future();
  }

  Future<JsonObject> createUserInDb(String shaUsername, String password) {
    LOGGER.trace("Info : RabbitClient#createUserInDb() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();

    String query = INSERT_DATABROKER_USER.replace("$1", shaUsername).replace("$2", password);

    // Check in DB, get username and password
    pgSqlClient
        .executeAsync(query)
        .onComplete(
            db -> {
              LOGGER.debug("Info : RabbitClient#createUserInDb()executeAsync completed");
              if (db.succeeded()) {
                LOGGER.debug("Info : RabbitClient#createUserInDb()executeAsync success");
                response.put("status", "success");
                promise.complete(response);
              } else {
                LOGGER.fatal("Fail : RabbitClient#createUserInDb()executeAsync failed");
                promise.fail("Error : Write to database failed");
              }
            });
    return promise.future();
  }

  Future<JsonObject> resetPasswordInRmq(String userid, String password) {
    LOGGER.trace("Info : RabbitClient#resetPassword() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    JsonObject arg = new JsonObject();
    arg.put(PASSWORD, password);
    arg.put(TAGS, NONE);
    String url = "/api/users/" + userid;
    webClient
        .requestAsync(REQUEST_PUT, url, arg)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                if (ar.result().statusCode() == HttpStatus.SC_NO_CONTENT) {
                  response.put(userid, userid);
                  response.put(PASSWORD, password);
                  LOGGER.debug("user password changed");
                  promise.complete(response);
                } else {
                  LOGGER.error("Error :reset pwd method failed", ar.cause());
                  response.put(FAILURE, NETWORK_ISSUE);
                  promise.fail(response.toString());
                }
              } else {
                LOGGER.error("User creation failed using mgmt API :", ar.cause());
                response.put(FAILURE, CHECK_CREDENTIALS);
                promise.fail(response.toString());
              }
            });
    return promise.future();
  }

  Future<JsonObject> resetPwdInDb(String userid, String password) {
    LOGGER.trace("Info : RabbitClient#resetpwdInDb() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();

    String query = RESET_PWD.replace("$1", password).replace("$2", userid);

    pgSqlClient
        .executeAsync(query)
        .onComplete(
            db -> {
              LOGGER.debug("Info : RabbitClient#resetpwdInDb()executeAsync completed");
              if (db.succeeded()) {
                LOGGER.debug("Info : RabbitClient#resetpwdInDb()executeAsync success");
                response.put("status", "success");
                promise.complete(response);
              } else {
                LOGGER.fatal("Fail : RabbitClient#resetpwdInDb()executeAsync failed");
                promise.fail("Error : Write to database failed");
              }
            });
    return promise.future();
  }

  Future<JsonObject> getUserInDb(String userid) {
    LOGGER.trace("Info : RabbitClient#getUserInDb() started");

    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    String query = SELECT_DATABROKER_USER.replace("$1", userid);
    // Check in DB, get username and password
    pgSqlClient
        .executeAsync(query)
        .onComplete(
            db -> {
              LOGGER.debug("Info : RabbitClient#getUserInDb()executeAsync completed");
              if (db.succeeded()) {
                LOGGER.debug("Info : RabbitClient#getUserInDb()executeAsync success");
                String apiKey = null;
                // Get the apiKey
                RowSet<Row> result = db.result();
                if (db.result().size() > 0) {
                  for (Row row : result) {
                    apiKey = row.getString(1);
                  }
                }
                response.put(APIKEY, apiKey);
                promise.complete(response);
              } else {
                LOGGER.fatal("Fail : RabbitClient#getUserInDb()executeAsync failed");
                promise.fail("Error : Get ID from database failed");
              }
            });
    return promise.future();
  }

  /* changed the access modifier to default as setTopicPermissions is not being called anywhere */
  /**
   * set topic permissions.
   *
   * @param vhost which is a String
   * @param adaptorId which is a String
   * @param userId which is a String
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> setTopicPermissions(String vhost, String adaptorId, String userId) {
    LOGGER.trace("Info : RabbitClient#setTopicPermissions() started");
    JsonObject param = new JsonObject();
    // set all mandatory fields
    param.put(EXCHANGE, adaptorId);
    param.put(WRITE, ALLOW);
    param.put(READ, DENY);
    param.put(CONFIGURE, DENY);

    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    String url = "/api/permissions/" + vhost + "/" + encodeValue(userId);
    webClient
        .requestAsync(REQUEST_PUT, url, param)
        .onComplete(
            result -> {
              if (result.succeeded()) {
                /* Check if request was a success */
                if (result.result().statusCode() == HttpStatus.SC_CREATED) {
                  response.mergeIn(
                      getResponseJson(
                          SUCCESS_CODE, TOPIC_PERMISSION, TOPIC_PERMISSION_SET_SUCCESS));
                  LOGGER.debug("Success : Topic permission set");
                  promise.complete(response);
                } else if (result.result().statusCode() == HttpStatus.SC_NO_CONTENT) {
                  /* Check if request was already served */
                  response.mergeIn(
                      getResponseJson(
                          SUCCESS_CODE, TOPIC_PERMISSION, TOPIC_PERMISSION_ALREADY_SET));
                  promise.complete(response);
                } else {
                  /* Check if request has an error */
                  LOGGER.error(
                      "Error : error in setting topic permissions"
                          + result.result().statusMessage());
                  response.mergeIn(
                      getResponseJson(
                          INTERNAL_ERROR_CODE, TOPIC_PERMISSION, TOPIC_PERMISSION_SET_ERROR));
                  promise.fail(response.toString());
                }
              } else {
                /* Check if request has an error */
                LOGGER.error("Error : error in setting topic permission : " + result.cause());
                response.mergeIn(
                    getResponseJson(
                        INTERNAL_ERROR_CODE, TOPIC_PERMISSION, TOPIC_PERMISSION_SET_ERROR));
                promise.fail(response.toString());
              }
            });
    return promise.future();
  }

  /**
   * set vhost permissions for given userName.
   *
   * @param shaUsername which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> setVhostPermissions(String shaUsername, String vhost) {
    LOGGER.trace("Info : RabbitClient#setVhostPermissions() started");
    /* Construct URL to use */
    JsonObject vhostPermissions = new JsonObject();
    // all keys are mandatory. empty strings used for configure,read as not
    // permitted.
    vhostPermissions.put(CONFIGURE, DENY);
    vhostPermissions.put(WRITE, NONE);
    vhostPermissions.put(READ, NONE);
    Promise<JsonObject> promise = Promise.promise();
    /* Construct a response object */
    JsonObject vhostPermissionResponse = new JsonObject();
    String url = "/api/permissions/" + vhost + "/" + encodeValue(shaUsername);
    webClient
        .requestAsync(REQUEST_PUT, url, vhostPermissions)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                /* Check if permission was set */
                if (handler.result().statusCode() == HttpStatus.SC_CREATED) {
                  LOGGER.debug(
                      "Success :write permission set for user [ "
                          + shaUsername
                          + " ] in vHost [ "
                          + vhost
                          + "]");
                  vhostPermissionResponse.mergeIn(
                      getResponseJson(SUCCESS_CODE, VHOST_PERMISSIONS, VHOST_PERMISSIONS_WRITE));
                  promise.complete(vhostPermissionResponse);
                } else {
                  LOGGER.error(
                      "Error : error in write permission set for user [ "
                          + shaUsername
                          + " ] in vHost [ "
                          + vhost
                          + " ]");
                  vhostPermissionResponse.mergeIn(
                      getResponseJson(
                          INTERNAL_ERROR_CODE, VHOST_PERMISSIONS, VHOST_PERMISSION_SET_ERROR));
                  promise.fail(vhostPermissions.toString());
                }
              } else {
                /* Check if request has an error */
                LOGGER.error(
                    "Error : error in write permission set for user [ "
                        + shaUsername
                        + " ] in vHost [ "
                        + vhost
                        + " ]");
                vhostPermissionResponse.mergeIn(
                    getResponseJson(
                        INTERNAL_ERROR_CODE, VHOST_PERMISSIONS, VHOST_PERMISSION_SET_ERROR));
                promise.fail(vhostPermissions.toString());
              }
            });
    return promise.future();
  }

  /**
   * Helper method which bind registered exchange with predefined queues
   *
   * @param adaptorResultContainer which is a AdaptorResultContainer class
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> queueBinding(AdaptorResultContainer adaptorResultContainer, String vhost) {
    LOGGER.trace("RabbitClient#queueBinding() method started");
    Promise<JsonObject> promise = Promise.promise();
    String topics;

    if (adaptorResultContainer.type.equalsIgnoreCase("resourceGroup")) {
      topics = adaptorResultContainer.adaptorId + DATA_WILDCARD_ROUTINGKEY;
    } else {
      topics = adaptorResultContainer.id + "/." + adaptorResultContainer.adaptorId;
    }
    bindQueue(QUEUE_DATA, adaptorResultContainer.adaptorId, topics, vhost)
        .compose(
            databaseResult ->
                bindQueue(REDIS_LATEST, adaptorResultContainer.adaptorId, topics, vhost))
        .compose(
            queueDataResult ->
                bindQueue(
                    QUEUE_ADAPTOR_LOGS,
                    adaptorResultContainer.adaptorId,
                    adaptorResultContainer.adaptorId + HEARTBEAT,
                    vhost))
        .compose(
            heartBeatResult ->
                bindQueue(
                    QUEUE_ADAPTOR_LOGS,
                    adaptorResultContainer.adaptorId,
                    adaptorResultContainer.adaptorId + DATA_ISSUE,
                    vhost))
        .compose(
            dataIssueResult ->
                bindQueue(
                    QUEUE_ADAPTOR_LOGS,
                    adaptorResultContainer.adaptorId,
                    adaptorResultContainer.adaptorId + DOWNSTREAM_ISSUE,
                    vhost))
        .compose(
            bindToAuditingQueueResult ->
                bindQueue(QUEUE_AUDITING, adaptorResultContainer.adaptorId, topics, vhost))
        .onSuccess(
            successHandler -> {
              JsonObject response = new JsonObject();
              response.mergeIn(
                  getResponseJson(
                      SUCCESS_CODE,
                      "Queue_Database",
                      QUEUE_DATA + " queue bound to " + adaptorResultContainer.adaptorId));
              LOGGER.debug("Success : " + response);
              promise.complete(response);
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("Error : queue bind error : " + failureHandler.getCause().toString());
              JsonObject response = getResponseJson(INTERNAL_ERROR_CODE, ERROR, QUEUE_BIND_ERROR);
              promise.fail(response.toString());
            });
    return promise.future();
  }

  Future<JsonObject> getUserPermissions(String userId) {
    LOGGER.trace("Info : RabbitClient#getUserpermissions() started");
    Promise<JsonObject> promise = Promise.promise();
    String url = "/api/users/" + encodeValue(userId) + "/permissions";
    webClient
        .requestAsync(REQUEST_GET, url)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                HttpResponse<Buffer> rmqResponse = handler.result();

                if (rmqResponse.statusCode() == HttpStatus.SC_OK) {
                  JsonArray permissionArray = new JsonArray(rmqResponse.body().toString());
                  promise.complete(permissionArray.getJsonObject(0));
                } else if (handler.result().statusCode() == HttpStatus.SC_NOT_FOUND) {
                  Response response =
                      new Response.Builder()
                          .withStatus(HttpStatus.SC_NOT_FOUND)
                          .withTitle(ResponseUrn.BAD_REQUEST_URN.getUrn())
                          .withDetail("user not exist.")
                          .withUrn(ResponseUrn.BAD_REQUEST_URN.getUrn())
                          .build();
                  promise.fail(response.toString());
                } else {
                  LOGGER.error(handler.cause());
                  LOGGER.error(handler.result());
                  Response response =
                      new Response.Builder()
                          .withStatus(rmqResponse.statusCode())
                          .withTitle(ResponseUrn.BAD_REQUEST_URN.getUrn())
                          .withDetail("problem while getting user permissions")
                          .withUrn(ResponseUrn.BAD_REQUEST_URN.getUrn())
                          .build();
                  promise.fail(response.toString());
                }
              } else {
                Response response =
                    new Response.Builder()
                        .withStatus(HttpStatus.SC_BAD_REQUEST)
                        .withTitle(ResponseUrn.BAD_REQUEST_URN.getUrn())
                        .withDetail(handler.cause().getLocalizedMessage())
                        .withUrn(ResponseUrn.BAD_REQUEST_URN.getUrn())
                        .build();
                promise.fail(response.toString());
              }
            });
    return promise.future();
  }

  Future<JsonObject> updateUserPermissions(
      String vhost, String userId, PermissionOpType type, String resourceId) {
    Promise<JsonObject> promise = Promise.promise();
    getUserPermissions(userId)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                String url = "/api/permissions/" + vhost + "/" + encodeValue(userId);
                JsonObject existingPermissions = handler.result();

                JsonObject updatedPermission =
                    getUpdatedPermission(existingPermissions, type, resourceId);
                webClient
                    .requestAsync(REQUEST_PUT, url, updatedPermission)
                    .onComplete(
                        updatePermissionHandler -> {
                          if (updatePermissionHandler.succeeded()) {
                            HttpResponse<Buffer> rmqResponse = updatePermissionHandler.result();
                            if (rmqResponse.statusCode() == HttpStatus.SC_NO_CONTENT) {
                              Response response =
                                  new Response.Builder()
                                      .withStatus(HttpStatus.SC_NO_CONTENT)
                                      .withTitle(ResponseUrn.SUCCESS_URN.getUrn())
                                      .withDetail("Permission updated successfully.")
                                      .withUrn(ResponseUrn.SUCCESS_URN.getUrn())
                                      .build();
                              promise.complete(response.toJson());
                            } else if (rmqResponse.statusCode() == HttpStatus.SC_CREATED) {
                              Response response =
                                  new Response.Builder()
                                      .withStatus(HttpStatus.SC_CREATED)
                                      .withTitle(ResponseUrn.SUCCESS_URN.getUrn())
                                      .withDetail("Permission updated successfully.")
                                      .withUrn(ResponseUrn.SUCCESS_URN.getUrn())
                                      .build();
                              promise.complete(response.toJson());
                            } else {
                              Response response =
                                  new Response.Builder()
                                      .withStatus(rmqResponse.statusCode())
                                      .withTitle(ResponseUrn.BAD_REQUEST_URN.getUrn())
                                      .withDetail(rmqResponse.statusMessage())
                                      .withUrn(ResponseUrn.BAD_REQUEST_URN.getUrn())
                                      .build();
                              promise.fail(response.toString());
                            }
                          } else {
                            Response response =
                                new Response.Builder()
                                    .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                                    .withTitle(ResponseUrn.BAD_REQUEST_URN.getUrn())
                                    .withDetail(updatePermissionHandler.cause().getMessage())
                                    .withUrn(ResponseUrn.BAD_REQUEST_URN.getUrn())
                                    .build();
                            promise.fail(response.toString());
                          }
                        });
              } else {
                promise.fail(handler.cause().getMessage());
              }
            });
    return promise.future();
  }

  private JsonObject getUpdatedPermission(
      JsonObject permissionsJson, PermissionOpType type, String resourceId) {
    StringBuilder permission;
    switch (type) {
      case ADD_READ:
      case ADD_WRITE:
        permission = new StringBuilder(permissionsJson.getString(type.permission));
        if (permission.length() != 0 && permission.indexOf(".*") != -1) {
          permission.deleteCharAt(0).deleteCharAt(0);
        }
        if (permission.length() != 0) {
          permission.append("|").append(resourceId);
        } else {
          permission.append(resourceId);
        }

        permissionsJson.put(type.permission, permission.toString());
        break;
      case DELETE_READ:
      case DELETE_WRITE:
        permission = new StringBuilder(permissionsJson.getString(type.permission));
        String[] permissionsArray = permission.toString().split("\\|");
        if (permissionsArray.length > 0) {
          Stream<String> stream = Arrays.stream(permissionsArray);
          String updatedPermission =
              stream.filter(item -> !item.equals(resourceId)).collect(Collectors.joining("|"));
          permissionsJson.put(type.permission, updatedPermission);
        }
        break;
      default:
        break;
    }
    return permissionsJson;
  }

  public class AdaptorResultContainer {
    public String apiKey;
    public String id;
    public String resourceServer;
    public String userid;
    public String adaptorId;
    public String vhost;
    public boolean isExchnageCreated;
    public String type;
  }
}
