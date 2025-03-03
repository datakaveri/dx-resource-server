package iudx.resource.server.databroker.util;

import static iudx.resource.server.apiserver.util.Constants.USER_ID;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.databroker.util.Util.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.serviceproxy.ServiceException;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.databroker.model.ExchangeSubscribersResponse;
import iudx.resource.server.databroker.model.UserResponse;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RabbitClient {
  private static final Logger LOGGER = LogManager.getLogger(RabbitClient.class);
  private final String amqpUrl;
  private final int amqpPort;
  private final RabbitWebClient rabbitWebClient;

  public RabbitClient(RabbitWebClient rabbitWebClient, String amqpUrl, int amqpPort) {
    this.rabbitWebClient = rabbitWebClient;
    this.amqpUrl = amqpUrl;
    this.amqpPort = amqpPort;
  }

  public Future<Void> deleteQueue(String queueName, String vhost) {
    LOGGER.trace("Info : RabbitClient#deleteQueue() started");
    Promise<Void> promise = Promise.promise();
    if (queueName != null && !queueName.isEmpty()) {
      LOGGER.debug("Info : queueName" + queueName);
      String url = "/api/queues/" + vhost + "/" + encodeValue(queueName);
      rabbitWebClient
          .requestAsync(REQUEST_DELETE, url)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    if (status == HttpStatus.SC_NO_CONTENT) {
                      promise.complete();
                    } else if (status == HttpStatus.SC_NOT_FOUND) {
                      promise.fail(new ServiceException(4, QUEUE_DOES_NOT_EXISTS));
                    }
                  }
                } else {
                  LOGGER.error("Fail : deletion of queue failed - ", ar.cause());
                  promise.fail(new ServiceException(0, QUEUE_DELETE_ERROR));
                }
              });
    }
    return promise.future();
  }

  public Future<List<String>> listQueueSubscribers(String queueName, String vhost) {
    LOGGER.trace("Info : RabbitClient#listQueueSubscribers() started");
    Promise<List<String>> promise = Promise.promise();
    if (queueName != null && !queueName.isEmpty()) {
      List<String> oroutingKeys = new ArrayList<String>();
      String url = "/api/queues/" + vhost + "/" + encodeValue(queueName) + "/bindings";
      rabbitWebClient
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
                        promise.complete(oroutingKeys);
                      }
                    } else {
                      promise.fail(new ServiceException(4, QUEUE_DOES_NOT_EXISTS));
                    }
                  }
                } else {
                  LOGGER.error("Error : Listing of Queue failed - " + ar.cause());
                  promise.fail(new ServiceException(0, QUEUE_LIST_ERROR));
                }
              });
    }
    return promise.future();
  }

  public Future<UserResponse> createUserIfNotExist(String userid, String vhost) {
    LOGGER.trace("Info : RabbitClient#createUserIfNotPresent() started");
    Promise<UserResponse> promise = Promise.promise();

    String password = randomPassword.get();
    String url = "/api/users/" + userid;
    rabbitWebClient
        .requestAsync(REQUEST_GET, url)
        .onComplete(
            reply -> {
              if (reply.succeeded()) {
                if (reply.result().statusCode() == HttpStatus.SC_NOT_FOUND) {
                  LOGGER.debug("Success : User not found. creating user");
                  Future<UserResponse> userCreated = createUser(userid, password, vhost, url);
                  userCreated.onComplete(
                      handler -> {
                        if (handler.succeeded()) {
                          UserResponse result = handler.result();
                          promise.complete(result);
                        } else {
                          LOGGER.error(
                              "Error : Error in user creation. Cause : " + handler.cause());
                          promise.fail(new ServiceException(0, USER_CREATION_ERROR));
                        }
                      });

                } else if (reply.result().statusCode() == HttpStatus.SC_OK) {
                  LOGGER.debug("DATABASE_READ_SUCCESS");
                  UserResponse userResponse = new UserResponse();
                  userResponse.setUserId(userid);
                  userResponse.setPassword(API_KEY_MESSAGE);
                  promise.complete(userResponse);
                } else {
                  LOGGER.error("Error : Something went wrong while finding user " + reply.cause());
                  promise.fail(new ServiceException(0, USER_CREATION_ERROR));
                }
              } else {
                LOGGER.error("Error : Something went wrong while finding user " + reply.cause());
                promise.fail(new ServiceException(0, USER_CREATION_ERROR));
              }
            });
    return promise.future();
  }

  Future<UserResponse> createUser(String userid, String password, String vhost, String url) {
    LOGGER.trace("Info : RabbitClient#createUser() started");
    Promise<UserResponse> promise = Promise.promise();
    JsonObject response = new JsonObject();
    JsonObject arg = new JsonObject();
    arg.put(PASSWORD, password);
    arg.put(TAGS, NONE);

    rabbitWebClient
        .requestAsync(REQUEST_PUT, url, arg)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                // Check if user is created
                if (ar.result().statusCode() == HttpStatus.SC_CREATED) {
                  LOGGER.debug("createUserRequest success");
                  response.put(USER_ID, userid);
                  response.put(PASSWORD, password);
                  UserResponse userResponse = new UserResponse();
                  userResponse.setUserId(userid);
                  userResponse.setPassword(password);
                  userResponse.setStatus("success");
                  userResponse.setDetail("User created and vHost permission set successfully.");
                  LOGGER.debug("Info : user created successfully");
                  // set permissions to vhost for newly created user
                  Future<Void> vhostPermission = setVhostPermissions(userid, vhost);
                  vhostPermission.onComplete(
                      handler -> {
                        if (handler.succeeded()) {
                          promise.complete(userResponse);
                        } else {
                          LOGGER.error(
                              "Error : error in setting vhostPermissions. Cause : ",
                              handler.cause());
                          promise.fail(new ServiceException(0, VHOST_PERMISSION_SET_ERROR));
                        }
                      });
                } else {
                  LOGGER.error(
                      "Error : createUser method - Some network error. cause" + ar.cause());
                  promise.fail(new ServiceException(0, NETWORK_ISSUE));
                }
              } else {
                LOGGER.info("Error : Something went wrong while creating user :", ar.cause());
                promise.fail(new ServiceException(0, CHECK_CREDENTIALS));
              }
            });
    return promise.future();
  }

  Future<Void> setVhostPermissions(String shaUsername, String vhost) {
    LOGGER.trace("Info : RabbitClient#setVhostPermissions() started");
    /* Construct URL to use */
    JsonObject vhostPermissions = new JsonObject();
    // all keys are mandatory. empty strings used for configure,read as not
    // permitted.
    vhostPermissions.put(CONFIGURE, DENY);
    vhostPermissions.put(WRITE, NONE);
    vhostPermissions.put(READ, NONE);
    Promise<Void> promise = Promise.promise();
    String url = "/api/permissions/" + vhost + "/" + encodeValue(shaUsername);
    rabbitWebClient
        .requestAsync(REQUEST_PUT, url, vhostPermissions)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("setVhost " + handler.result());
                if (handler.result().statusCode() == HttpStatus.SC_CREATED) {
                  LOGGER.debug(
                      "Success :write permission set for user [ "
                          + shaUsername
                          + " ] in vHost [ "
                          + vhost
                          + "]");
                  promise.complete();
                } else {
                  LOGGER.error(
                      "Error : error in write permission set for user [ "
                          + shaUsername
                          + " ] in vHost [ "
                          + vhost
                          + " ]");
                  promise.fail(new ServiceException(0, VHOST_PERMISSION_SET_ERROR));
                }
              } else {
                LOGGER.error(
                    "Error : error in write permission set for user [ "
                        + shaUsername
                        + " ] in vHost [ "
                        + vhost
                        + " ]");
                promise.fail(new ServiceException(0, VHOST_PERMISSION_SET_ERROR));
              }
            });
    return promise.future();
  }

  public Future<String> createQueue(String queueName, String vhost) {
    LOGGER.trace("Info : RabbitClient#createQueue() started");
    Promise<String> promise = Promise.promise();
    if (queueName != null && !queueName.isEmpty()) {
      JsonObject configProp = new JsonObject();
      JsonObject arguments = new JsonObject();
      arguments
          .put(X_MESSAGE_TTL_NAME, X_MESSAGE_TTL_VALUE)
          .put(X_MAXLENGTH_NAME, X_MAXLENGTH_VALUE)
          .put(X_QUEUE_MODE_NAME, X_QUEUE_MODE_VALUE);
      configProp.put(X_QUEUE_TYPE, true);
      configProp.put(X_QUEUE_ARGUMENTS, arguments);
      String url = "/api/queues/" + vhost + "/" + encodeValue(queueName); // "durable":true
      rabbitWebClient
          .requestAsync(REQUEST_PUT, url, configProp)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  LOGGER.debug("status code:" + +response.statusCode());
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    if (status == HttpStatus.SC_CREATED) {
                      promise.complete(queueName);
                    } else if (status == HttpStatus.SC_NO_CONTENT) {
                      promise.fail(new ServiceException(9, QUEUE_ALREADY_EXISTS));
                    } else if (status == HttpStatus.SC_BAD_REQUEST) {
                      promise.fail(
                          new ServiceException(8, QUEUE_ALREADY_EXISTS_WITH_DIFFERENT_PROPERTIES));
                    }
                  }
                } else {
                  LOGGER.error("Fail : Creation of Queue failed - ", ar.cause());
                  promise.fail(new ServiceException(0, QUEUE_CREATE_ERROR));
                }
              });
    }
    return promise.future();
  }

  public Future<Void> bindQueue(String exchangeName, String queueName, String topic, String vhost) {
    LOGGER.trace("Info : RabbitClient#bindQueue() started");
    JsonObject requestBody = new JsonObject();
    Promise<Void> promise = Promise.promise();
    if (exchangeName != null && !exchangeName.isEmpty()
        || queueName != null && queueName.isEmpty()) {

      String url =
          "/api/bindings/"
              + vhost
              + "/e/"
              + encodeValue(exchangeName)
              + "/q/"
              + encodeValue(queueName);

      requestBody.put("routing_key", topic);

      rabbitWebClient
          .requestAsync(REQUEST_POST, url, requestBody)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    LOGGER.info("Info : Binding " + topic + " Status is " + status);
                    if (status == HttpStatus.SC_CREATED) {
                      promise.complete();
                    } else if (status == HttpStatus.SC_NOT_FOUND) {
                      promise.fail(new ServiceException(4, QUEUE_EXCHANGE_NOT_FOUND));
                    }
                  }
                } else {
                  LOGGER.error("Fail : Binding of Queue failed - ", ar.cause());
                  promise.fail(new ServiceException(0, QUEUE_BIND_ERROR));
                }
              });
    }

    return promise.future();
  }

  public Future<JsonObject> updateUserPermissions(
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
                rabbitWebClient
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
                              promise.fail(new ServiceException(8, rmqResponse.statusMessage()));
                            }
                          } else {
                            promise.fail(new ServiceException(8, BAD_REQUEST.getDescription()));
                          }
                        });
              } else {
                promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
              }
            });
    return promise.future();
  }

  Future<JsonObject> getUserPermissions(String userId) {
    LOGGER.trace("Info : RabbitClient#getUserpermissions() started");
    Promise<JsonObject> promise = Promise.promise();
    String url = "/api/users/" + encodeValue(userId) + "/permissions";
    rabbitWebClient
        .requestAsync(REQUEST_GET, url)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                HttpResponse<Buffer> rmqResponse = handler.result();

                if (rmqResponse.statusCode() == HttpStatus.SC_OK) {
                  JsonArray permissionArray = new JsonArray(rmqResponse.body().toString());
                  promise.complete(permissionArray.getJsonObject(0));
                } else if (handler.result().statusCode() == HttpStatus.SC_NOT_FOUND) {
                  promise.fail(new ServiceException(4, "user not exist."));
                } else {
                  LOGGER.error(handler.cause());
                  promise.fail(new ServiceException(8, "problem while getting user permissions"));
                }
              } else {
                promise.fail(new ServiceException(8, handler.cause().getLocalizedMessage()));
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

  public Future<JsonObject> createExchange(String exchangeName, String vhost) {
    LOGGER.trace("Info : RabbitClient#createExchange() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject obj = new JsonObject();
    obj.put(TYPE, EXCHANGE_TYPE);
    obj.put(AUTO_DELETE, false);
    obj.put(DURABLE, true);
    String url = "/api/exchanges/" + vhost + "/" + encodeValue(exchangeName);
    rabbitWebClient
        .requestAsync(REQUEST_PUT, url, obj)
        .onComplete(
            requestHandler -> {
              if (requestHandler.succeeded()) {
                JsonObject responseJson = new JsonObject();
                HttpResponse<Buffer> response = requestHandler.result();
                int statusCode = response.statusCode();
                if (statusCode == HttpStatus.SC_CREATED) {
                  LOGGER.debug("Success : " + responseJson);
                  responseJson.put(EXCHANGE, exchangeName);
                  promise.complete(responseJson);
                } else if (statusCode == HttpStatus.SC_NO_CONTENT) {
                  promise.fail(new ServiceException(9, EXCHANGE_EXISTS));
                } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                  promise.fail(new ServiceException(9, EXCHANGE_EXISTS));
                }
              } else {
                promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
              }
            });

    return promise.future();
  }

  public Future<Void> deleteExchange(String exchangeName, String vhost) {
    LOGGER.trace("Info : RabbitClient#deleteExchange() started");
    Promise<Void> promise = Promise.promise();
    String url = "/api/exchanges/" + vhost + "/" + encodeValue(exchangeName);
    rabbitWebClient
        .requestAsync(REQUEST_DELETE, url)
        .onComplete(
            requestHandler -> {
              if (requestHandler.succeeded()) {
                /*JsonObject responseJson = new JsonObject();*/
                HttpResponse<Buffer> response = requestHandler.result();
                int statusCode = response.statusCode();
                LOGGER.debug("status code in delete Exchange " + statusCode);
                if (statusCode == HttpStatus.SC_NO_CONTENT) {
                  promise.complete();
                } else {
                  LOGGER.error("Error : Exchange not found ");
                  promise.fail(new ServiceException(4, EXCHANGE_NOT_FOUND));
                }
              } else {
                LOGGER.error("Error : " + EXCHANGE_DELETE_ERROR);
                promise.fail(new ServiceException(0, EXCHANGE_DELETE_ERROR));
              }
            });
    return promise.future();
  }

  public Future<Void> getExchange(String exchangeName, String vhost) {
    Promise<Void> promise = Promise.promise();
    String url;
    url = "/api/exchanges/" + vhost + "/" + encodeValue(exchangeName);
    rabbitWebClient
        .requestAsync(REQUEST_GET, url)
        .onComplete(
            result -> {
              if (result.succeeded()) {
                int status = result.result().statusCode();
                if (status == HttpStatus.SC_OK) {
                  LOGGER.debug("found Exchange");
                  promise.complete();
                } else if (status == HttpStatus.SC_NOT_FOUND) {
                  promise.fail(new ServiceException(4, EXCHANGE_NOT_FOUND));
                } else {
                  promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
                }
              } else {
                promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
              }
            });
    return promise.future();
  }

  public Future<ExchangeSubscribersResponse> listExchangeSubscribers(
      String exchangeName, String vhost) {
    LOGGER.trace("Info : RabbitClient#listExchangeSubscribers() started");
    Promise<ExchangeSubscribersResponse> promise = Promise.promise();
    String url = "/api/exchanges/" + vhost + "/" + encodeValue(exchangeName) + "/bindings/source";
    rabbitWebClient
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
                      Map<String, List<String>> res =
                          jsonBody.stream()
                              .map(JsonObject.class::cast)
                              .collect(
                                  Collectors.toMap(
                                      json -> json.getString("destination"),
                                      json -> List.of(json.getString("routing_key")),
                                      (existing, newValue) -> {
                                        existing.addAll(newValue);
                                        return existing;
                                      }));

                      LOGGER.debug("Info : exchange subscribers : " + jsonBody);
                      ExchangeSubscribersResponse finalResponse =
                          new ExchangeSubscribersResponse(res);
                      promise.complete(finalResponse);
                    }
                  } else if (status == HttpStatus.SC_NOT_FOUND) {
                    LOGGER.debug("Info : Exchange not found.");
                    promise.complete(new ExchangeSubscribersResponse(Map.of()));
                  }
                }
              } else {
                LOGGER.error("Fail : Listing of Exchange failed  ");
                promise.fail(new ServiceException(0, INTERNAL_SERVER_ERROR.getDescription()));
              }
            });

    return promise.future();
  }

  public Future<JsonObject> resetPasswordInRmq(String userid, String password) {
    LOGGER.trace("Info : RabbitClient#resetPassword() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    JsonObject arg = new JsonObject();
    arg.put(PASSWORD, password);
    arg.put(TAGS, NONE);
    String url = "/api/users/" + userid;
    LOGGER.debug("url : " + url);
    rabbitWebClient
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
}
