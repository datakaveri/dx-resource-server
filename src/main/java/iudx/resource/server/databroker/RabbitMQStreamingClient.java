package iudx.resource.server.databroker;

import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.databroker.util.Util.*;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.resource.server.databroker.util.Constants;
import iudx.resource.server.databroker.util.Util;

public class RabbitMQStreamingClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQStreamingClient.class);

  private RabbitMQClient rabbitMQClient;
  private RabbitMQWebClient rabbitMQwebClient;

  public RabbitMQStreamingClient(Vertx vertx, RabbitMQOptions rabbitConfigs,
      RabbitMQWebClient rabbitMQwebClient) {
    this.rabbitMQClient = getRabbitMQClient(vertx, rabbitConfigs);
    this.rabbitMQwebClient = rabbitMQwebClient;
    rabbitMQClient.start(rabbitMQClientStartupHandler -> {
      if (rabbitMQClientStartupHandler.succeeded()) {
        LOGGER.info("rabbit MQ client started");
      } else if (rabbitMQClientStartupHandler.failed()) {
        LOGGER.error("rabbit MQ client startup failed.");
      }
    });
  }

  private RabbitMQClient getRabbitMQClient(Vertx vertx, RabbitMQOptions rabbitConfigs) {
    return RabbitMQClient.create(vertx, rabbitConfigs);
  }

  /**
   * The createExchange implements the create exchange.
   * 
   * @param request which is a Json object
   * @Param vHost virtual-host
   * @return response which is a Future object of promise of Json type
   **/
  public Future<JsonObject> createExchange(JsonObject request, String vHost) {
    LOGGER.info("RabbitMQStreamingClient#createExchage() started");
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String url = "/api/exchanges/" + vHost + "/" + Util.encodedValue(exchangeName);
      JsonObject obj = new JsonObject();
      obj.put(TYPE, EXCHANGE_TYPE);
      obj.put(AUTO_DELETE, false);
      obj.put(DURABLE, true);
      rabbitMQwebClient.requestAsync(REQUEST_PUT, url, obj).onComplete(requestHandler -> {
        if (requestHandler.succeeded()) {
          JsonObject responseJson = new JsonObject();
          HttpResponse<Buffer> response = requestHandler.result();
          int statusCode = response.statusCode();
          System.out.println(statusCode);
          if (statusCode == HttpStatus.SC_CREATED) {
            responseJson.put(EXCHANGE, exchangeName);
          } else if (statusCode == HttpStatus.SC_NO_CONTENT) {
            responseJson = Util.getResponseJson(statusCode, FAILURE, EXCHANGE_EXISTS);
          } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            responseJson = Util.getResponseJson(statusCode, FAILURE,
                EXCHANGE_EXISTS_WITH_DIFFERENT_PROPERTIES);
          }
          promise.complete(responseJson);
        } else {
          JsonObject errorJson = Util.getResponseJson(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR,
              EXCHANGE_CREATE_ERROR);
          promise.fail(errorJson.toString());
        }
      });
    }
    return promise.future();
  }

  Future<JsonObject> getExchangeDetails(JsonObject request, String vHost) {
    LOGGER.info("RabbitMQStreamingClient#getExchange() started");
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String url = "/api/exchanges/" + vHost + "/" + Util.encodedValue(exchangeName);
      rabbitMQwebClient.requestAsync(REQUEST_GET, url).onComplete(requestHandler -> {
        if (requestHandler.succeeded()) {
          JsonObject responseJson = new JsonObject();
          HttpResponse<Buffer> response = requestHandler.result();
          int statusCode = response.statusCode();
          if (statusCode == HttpStatus.SC_OK) {
            responseJson = new JsonObject(response.body().toString());
          } else {
            responseJson = Util.getResponseJson(statusCode, FAILURE, EXCHANGE_NOT_FOUND);
          }
          promise.complete(responseJson);
        } else {
          JsonObject errorJson =
              Util.getResponseJson(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR, EXCHANGE_NOT_FOUND);
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
      exchangeName = exchangeName.replace("/", "%2F");
      String url;
      if (vhost.contains("/")) {
        url = "/api/exchanges/" + Util.encodedValue(vhost) + "/" + exchangeName;
      } else {
        url = "/api/exchanges/" + vhost + "/" + exchangeName;
      }
      rabbitMQwebClient.requestAsync(REQUEST_GET, url).onComplete(result -> {
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
        LOGGER.info("getExchange method response : " + response);
        promise.complete(response);
      });

    } else {
      promise.fail("exchangeName not provided");
    }

    return promise.future();

  }

  /**
   * The deleteExchange implements the delete exchange operation.
   * 
   * @param request which is a Json object
   * @Param VHost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> deleteExchange(JsonObject request, String vHost) {
    LOGGER.info("RabbitMQStreamingClient#deleteExchange() started");
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String url = "/api/exchanges/" + vHost + "/" + Util.encodedValue(exchangeName);
      rabbitMQwebClient.requestAsync(REQUEST_DELETE, url).onComplete(requestHandler -> {
        if (requestHandler.succeeded()) {
          JsonObject responseJson = new JsonObject();
          HttpResponse<Buffer> response = requestHandler.result();
          int statusCode = response.statusCode();
          if (statusCode == HttpStatus.SC_NO_CONTENT) {
            responseJson = new JsonObject();
            responseJson.put(EXCHANGE, exchangeName);
          } else {
            responseJson = Util.getResponseJson(statusCode, FAILURE, EXCHANGE_NOT_FOUND);
          }
          promise.complete(responseJson);
        } else {
          JsonObject errorJson = Util.getResponseJson(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR,
              EXCHANGE_DELETE_ERROR);
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
   * @param vHost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> listExchangeSubscribers(JsonObject request, String vhost) {
    LOGGER.info("RabbitMQStreamingClient#listExchangeSubscribers() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String url =
          "/api/exchanges/" + vhost + "/" + Util.encodedValue(exchangeName) + "/bindings/source";
      rabbitMQwebClient.requestAsync(REQUEST_GET, url).onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_OK) {
              Buffer body = response.body();
              if (body != null) {
                JsonArray jsonBody = new JsonArray(body.toString());
                Map res = jsonBody.stream().map(JsonObject.class::cast)
                    .collect(Collectors.toMap(json -> json.getString("destination"),
                        json -> new JsonArray().add(json.getString("routing_key")),
                        Util.bindingMergeOperator));
                LOGGER.info("exchange subscribers : " + jsonBody);
                finalResponse.clear().mergeIn(new JsonObject(res));
                if (finalResponse.isEmpty()) {
                  finalResponse.clear().mergeIn(
                      Util.getResponseJson(HttpStatus.SC_NOT_FOUND, FAILURE, EXCHANGE_NOT_FOUND),
                      true);
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.mergeIn(
                  Util.getResponseJson(HttpStatus.SC_NOT_FOUND, FAILURE, EXCHANGE_NOT_FOUND), true);
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Listing of Exchange failed" + ar.cause());
          JsonObject error = Util.getResponseJson(500, FAILURE, "Internal server error");
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
   * @param vHost virtual-host
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> createQueue(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String queueName = request.getString("queueName");
      String url = "/api/queues/" + vhost + "/" + Util.encodedValue(queueName);
      JsonObject configProp = new JsonObject();
      configProp.put(Constants.X_MESSAGE_TTL_NAME, Constants.X_MESSAGE_TTL_VALUE)
          .put(Constants.X_MAXLENGTH_NAME, Constants.X_MAXLENGTH_VALUE)
          .put(Constants.X_QUEUE_MODE_NAME, Constants.X_QUEUE_MODE_VALUE);
      rabbitMQwebClient.requestAsync(REQUEST_PUT, url, configProp).onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_CREATED) {
              finalResponse.put(Constants.QUEUE, queueName);
            } else if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.mergeIn(Util.getResponseJson(status, FAILURE, QUEUE_ALREADY_EXISTS),
                  true);
            } else if (status == HttpStatus.SC_BAD_REQUEST) {
              finalResponse.mergeIn(Util.getResponseJson(status, FAILURE,
                  QUEUE_ALREADY_EXISTS_WITH_DIFFERENT_PROPERTIES), true);
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Creation of Queue failed" + ar.cause());
          finalResponse.mergeIn(Util.getResponseJson(500, FAILURE, QUEUE_CREATE_ERROR));
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
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String queueName = request.getString("queueName");
      String url = "/api/queues/" + vhost + "/" + Util.encodedValue(queueName);
      rabbitMQwebClient.requestAsync(REQUEST_DELETE, url).onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put(Constants.QUEUE, queueName);
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.mergeIn(Util.getResponseJson(status, FAILURE, QUEUE_DOES_NOT_EXISTS));
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Deletion of Queue failed" + ar.cause());
          finalResponse.mergeIn(Util.getResponseJson(500, FAILURE, QUEUE_DELETE_ERROR));
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
    JsonObject finalResponse = new JsonObject();
    JsonObject requestBody = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String queueName = request.getString("queueName");
      JsonArray entities = request.getJsonArray("entities");
      int arrayPos = entities.size() - 1;
      String url = "/api/bindings/" + vhost + "/e/" + Util.encodedValue(exchangeName) + "/q/"
          + Util.encodedValue(queueName);
      for (Object rkey : entities) {
        requestBody.put("routing_key", rkey.toString());
        rabbitMQwebClient.requestAsync(REQUEST_POST, url, requestBody).onComplete(ar -> {
          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            if (response != null && !response.equals(" ")) {
              int status = response.statusCode();
              LOGGER.info("Binding " + rkey.toString() + "Success. Status is " + status);
              if (status == HttpStatus.SC_CREATED) {
                finalResponse.put(Constants.EXCHANGE, exchangeName);
                finalResponse.put(Constants.QUEUE, queueName);
                finalResponse.put(Constants.ENTITIES, entities);
              } else if (status == HttpStatus.SC_NOT_FOUND) {
                finalResponse
                    .mergeIn(Util.getResponseJson(status, FAILURE, QUEUE_EXCHANGE_NOT_FOUND));
              }
            }
            if (rkey == entities.getValue(arrayPos)) {
              promise.complete(finalResponse);
            }
          } else {
            LOGGER.error("Binding of Queue failed" + ar.cause());
            finalResponse.mergeIn(Util.getResponseJson(500, FAILURE, QUEUE_BIND_ERROR));
            promise.fail(finalResponse.toString());
          }
        });
      }
    }
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
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String queueName = request.getString("queueName");
      JsonArray entities = request.getJsonArray("entities");
      int arrayPos = entities.size() - 1;
      for (Object rkey : entities) {
        String url = "/api/bindings/" + vhost + "/e/" + Util.encodedValue(exchangeName) + "/q/"
            + Util.encodedValue(queueName) + "/" + Util.encodedValue((String) rkey);
        rabbitMQwebClient.requestAsync(REQUEST_DELETE, url).onComplete(ar -> {
          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            if (response != null && !response.equals(" ")) {
              int status = response.statusCode();
              if (status == HttpStatus.SC_NO_CONTENT) {
                finalResponse.put(Constants.EXCHANGE, exchangeName);
                finalResponse.put(Constants.QUEUE, queueName);
                finalResponse.put(Constants.ENTITIES, entities);
              } else if (status == HttpStatus.SC_NOT_FOUND) {
                finalResponse.mergeIn(Util.getResponseJson(status, FAILURE, ALL_NOT_FOUND));
              }
            }
            if (rkey == entities.getValue(arrayPos)) {
              promise.complete(finalResponse);
            }
          } else {
            LOGGER.error("Unbinding of Queue failed" + ar.cause());
            finalResponse.mergeIn(Util.getResponseJson(500, FAILURE, QUEUE_BIND_ERROR));
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
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String vhost = request.getString("vHost");
      String url = "/api/vhosts/" + Util.encodedValue(vhost);
      rabbitMQwebClient.requestAsync(REQUEST_PUT, url).onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_CREATED) {
              finalResponse.put(Constants.VHOST, vhost);
            } else if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.mergeIn(Util.getResponseJson(status, FAILURE, VHOST_ALREADY_EXISTS));
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Creation of vHost failed" + ar.cause());
          finalResponse.mergeIn(Util.getResponseJson(500, FAILURE, VHOST_CREATE_ERROR));
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
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String vhost = request.getString("vHost");
      String url = "/api/vhosts/" + Util.encodedValue(vhost);
      rabbitMQwebClient.requestAsync(REQUEST_DELETE, url).onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put(Constants.VHOST, vhost);
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.mergeIn(Util.getResponseJson(status, FAILURE, VHOST_NOT_FOUND));
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Deletion of vHost failed" + ar.cause());
          finalResponse.mergeIn(Util.getResponseJson(500, FAILURE, VHOST_DELETE_ERROR));
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
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null) {
      JsonArray vhostList = new JsonArray();
      String url = "/api/vhosts";
      rabbitMQwebClient.requestAsync(REQUEST_GET, url).onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_OK) {
              Buffer body = response.body();
              if (body != null) {
                JsonArray jsonBody = new JsonArray(body.toString());
                jsonBody.forEach(current -> {
                  JsonObject currentJson = new JsonObject(current.toString());
                  String vhostName = currentJson.getString("name");
                  vhostList.add(vhostName);
                });
                if (vhostList != null && !vhostList.isEmpty()) {
                  finalResponse.put(Constants.VHOST, vhostList);
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.mergeIn(Util.getResponseJson(status, FAILURE, VHOST_NOT_FOUND));
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Listing of vHost failed" + ar.cause());
          finalResponse.mergeIn(Util.getResponseJson(500, FAILURE, VHOST_LIST_ERROR));
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
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String queueName = request.getString("queueName");
      JsonArray oroutingKeys = new JsonArray();
      String url = "/api/queues/" + vhost + "/" + Util.encodedValue(queueName) + "/bindings";
      rabbitMQwebClient.requestAsync(REQUEST_GET, url).onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_OK) {
              Buffer body = response.body();
              if (body != null) {
                JsonArray jsonBody = new JsonArray(body.toString());
                jsonBody.forEach(current -> {
                  JsonObject currentJson = new JsonObject(current.toString());
                  String rkeys = currentJson.getString("routing_key");
                  if (rkeys != null && !rkeys.equalsIgnoreCase(queueName)) {
                    oroutingKeys.add(rkeys);
                  }
                });
                if (oroutingKeys != null && !oroutingKeys.isEmpty()) {
                  finalResponse.put(Constants.ENTITIES, oroutingKeys);
                } else {
                  finalResponse.clear().mergeIn(Util.getResponseJson(HttpStatus.SC_NOT_FOUND,
                      FAILURE, QUEUE_DOES_NOT_EXISTS));
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.clear()
                  .mergeIn(Util.getResponseJson(status, FAILURE, QUEUE_DOES_NOT_EXISTS));
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Listing of Queue failed" + ar.cause());
          finalResponse.mergeIn(Util.getResponseJson(500, FAILURE, QUEUE_LIST_ERROR));
          promise.fail(finalResponse.toString());
        }
      });
    }
    return promise.future();
  }

  public Future<JsonObject> registerAdaptor(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    System.out.println(request.toString());
    /* Get the ID and userName from the request */
    String id = request.getString("resourceGroup");
    String resourceServer = request.getString("resourceServer");
    String userName = request.getString(CONSUMER);
    LOGGER.info("Resource Group Name given by user is : " + id);
    LOGGER.info("Resource Server Name by user is : " + resourceServer);
    LOGGER.info("User Name is : " + userName);
    /* Construct a response object */
    JsonObject registerResponse = new JsonObject();
    /* Validate the request object */
    if (request != null && !request.isEmpty()) {
      /* Goto Create user if ID is not empty */
      if (id != null && !id.isEmpty() && !id.isBlank()) {
        /* Validate the ID for special characters */
        if (Util.isValidId.test(id)) {
          /* Validate the userName */
          if (userName != null && !userName.isBlank() && !userName.isEmpty()) {
            /* Create a new user, if it does not exists */
            Future<JsonObject> userCreationFuture = createUserIfNotExist(userName, vhost);
            /* On completion of user creation, handle the result */
            userCreationFuture.onComplete(rh -> {
              if (rh.succeeded()) {
                /* Obtain the result of user creation */
                JsonObject result = rh.result();
                LOGGER.info("Response of createUserIfNotExist is : " + result);
                /* Construct the domain, userNameSHA, userID and adaptorID */
                String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
                String userNameSha = Util.getSha(userName);
                String userID = domain + "/" + userNameSha;
                String adaptorID = userID + "/" + resourceServer + "/" + id;
                LOGGER.info("userID is : " + userID);
                LOGGER.info("adaptorID is : " + adaptorID);
                if (adaptorID != null && !adaptorID.isBlank() && !adaptorID.isEmpty()) {
                  JsonObject json = new JsonObject();
                  json.put(EXCHANGE_NAME, adaptorID);
                  /* Create an exchange if it does not exists */
                  Future<JsonObject> exchangeDeclareFuture = createExchange(json, vhost);
                  /* On completion of exchange creation, handle the result */
                  exchangeDeclareFuture.onComplete(ar -> {
                    if (ar.succeeded()) {
                      /* Obtain the result of exchange creation */
                      JsonObject obj = ar.result();
                      LOGGER.info("Response of createExchange is : " + obj);
                      LOGGER.info("exchange name provided : " + adaptorID);
                      LOGGER.info("exchange name received : " + obj.getString("exchange"));
                      // if exchange just registered then set topic permission and bind with queues
                      if (!obj.containsKey("detail")) {
                        Future<JsonObject> topicPermissionFuture = setTopicPermissions(vhost,
                            domain + "/" + userNameSha + "/" + resourceServer + "/" + id, userID);
                        topicPermissionFuture.onComplete(topicHandler -> {
                          if (topicHandler.succeeded()) {
                            LOGGER.info("Write permission set on topic for exchange "
                                + obj.getString("exchange"));
                            /* Bind the exchange with the database and adaptorLogs queue */
                            Future<JsonObject> queueBindFuture = queueBinding(
                                domain + "/" + userNameSha + "/" + resourceServer + "/" + id);
                            queueBindFuture.onComplete(res -> {
                              if (res.succeeded()) {
                                LOGGER.info("Queue_Database, Queue_adaptorLogs binding done with "
                                    + obj.getString("exchange") + " exchange");
                                /* Construct the response for registration of adaptor */
                                registerResponse.put(USER_NAME, domain + "/" + userNameSha);
                                /*
                                 * APIKEY should be equal to password generated. For testing use
                                 * APIKEY_TEST_EXAMPLE
                                 */
                                registerResponse.put(APIKEY, APIKEY_TEST_EXAMPLE);
                                registerResponse.put(ID,
                                    domain + "/" + userNameSha + "/" + resourceServer + "/" + id);
                                registerResponse.put(VHOST, VHOST_IUDX);
                                LOGGER.info("registerResponse : " + registerResponse);
                                promise.complete(registerResponse);
                                // handler.handle(Future.succeededFuture(registerResponse));
                              } else {
                                /* Handle Queue Error */
                                LOGGER.error(
                                    "error in queue binding with adaptor - cause : " + res.cause());
                                registerResponse.put(ERROR, QUEUE_BIND_ERROR);
                                promise.fail(registerResponse.toString());
                                // handler.handle(Future.failedFuture(registerResponse.toString()));
                              }
                            });
                          } else {
                            /* Handle Topic Permission Error */
                            LOGGER.info("topic permissions not set for exchange "
                                + obj.getString("exchange") + " - cause : "
                                + topicHandler.cause().getMessage());

                            registerResponse.put(ERROR, TOPIC_PERMISSION_SET_ERROR);
                            promise.fail(registerResponse.toString());
                            // handler.handle(Future.failedFuture(registerResponse.toString()));
                          }
                        });
                      } else if (obj.getString("detail") != null
                          && !obj.getString("detail").isEmpty()
                          && obj.getString("detail").equalsIgnoreCase("Exchange already exists")) {
                        /* Handle Exchange Error */
                        LOGGER.error("something wrong in exchange declaration : " + ar.cause());
                        registerResponse.put(ERROR, EXCHANGE_EXISTS);
                        promise.fail(registerResponse.toString());
                        // handler.handle(Future.failedFuture(registerResponse.toString()));
                      }
                    } else {
                      /* Handle Exchange Error */
                      registerResponse.put(ERROR, EXCHANGE_DECLARATION_ERROR);
                      promise.fail(registerResponse.toString());
                      // handler.handle(Future.failedFuture(registerResponse.toString()));
                    }
                  });
                } else {
                  /* Handle Request Error */
                  LOGGER.error("AdaptorID / Exchange not provided in request");
                  registerResponse.put(ERROR, ADAPTOR_ID_NOT_PROVIDED);
                  promise.fail(registerResponse.toString());
                  // handler.handle(Future.failedFuture(registerResponse.toString()));
                }
              } else if (rh.failed()) {
                /* Handle User Creation Error */
                LOGGER.error("User creation failed. " + rh.cause());
                registerResponse.put(ERROR, USER_CREATION_ERROR);
                promise.fail(registerResponse.toString());
                // handler.handle(Future.failedFuture(registerResponse.toString()));
              } else {
                /* Handle User Creation Error */
                LOGGER.error("User creation failed. " + rh.cause());
                registerResponse.put(ERROR, USER_CREATION_ERROR);
                promise.fail(registerResponse.toString());
                // handler.handle(Future.failedFuture(registerResponse.toString()));
              }
            });
          } else {
            /* Handle Request Error */
            LOGGER.error("user not provided in adaptor registration");
            registerResponse.put(ERROR, USER_NAME_NOT_PROVIDED);
            promise.fail(registerResponse.toString());
            // handler.handle(Future.failedFuture(registerResponse.toString()));
          }
        } else {
          /* Handle Invalid ID Error */
          registerResponse.put(ERROR, INVALID_ID);
          promise.fail(registerResponse.toString());
          // handler
          // .handle(Future.failedFuture(new JsonObject().put("error", "invalid id").toString()));
          LOGGER.error("id not provided in adaptor registration");
        }
      } else {
        /* Handle Request Error */
        LOGGER.error("id not provided in adaptor registration");
        registerResponse.put(ERROR, ID_NOT_PROVIDED);
        promise.fail(registerResponse.toString());
        // handler.handle(Future.failedFuture(registerResponse.toString()));
      }
    } else {
      /* Handle Request Error */
      LOGGER.error("Bad Request");
      registerResponse.put(ERROR, BAD_REQUEST);
      promise.fail(registerResponse.toString());
      // handler.handle(Future.failedFuture(registerResponse.toString()));
    }
    return promise.future();
  }

  Future<JsonObject> deleteAdapter(JsonObject json, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    System.out.println(json.toString());
    Future<JsonObject> result = getExchange(json, vhost);
    result.onComplete(resultHandler -> {
      if (resultHandler.succeeded()) {
        int status = resultHandler.result().getInteger("type");
        if (status == 200) {
          String exchangeID = json.getString("id");
          rabbitMQClient.exchangeDelete(exchangeID, rh -> {
            if (rh.succeeded()) {
              LOGGER.info(exchangeID + " adaptor deleted successfully");
              finalResponse.mergeIn(getResponseJson(200, "success", "adaptor deleted"));
              finalResponse.put("id", exchangeID);
            } else if (rh.failed()) {
              finalResponse.clear()
                  .mergeIn(getResponseJson(500, "adaptor delete", rh.cause().toString()));
              promise.fail(finalResponse.toString());
            } else {
              LOGGER.error("Something wrong in deleting adaptor" + rh.cause());
              finalResponse.mergeIn(getResponseJson(400, "bad request", "nothing to delete"));
              promise.fail(finalResponse.toString());
            }
            promise.complete(finalResponse);
          });

        } else if (status == 404) { // exchange not found
          finalResponse.clear().mergeIn(
              getResponseJson(status, "not found", resultHandler.result().getString("detail")));
        } else { // some other issue
          finalResponse.mergeIn(getResponseJson(400, "bad request", "nothing to delete"));
        }
      }
      if (resultHandler.failed()) {
        LOGGER.error("deleteAdaptor - resultHandler failed : " + resultHandler.cause());
        finalResponse.clear().mergeIn(getResponseJson(500, "bad request", "nothing to delete"));
      }
    });
    return promise.future();
  }

  /**
   * The createUserIfNotExist implements the create user if does not exist.
   * 
   * @param userName which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   **/
  Future<JsonObject> createUserIfNotExist(String userName, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    /* Create a response object */
    JsonObject response = new JsonObject();
    Future<JsonObject> future = createUserIfNotPresent(userName, vhost);
    future.onComplete(handler -> {
      /* On successful response handle the result */
      if (handler.succeeded()) {
        /* Respond to the requestor */
        JsonObject result = handler.result();
        response.put(SHA_USER_NAME, result.getString("shaUsername"));
        response.put(APIKEY, result.getString("password"));
        response.put(TYPE, result.getString("type"));
        response.put(TITLE, result.getString("title"));
        response.put(DETAILS, result.getString("detail"));
        response.put(VHOST_PERMISSIONS, result.getString("vhostPermissions"));
        promise.complete(response);
      } else {
        LOGGER.info("Something went wrong - Cause: " + handler.cause());
        response.put(ERROR, USER_CREATION_ERROR);
        promise.fail(response.toString());
      }
    });
    return promise.future();
  }

  /**
   * createUserIfNotExist helper method which check user existence. Create user if not present
   * 
   * @param userName which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   **/
  Future<JsonObject> createUserIfNotPresent(String userName, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    /* Get domain, shaUsername from userName */
    String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
    String shaUsername = domain + "/" + Util.getSha(userName);
    // This API requires user name in path parameter. Encode the username as it
    // contains a "/"
    String url = "/api/users/" + Util.encodedValue(shaUsername);
    /* Check if user exists */
    JsonObject response = new JsonObject();
    rabbitMQwebClient.requestAsync(REQUEST_GET, url).onComplete(reply -> {
      if (reply.succeeded()) {
        /* Check if user not found */
        if (reply.result().statusCode() == HttpStatus.SC_NOT_FOUND) {
          LOGGER.info(
              "createUserIfNotExist success method : User not found. So creating user .........");
          /* Create new user */
          Future<JsonObject> userCreated = createUser(shaUsername, vhost, url);
          userCreated.onComplete(handler -> {
            if (handler.succeeded()) {
              /* Handle the response */
              JsonObject result = handler.result();
              response.put(SHA_USER_NAME, result.getString("shaUsername"));
              response.put(APIKEY, result.getString("password"));
              response.put(TYPE, result.getString("type"));
              response.put(TITLE, result.getString("title"));
              response.put(DETAILS, result.getString("detail"));
              response.put(VHOST_PERMISSIONS, result.getString("vhostPermissions"));
              promise.complete(response);
            } else {
              LOGGER.error("createUser method onComplete() - Error in user creation. Cause : "
                  + handler.cause());
            }
          });

        } else if (reply.result().statusCode() == HttpStatus.SC_OK) {
          // user exists , So something useful can be done here
          // TODO : Need to get the "apiKey"
          /* Handle the response if a user exists */
          JsonObject result = reply.result().bodyAsJsonObject();
          response.put(SHA_USER_NAME, result.getString("shaUsername"));
          response.put(TYPE, USER_EXISTS);
          response.put(TITLE, SUCCESS);
          response.put(DETAILS, USER_ALREADY_EXISTS);
          promise.complete(response);
        }

      } else {
        /* Handle API error */
        LOGGER.info("Something went wrong while finding user using mgmt API: " + reply.cause());
        promise.fail(reply.cause().toString());
      }

    });

    return promise.future();

  }


  /**
   * CreateUserIfNotPresent's helper method which creates user if not present.
   * 
   * @param userName which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   **/
  Future<JsonObject> createUser(String shaUsername, String vhost, String url) {
    Promise<JsonObject> promise = Promise.promise();
    // now creating user using same url with method put
    // HttpRequest<Buffer> createUserRequest = webClient.put(url).basicAuthentication(user,
    // password);
    JsonObject response = new JsonObject();
    JsonObject arg = new JsonObject();
    arg.put(PASSWORD, Util.randomPassword.get());
    arg.put(TAGS, NONE);

    rabbitMQwebClient.requestAsync(REQUEST_PUT, url, arg).onComplete(ar -> {
      if (ar.succeeded()) {
        /* Check if user is created */
        if (ar.result().statusCode() == HttpStatus.SC_CREATED) {
          LOGGER.info("createUserRequest success");
          response.put(SHA_USER_NAME, shaUsername);
          response.put(PASSWORD, arg.getString("password"));
          response.put(TITLE, SUCCESS);
          response.put(TYPE, "" + ar.result().statusCode());
          response.put(DETAILS, USER_CREATED);
          LOGGER.info("createUser method : given user created successfully");
          // set permissions to vhost for newly created user
          Future<JsonObject> vhostPermission = setVhostPermissions(shaUsername, vhost);
          vhostPermission.onComplete(handler -> {
            if (handler.succeeded()) {
              response.put(VHOST_PERMISSIONS, handler.result().getString("vhostPermissions"));
              promise.complete(response);
            } else {
              /* Handle error */
              LOGGER.error("Error in setting vhostPermissions. Cause : " + handler.cause());
              response.put(VHOST_PERMISSIONS, VHOST_PERMISSIONS_FAILURE);
              promise.complete(response);
            }
          });

        } else {
          /* Handle error */
          LOGGER.error("createUser method - Some network error. cause" + ar.cause());
          response.put(FAILURE, NETWORK_ISSUE);
          promise.fail(response.toString());
        }
      } else {
        /* Handle error */
        LOGGER.info("Something went wrong while creating user using mgmt API :" + ar.cause());
        response.put(FAILURE, CHECK_CREDENTIALS);
        promise.fail(response.toString());
      }
    });
    return promise.future();
  }

  /**
   * set topic permissions.
   * 
   * @param vhost which is a String
   * @param adaptorID which is a String
   * @param shaUsername which is a String
   * @return response which is a Future object of promise of Json type
   **/
  private Future<JsonObject> setTopicPermissions(String vhost, String adaptorID, String userID) {
    // now set write permission to user for this adaptor(exchange)
    String url = "/api/topic-permissions/" + vhost + "/" + Util.encodedValue(userID);
    JsonObject param = new JsonObject();
    // set all mandatory fields
    param.put(EXCHANGE, adaptorID);
    param.put(WRITE, ALLOW);
    param.put(READ, DENY);
    param.put(CONFIGURE, DENY);

    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    rabbitMQwebClient.requestAsync(REQUEST_PUT, url, param).onComplete(result -> {
      if (result.succeeded()) {
        /* Check if request was a success */
        if (result.result().statusCode() == HttpStatus.SC_CREATED) {
          response.put(TOPIC_PERMISSION, TOPIC_PERMISSION_SET_SUCCESS);
          LOGGER.info("Topic permission set");
          promise.complete(response);
        } else if (result.result()
            .statusCode() == HttpStatus.SC_NO_CONTENT) { /* Check if request was already served */
          response.put(TOPIC_PERMISSION, TOPIC_PERMISSION_ALREADY_SET);
          promise.complete(response);
        } else { /* Check if request has an error */
          LOGGER.error("Error in setting topic permissions" + result.result().statusMessage());
          response.put(TOPIC_PERMISSION, TOPIC_PERMISSION_SET_ERROR);
          promise.fail(response.toString());
        }
      } else { /* Check if request has an error */
        LOGGER.error("Error in setting topic permission : " + result.cause());
        response.put(TOPIC_PERMISSION, TOPIC_PERMISSION_SET_ERROR);
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
   **/
  private Future<JsonObject> setVhostPermissions(String shaUsername, String vhost) {
    // set permissions for this user
    /* Construct URL to use */
    String url = "/api/permissions/" + vhost + "/" + Util.encodedValue(shaUsername);
    JsonObject vhostPermissions = new JsonObject();
    // all keys are mandatory. empty strings used for configure,read as not
    // permitted.
    vhostPermissions.put(CONFIGURE, DENY);
    vhostPermissions.put(WRITE, ALLOW);
    vhostPermissions.put(READ, ALLOW);

    Promise<JsonObject> promise = Promise.promise();
    /* Construct a response object */
    JsonObject vhostPermissionResponse = new JsonObject();
    rabbitMQwebClient.requestAsync(REQUEST_PUT, url, vhostPermissions).onComplete(handler -> {
      if (handler.succeeded()) {
        /* Check if permission was set */
        if (handler.result().statusCode() == HttpStatus.SC_CREATED) {
          LOGGER.info("vhostPermissionRequest success");
          vhostPermissionResponse.put(VHOST_PERMISSIONS, VHOST_PERMISSIONS_WRITE);
          LOGGER.info(
              "write permission set for user [ " + shaUsername + " ] in vHost [ " + vhost + "]");
          promise.complete(vhostPermissionResponse);
        } else {
          LOGGER.error("Error in write permission set for user [ " + shaUsername + " ] in vHost [ "
              + vhost + " ]");
          vhostPermissionResponse.put(VHOST_PERMISSIONS, VHOST_PERMISSION_SET_ERROR);
          promise.fail(vhostPermissions.toString());
        }
      } else {
        /* Check if request has an error */
        LOGGER.error("Error in write permission set for user [ " + shaUsername + " ] in vHost [ "
            + vhost + " ]");
        vhostPermissionResponse.put(VHOST_PERMISSIONS, VHOST_PERMISSION_SET_ERROR);
        promise.fail(vhostPermissions.toString());
      }
    });

    return promise.future();
  }

  /*
   * helper method which bind registered exchange with predefined queues
   * 
   * @param adaptorID which is a String object
   * 
   * @return response which is a Future object of promise of Json type
   */
  @Deprecated
  // TODO : name changed to avoid same name method
  Future<JsonObject> queueBinding1(String adaptorID) {
    Promise<JsonObject> promise = Promise.promise();
    /* Create a response object */
    JsonObject response = new JsonObject();
    // Now bind newly created adaptor with queues i.e. adaptorLogs,database
    String topics = adaptorID + "/.*";
    /* Bind to database queue */
    rabbitMQClient.queueBind(QUEUE_DATA, adaptorID, topics, result -> {
      if (result.succeeded()) {
        /* On success bind to adaptorLogs queue */
        response.put("Queue_Database", QUEUE_DATA + " queue bound to " + adaptorID);

        rabbitMQClient.queueBind(QUEUE_ADAPTOR_LOGS, adaptorID, adaptorID + HEARTBEAT,
            bindingheartBeatResult -> {
              if (bindingheartBeatResult.succeeded()) {
                rabbitMQClient.queueBind(QUEUE_ADAPTOR_LOGS, adaptorID, adaptorID + DATA_ISSUE,
                    bindingdataIssueResult -> {
                      if (bindingdataIssueResult.succeeded()) {
                        rabbitMQClient.queueBind(QUEUE_ADAPTOR_LOGS, adaptorID,
                            adaptorID + DOWNSTREAM_ISSUE, bindingdownstreamIssueResult -> {
                              if (bindingdownstreamIssueResult.succeeded()) {

                                promise.complete(response);

                              } else {
                                /* Handle bind to adaptorLogs queue error */
                                LOGGER.error(" Queue_adaptorLogs binding error : "
                                    + bindingdownstreamIssueResult.cause());
                                response.put(ERROR, QUEUE_BIND_ERROR);
                                promise.fail(response.toString());
                              }
                            });
                      } else {
                        /* Handle bind to adaptorLogs queue error */
                        LOGGER.error(
                            " Queue_adaptorLogs binding error : " + bindingdataIssueResult.cause());
                        response.put(ERROR, QUEUE_BIND_ERROR);
                        promise.fail(response.toString());
                      }
                    });
              } else {
                /* Handle bind to adaptorLogs queue error */
                LOGGER
                    .error(" Queue_adaptorLogs binding error : " + bindingheartBeatResult.cause());
                response.put(ERROR, QUEUE_BIND_ERROR);
                promise.fail(response.toString());
              }
            });
      } else {
        /* Handle bind to database queue error */
        LOGGER.error(" Queue_Database binding error : " + result.cause());
        response.put(ERROR, QUEUE_BIND_ERROR);
        promise.fail(response.toString());
      }
    });

    return promise.future();
  }

  // TODO : discuss with team for new compose functionality.
  Future<JsonObject> queueBinding(String adaptorID) {
    LOGGER.info("RabbitMQStreamingClient#queueBinding() method started");
    Promise<JsonObject> promise = Promise.promise();
    String topics = adaptorID + "/.*";
    bindQueue(QUEUE_DATA, adaptorID, topics)
        .compose(queueDataResult -> bindQueue(QUEUE_ADAPTOR_LOGS, adaptorID, adaptorID + HEARTBEAT))
        .compose(
            heartBeatResult -> bindQueue(QUEUE_ADAPTOR_LOGS, adaptorID, adaptorID + DATA_ISSUE))
        .compose(dataIssueResult -> bindQueue(QUEUE_ADAPTOR_LOGS, adaptorID,
            adaptorID + DOWNSTREAM_ISSUE))
        .onSuccess(successHandler -> {
          JsonObject response = new JsonObject();
          response.put("Queue_Database", QUEUE_DATA + " queue bound to " + adaptorID);
          promise.complete(response);
        }).onFailure(failureHandler -> {
          LOGGER.error("queue bind error : " + failureHandler.getCause().toString());
          JsonObject response = new JsonObject();
          response.put(ERROR, QUEUE_BIND_ERROR);
          promise.fail(response.toString());
        });
    return promise.future();
  }

  Future<JsonObject> bindQueue(String data, String adaptorID, String topics) {
    LOGGER.info("RabbitMQStreamingClient#bindQueue() started");
    LOGGER.info("data : " + data + " adaptorID : " + adaptorID + " topics : " + topics);
    Promise<JsonObject> promise = Promise.promise();
    rabbitMQClient.queueBind(data, adaptorID, topics, handler -> {
      if (handler.succeeded()) {
        promise.complete();
      } else {
        LOGGER.error(" Queue" + data + " binding error : " + handler.cause());
        promise.fail(handler.cause());
      }
    });
    return promise.future();
  }
}
