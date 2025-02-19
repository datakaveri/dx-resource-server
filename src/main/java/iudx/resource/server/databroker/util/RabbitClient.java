package iudx.resource.server.databroker.util;

import static iudx.resource.server.apiserver.util.Constants.USER_ID;
import static iudx.resource.server.database.util.Constants.ERROR;
import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.databroker.util.Util.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.databroker.model.UserResponse;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RabbitClient {
  private static final Logger LOGGER = LogManager.getLogger(RabbitClient.class);
  public static Supplier<String> randomPassword =
      () -> {
        UUID uid = UUID.randomUUID();
        byte[] pwdBytes =
            ByteBuffer.wrap(new byte[16])
                .putLong(uid.getMostSignificantBits())
                .putLong(uid.getLeastSignificantBits())
                .array();
        return Base64.getUrlEncoder().encodeToString(pwdBytes).substring(0, 22);
      };
  private final String amqpUrl;
  private final int amqpPort;
  private RabbitWebClient rabbitWebClient;

  public RabbitClient(RabbitWebClient rabbitWebClient, String amqpUrl, int amqpPort) {
    this.rabbitWebClient = rabbitWebClient;
    this.amqpUrl = amqpUrl;
    this.amqpPort = amqpPort;
  }

  public Future<Void> deleteQueue(String queueName, String vhost) {
    LOGGER.trace("Info : RabbitClient#deleteQueue() started");
    Promise<Void> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
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
                      /*finalResponse.put(QUEUE, queueName);*/
                      promise.complete();
                    } else if (status == HttpStatus.SC_NOT_FOUND) {
                      finalResponse.mergeIn(
                          getResponseJson(
                              HttpStatusCode.NO_CONTENT.getUrn(),
                              status,
                              FAILURE,
                              QUEUE_DOES_NOT_EXISTS));
                      promise.fail(finalResponse.toString());
                    }
                  }
                  LOGGER.info(finalResponse);
                } else {
                  LOGGER.error("Fail : deletion of queue failed - ", ar.cause());
                  finalResponse.mergeIn(
                      getResponseJson(
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                          INTERNAL_ERROR_CODE,
                          FAILURE,
                          QUEUE_DELETE_ERROR));
                  promise.fail(finalResponse.toString());
                }
              });
    }
    return promise.future();
  }

  public Future<List<String>> listQueueSubscribers(String queueName, String vhost) {
    LOGGER.trace("Info : RabbitClient#listQueueSubscribers() started");
    JsonObject finalResponse = new JsonObject();
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
                        /*finalResponse.put(ENTITIES, oroutingKeys);*/
                        promise.complete(oroutingKeys);
                      }
                    } else if (status == HttpStatus.SC_NOT_FOUND) {
                      finalResponse
                          .clear()
                          .mergeIn(
                              getResponseJson(
                                  HttpStatusCode.NOT_FOUND.getUrn(),
                                  status,
                                  FAILURE,
                                  QUEUE_DOES_NOT_EXISTS));
                      promise.fail(finalResponse.toString());
                    }
                  }
                  LOGGER.debug("Info : " + finalResponse);

                } else {
                  LOGGER.error("Error : Listing of Queue failed - " + ar.cause());
                  finalResponse.mergeIn(
                      getResponseJson(
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                          INTERNAL_ERROR_CODE,
                          FAILURE,
                          QUEUE_LIST_ERROR));
                  promise.fail(finalResponse.toString());
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
    /* Check if user exists */
    JsonObject response = new JsonObject();
    rabbitWebClient
        .requestAsync(REQUEST_GET, url)
        .onComplete(
            reply -> {
              if (reply.succeeded()) {
                /* Check if user not found */
                if (reply.result().statusCode() == HttpStatus.SC_NOT_FOUND) {
                  LOGGER.debug("Success : User not found. creating user");
                  /* Create new user */

                  Future<UserResponse> userCreated = createUser(userid, password, vhost, url);
                  userCreated.onComplete(
                      handler -> {
                        if (handler.succeeded()) {
                          /* Handle the response */
                          UserResponse result = handler.result();
                          /*response.put(USER_ID, userid);
                          response.put(APIKEY, password);
                          response.put(TYPE, result.getInteger("type"));
                          response.put(TITLE, result.getString("title"));
                          response.put(DETAILS, result.getString("detail"));
                          response.put(VHOST_PERMISSIONS, vhost);*/
                          promise.complete(/*response*/ result);
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
                  UserResponse userResponse = new UserResponse();
                  userResponse.setUserId(userid);
                  userResponse.setPassword(API_KEY_MESSAGE);
                  /*response.put(USER_ID, userid);
                  response.put(APIKEY, API_KEY_MESSAGE);*/
                  /*response.mergeIn(
                  getResponseJson(SUCCESS_CODE, DATABASE_READ_SUCCESS, DATABASE_READ_SUCCESS));*/
                  /*response.put(VHOST_PERMISSIONS, vhost);*/
                  promise.complete(userResponse);
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
                          promise.fail("Error : error in setting vhostPermissions");
                        }
                      });

                } else {
                  LOGGER.error(
                      "Error : createUser method - Some network error. cause" + ar.cause());
                  response.put(FAILURE, NETWORK_ISSUE);
                  promise.fail(response.toString());
                }
              } else {
                LOGGER.info(
                    "Error : Something went wrong while creating user using mgmt API :",
                    ar.cause());
                response.put(FAILURE, CHECK_CREDENTIALS);
                promise.fail(response.toString());
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
    /* Construct a response object */
    JsonObject vhostPermissionResponse = new JsonObject();
    String url = "/api/permissions/" + vhost + "/" + encodeValue(shaUsername);
    rabbitWebClient
        .requestAsync(REQUEST_PUT, url, vhostPermissions)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("setVhost " + handler.result());
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
                  promise.complete();
                } else {
                  LOGGER.error(
                      "Error : error in write permission set for user [ "
                          + shaUsername
                          + " ] in vHost [ "
                          + vhost
                          + " ]");
                  vhostPermissionResponse.mergeIn(
                      getResponseJson(
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                          INTERNAL_ERROR_CODE,
                          VHOST_PERMISSIONS,
                          VHOST_PERMISSION_SET_ERROR));
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
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                        INTERNAL_ERROR_CODE,
                        VHOST_PERMISSIONS,
                        VHOST_PERMISSION_SET_ERROR));
                promise.fail(vhostPermissions.toString());
              }
            });
    return promise.future();
  }

  public Future<String> createQueue(String queueName, String vhost) {
    LOGGER.trace("Info : RabbitClient#createQueue() started");
    Promise<String> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
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
                  LOGGER.debug(
                      "------------------------------"
                          + response.statusCode()
                          + "----------------");
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    if (status == HttpStatus.SC_CREATED) {
                      /*finalResponse.put(QUEUE, queueName);*/
                      promise.complete(queueName);
                    } else if (status == HttpStatus.SC_NO_CONTENT) {
                      /*throw new DxRuntimeException(409,QUEUE_ERROR_URN);*/
                      finalResponse.mergeIn(
                          getResponseJson(
                              HttpStatusCode.CONFLICT.getUrn(),
                              HttpStatus.SC_CONFLICT,
                              HttpStatusCode.CONFLICT.getDescription(),
                              QUEUE_ALREADY_EXISTS),
                          true);
                      promise.fail(finalResponse.toString());
                    } else if (status == HttpStatus.SC_BAD_REQUEST) {
                      finalResponse.mergeIn(
                          getResponseJson(
                              HttpStatusCode.BAD_REQUEST.getUrn(),
                              status,
                              FAILURE,
                              QUEUE_ALREADY_EXISTS_WITH_DIFFERENT_PROPERTIES),
                          true);
                      promise.fail(finalResponse.toString());
                    }
                  }
                } else {
                  LOGGER.error("Fail : Creation of Queue failed - ", ar.cause());
                  finalResponse.mergeIn(
                      getResponseJson(
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                          INTERNAL_ERROR_CODE,
                          FAILURE,
                          QUEUE_CREATE_ERROR));
                  promise.fail(finalResponse.toString());
                }
              });
    }
    return promise.future();
  }

  public Future<Void> bindQueue(
      String exchangeName, String queueName, JsonArray entities, String vhost) {
    LOGGER.trace("Info : RabbitClient#bindQueue() started");
    JsonObject finalResponse = new JsonObject();
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

      requestBody.put("routing_key", entities.getString(0));

      rabbitWebClient
          .requestAsync(REQUEST_POST, url, requestBody)
          .onComplete(
              ar -> {
                if (ar.succeeded()) {
                  HttpResponse<Buffer> response = ar.result();
                  if (response != null && !response.equals(" ")) {
                    int status = response.statusCode();
                    LOGGER.info("Info : Binding " + entities.getString(0) + " Status is " + status);
                    if (status == HttpStatus.SC_CREATED) {
                      /*finalResponse.put(EXCHANGE, exchangeName);
                      finalResponse.put(QUEUE, queueName);
                      finalResponse.put(ENTITIES, entities);
                      LOGGER.debug("Success : " + finalResponse);*/
                      promise.complete();
                    } else if (status == HttpStatus.SC_NOT_FOUND) {
                      finalResponse.mergeIn(
                          getResponseJson(
                              HttpStatusCode.NOT_FOUND.getUrn(),
                              status,
                              FAILURE,
                              QUEUE_EXCHANGE_NOT_FOUND));
                      promise.fail(finalResponse.toString());
                    }
                  }
                } else {
                  LOGGER.error("Fail : Binding of Queue failed - ", ar.cause());
                  finalResponse.mergeIn(
                      getResponseJson(
                          HttpStatusCode.INTERNAL_SERVER_ERROR.getUrn(),
                          INTERNAL_ERROR_CODE,
                          FAILURE,
                          QUEUE_BIND_ERROR));
                  promise.fail(finalResponse.toString());
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
}
