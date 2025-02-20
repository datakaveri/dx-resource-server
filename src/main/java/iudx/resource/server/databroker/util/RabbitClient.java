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
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  public static Predicate<String> isValidId =
      (id) -> {
        if (id == null) {
          return false;
        }
        Pattern allowedPattern = Pattern.compile("[^-_.//a-zA-Z0-9 ]", Pattern.CASE_INSENSITIVE);
        Matcher isInvalid = allowedPattern.matcher(id);
        return !isInvalid.find();
      };
  public static BinaryOperator<JsonArray> bindingMergeOperator =
      (key1, key2) -> {
        JsonArray mergedArray = new JsonArray();
        mergedArray.clear().addAll((JsonArray) key1).addAll((JsonArray) key2);
        return mergedArray;
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
                  LOGGER.debug("status code:" + +response.statusCode());
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

  Future<Void> bindQueue(String queue, String adaptorId, String topics, String vhost) {
    LOGGER.trace("Info : RabbitClient#bindQueue() started");
    LOGGER.debug("Info : data : " + queue + " adaptorID : " + adaptorId + " topics : " + topics);
    Promise<Void> promise = Promise.promise();
    String url =
        "/api/bindings/" + vhost + "/e/" + encodeValue(adaptorId) + "/q/" + encodeValue(queue);
    JsonObject bindRequest = new JsonObject();
    bindRequest.put("routing_key", topics);

    rabbitWebClient
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

  public Future<JsonObject> registerAdapter(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#registerAdaptor() started");
    LOGGER.debug("Request :" + request + " vhost : " + vhost);
    Promise<JsonObject> promise = Promise.promise();
    String id = request.getJsonArray("entities").getString(0); // getting first and only id
    AdaptorResultContainer requestParams = new AdaptorResultContainer();
    requestParams.vhost = vhost;
    requestParams.id = request.getString("resourceGroup");
    requestParams.resourceServer = request.getString("resourceServer");
    requestParams.userid = request.getString("userid");

    requestParams.adaptorId = id;
    requestParams.type = request.getString("types");
    if (isValidId.test(requestParams.adaptorId)) {
      if (requestParams.adaptorId != null
          && !requestParams.adaptorId.isEmpty()
          && !requestParams.adaptorId.isBlank()) {
        Future<UserResponse> userCreationFuture = createUserIfNotExist(requestParams.userid, vhost);
        userCreationFuture
            .compose(
                userCreationResult -> {
                  requestParams.apiKey =
                      userCreationResult.getPassword() /*userCreationResult.getString("apiKey")*/;
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
      rabbitWebClient
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

  Future<JsonObject> deleteExchange(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#deleteExchange() started");
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String url = "/api/exchanges/" + vhost + "/" + encodeValue(exchangeName);
      rabbitWebClient
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

  Future<JsonObject> getExchange(JsonObject request, String vhost) {
    JsonObject response = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("id");
      String url;
      url = "/api/exchanges/" + vhost + "/" + encodeValue(exchangeName);
      rabbitWebClient
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

  public Future<JsonObject> deleteAdapter(JsonObject json, String vhost) {
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
              rabbitWebClient
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

  public Future<JsonObject> listExchangeSubscribers(JsonObject request, String vhost) {
    LOGGER.trace("Info : RabbitClient#listExchangeSubscribers() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString(ID);
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
