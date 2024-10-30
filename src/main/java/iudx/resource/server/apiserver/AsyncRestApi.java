package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.response.ResponseUtil.generateResponse;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.ENCRYPTED_DATA;
import static iudx.resource.server.apiserver.util.RequestType.ASYNC_SEARCH;
import static iudx.resource.server.apiserver.util.RequestType.ASYNC_STATUS;
import static iudx.resource.server.authenticator.Constants.ROLE;
import static iudx.resource.server.common.Constants.*;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.ResponseUrn.BACKING_SERVICE_FORMAT_URN;
import static iudx.resource.server.common.ResponseUrn.INVALID_PARAM_URN;
import static iudx.resource.server.database.archives.Constants.ITEM_TYPES;
import static iudx.resource.server.database.postgres.Constants.INSERT_S3_PENDING_SQL;

import com.google.common.hash.Hashing;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.handlers.AuthHandler;
import iudx.resource.server.apiserver.handlers.FailureHandler;
import iudx.resource.server.apiserver.handlers.ValidationHandler;
import iudx.resource.server.apiserver.query.NgsildQueryParams;
import iudx.resource.server.apiserver.query.QueryMapper;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.service.CatalogueService;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cachelmpl.CacheType;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.async.AsyncService;
import iudx.resource.server.database.async.util.QueryProgress;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import iudx.resource.server.encryption.EncryptionService;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AsyncRestApi {

  private static final Logger LOGGER = LogManager.getLogger(AsyncRestApi.class);

  private final Vertx vertx;
  private final Router router;
  private final ParamsValidator validator;
  private final CatalogueService catalogueService;
  private final PostgresService postgresService;
  private final DataBrokerService databroker;
  private final CacheService cacheService;
  private AsyncService asyncService;
  private EncryptionService encryptionService;
  private Api api;
  private int timeLimitForAsync;

  AsyncRestApi(Vertx vertx, Router router, Api api, int timeLimitForAsync) {
    this.vertx = vertx;
    this.router = router;
    this.databroker = DataBrokerService.createProxy(vertx, BROKER_SERVICE_ADDRESS);
    this.cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    this.catalogueService = new CatalogueService(cacheService);
    this.validator = new ParamsValidator(catalogueService);
    this.postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    this.encryptionService = EncryptionService.createProxy(vertx, ENCRYPTION_SERVICE_ADDRESS);
    this.api = api;
    this.timeLimitForAsync = timeLimitForAsync;
  }

  Router init() {
    FailureHandler validationsFailureHandler = new FailureHandler();

    asyncService = AsyncService.createProxy(vertx, ASYNC_SERVICE_ADDRESS);

    ValidationHandler asyncSearchValidation = new ValidationHandler(vertx, ASYNC_SEARCH);
    router
        .get(SEARCH)
        .handler(asyncSearchValidation)
        .handler(AuthHandler.create(vertx, api))
        .handler(this::handleAsyncSearchRequest)
        .failureHandler(validationsFailureHandler);

    ValidationHandler asyncStatusValidation = new ValidationHandler(vertx, ASYNC_STATUS);
    router
        .get(STATUS)
        .handler(asyncStatusValidation)
        .handler(AuthHandler.create(vertx, api))
        .handler(this::handleAsyncStatusRequest)
        .failureHandler(validationsFailureHandler);

    return this.router;
  }

  private void handleAsyncSearchRequest(RoutingContext routingContext) {
    LOGGER.trace("starting async search");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    String instanceId = request.getHeader(HEADER_HOST);
    MultiMap params = getQueryParams(routingContext, response).get();

    if (containsTemporalParams(params) && !isValidTemporalQuery(params)) {
      routingContext.fail(
          400, new DxRuntimeException(400, ResponseUrn.BAD_REQUEST_URN, "Invalid temporal query"));
      return;
    }

    if (containsGeoParams(params) && !isValidGeoQuery(params)) {
      routingContext.fail(
          400, new DxRuntimeException(400, ResponseUrn.BAD_REQUEST_URN, "Invalid geo query"));
      return;
    }

    Future<Boolean> validationResult = validator.validate(params);
    validationResult.onComplete(
        validationHandler -> {
          if (validationHandler.succeeded()) {
            NgsildQueryParams ngsildquery = new NgsildQueryParams(params);
            QueryMapper queryMapper = new QueryMapper(routingContext, timeLimitForAsync);
            JsonObject json = queryMapper.toJson(ngsildquery, true, true);
            json.put(JSON_INSTANCEID, instanceId);
            LOGGER.debug("Info: IUDX json query;" + json);
            JsonObject requestBody = new JsonObject();
            requestBody.put("ids", json.getJsonArray("id"));

            if (routingContext.request().getHeader(HEADER_RESPONSE_FILE_FORMAT) != null) {
              json.put("format", routingContext.request().getHeader(HEADER_RESPONSE_FILE_FORMAT));
            }

            Future<List<String>> filtersFuture =
                catalogueService.getApplicableFilters(json.getJsonArray("id").getString(0));
            filtersFuture.onComplete(
                filtersHandler -> {
                  if (filtersHandler.succeeded()) {
                    json.put("applicableFilters", filtersHandler.result());
                    executeAsyncUrlSearch(routingContext, json);
                  } else {
                    LOGGER.error("catalogue item/group doesn't have filters.");
                  }
                });
          } else if (validationHandler.failed()) {
            LOGGER.error("Fail: Bad request;");
            handleResponse(
                response, BAD_REQUEST, INVALID_PARAM_URN, validationHandler.cause().getMessage());
          }
        });
  }

  private void executeAsyncUrlSearch(RoutingContext routingContext, JsonObject json) {
    String sub = ((JsonObject) routingContext.data().get("authInfo")).getString(USER_ID);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    String requestId =
        Hashing.sha256().hashString(json.toString(), StandardCharsets.UTF_8).toString();

    String searchId = UUID.randomUUID().toString();
    String format = routingContext.request().getHeader(HEADER_RESPONSE_FILE_FORMAT);

    StringBuilder insertQuery =
        new StringBuilder(
            INSERT_S3_PENDING_SQL
                .replace("$1", UUID.randomUUID().toString())
                .replace("$2", searchId)
                .replace("$3", requestId)
                .replace("$4", sub)
                .replace("$5", QueryProgress.SUBMITTED.toString())
                .replace("$6", String.valueOf(0.0))
                .replace("$7", json.toString()));

    String resourceId = json.getJsonArray("id").getString(0);
    JsonObject cacheRequest = new JsonObject();
    cacheRequest.put("type", CacheType.CATALOGUE_CACHE);
    cacheRequest.put("key", resourceId);
    cacheService
        .get(cacheRequest)
        .onSuccess(
            successHandler -> {
              Set<String> type = new HashSet<String>(successHandler.getJsonArray("type").getList());
              Set<String> itemTypeSet =
                  type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
              itemTypeSet.retainAll(ITEM_TYPES);

              String groupId;
              if (!itemTypeSet.contains("Resource")) {
                groupId = resourceId;
              } else {
                groupId = successHandler.getString("resourceGroup");
              }
              json.put("resourceGroup", groupId);
              JsonObject rmqQueryMessage =
                  new JsonObject()
                      .put("searchId", searchId)
                      .put("requestId", requestId)
                      .put("user", sub)
                      .put(HEADER_RESPONSE_FILE_FORMAT, format)
                      .put("query", json)
                      .put(ROLE, authInfo.getString(ROLE))
                      .put(DRL, authInfo.getString(DRL))
                      .put(DID, authInfo.getString(DID));

              postgresService.executeQuery(
                  insertQuery.toString(),
                  pgInsertHandler -> {
                    if (pgInsertHandler.succeeded()) {
                      databroker.publishMessage(
                          rmqQueryMessage,
                          "async-query",
                          "#",
                          rmqPublishHandler -> {
                            if (rmqPublishHandler.succeeded()) {
                              JsonObject response = new JsonObject();
                              response.put(JSON_TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                              response.put(JSON_TITLE, "query submitted successfully");
                              JsonArray resultArray = new JsonArray();
                              resultArray.add(new JsonObject().put("searchId", searchId));
                              response.put("result", resultArray);
                              if (routingContext.request().getHeader(HEADER_PUBLIC_KEY) == null) {
                                handleSuccessResponse(
                                    routingContext.response(),
                                    ResponseType.Created.getCode(),
                                    response.toString());
                              } else {
                                // Encryption
                                Future<JsonObject> future =
                                    encryption(
                                        routingContext, response.getJsonArray("result").toString());
                                future.onComplete(
                                    encryptionHandler -> {
                                      if (encryptionHandler.succeeded()) {
                                        JsonObject result = encryptionHandler.result();
                                        response.put("result", result);
                                        handleSuccessResponse(
                                            routingContext.response(),
                                            ResponseType.Created.getCode(),
                                            response.encode());
                                      } else {
                                        LOGGER.error(
                                            "Encryption not completed: "
                                                + encryptionHandler.cause().getMessage());
                                        processBackendResponse(
                                            routingContext.response(),
                                            encryptionHandler.cause().getMessage());
                                      }
                                    });
                              }

                            } else {
                              LOGGER.error("message published failed", rmqPublishHandler);
                            }
                          });
                    } else {
                      LOGGER.error("message save to postgres failed", pgInsertHandler);
                    }
                  });
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("Failed to fetch groupid");
            });
  }

  private void handleAsyncStatusRequest(RoutingContext routingContext) {
    LOGGER.trace("starting async status");

    String sub = ((JsonObject) routingContext.data().get("authInfo")).getString(USER_ID);
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    String searchId = request.getParam("searchID");

    asyncService.asyncStatus(
        sub,
        searchId,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Async status success");
            if (routingContext.request().getHeader(HEADER_PUBLIC_KEY) == null) {
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), handler.result().toString());
            } else {
              // Encryption
              Future<JsonObject> future =
                  encryption(routingContext, handler.result().getJsonArray("results").toString());
              future.onComplete(
                  encryptionHandler -> {
                    if (encryptionHandler.succeeded()) {
                      JsonObject result = encryptionHandler.result();
                      handler.result().put("results", result);
                      handleSuccessResponse(
                          response, ResponseType.Ok.getCode(), handler.result().encode());
                    } else {
                      LOGGER.error(
                          "Encryption not completed: " + encryptionHandler.cause().getMessage());
                      processBackendResponse(response, encryptionHandler.cause().getMessage());
                    }
                  });
            }
          } else if (handler.failed()) {
            LOGGER.error("Fail: Async status fail");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  /**
   * Encrypts the result of API response.
   *
   * @param context Routing Context
   * @param result value of result param in the API response
   * @return encrypted data
   */
  private Future<JsonObject> encryption(RoutingContext context, String result) {
    String urlBase64Publickey = context.request().getHeader(HEADER_PUBLIC_KEY);
    Promise<JsonObject> promise = Promise.promise();

    /* get the urlbase64 public key from the header and send it for encryption */
    Future<JsonObject> future =
        encryptionService.encrypt(result, new JsonObject().put(ENCODED_KEY, urlBase64Publickey));
    future.onComplete(
        handler -> {
          if (handler.succeeded()) {
            /* get encoded cipher text */
            String encodedCipherText = handler.result().getString(ENCODED_CIPHER_TEXT);
            /* Send back the encoded cipherText to Client */
            final JsonObject jsonObject = new JsonObject();
            jsonObject.put(ENCRYPTED_DATA, encodedCipherText);
            promise.complete(jsonObject);
          } else {
            LOGGER.error("Failure in handler : " + handler.cause().getMessage());
            promise.fail("Failure in handler");
          }
        });
    return promise.future();
  }

  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  private void processBackendResponse(HttpServerResponse response, String failureMessage) {
    LOGGER.debug("Info : " + failureMessage);
    try {
      JsonObject json = new JsonObject(failureMessage);
      int type = json.getInteger(JSON_TYPE);
      HttpStatusCode status = HttpStatusCode.getByValue(type);
      String urnTitle = json.getString(JSON_TITLE);
      String detail = json.getString(JSON_DETAIL);
      ResponseUrn urn;
      if (urnTitle != null) {
        urn = ResponseUrn.fromCode(urnTitle);
      } else {
        urn = ResponseUrn.fromCode(type + "");
      }
      // return urn in body
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(type)
          .end(generateResponse(status, urn, detail).toString());
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      handleResponse(response, BAD_REQUEST, BACKING_SERVICE_FORMAT_URN);
    }
  }

  private Optional<MultiMap> getQueryParams(
      RoutingContext routingContext, HttpServerResponse response) {
    MultiMap queryParams = null;
    try {
      queryParams = MultiMap.caseInsensitiveMultiMap();
      // Internally + sign is dropped and treated as space, replacing + with %2B do the trick
      String uri = routingContext.request().uri().replaceAll("\\+", "%2B");
      Map<String, List<String>> decodedParams =
          new QueryStringDecoder(uri, HttpConstants.DEFAULT_CHARSET, true, 1024, true).parameters();
      for (Map.Entry<String, List<String>> entry : decodedParams.entrySet()) {
        LOGGER.debug("Info: param :" + entry.getKey() + " value : " + entry.getValue());
        queryParams.add(entry.getKey(), entry.getValue());
      }
    } catch (IllegalArgumentException ex) {
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(BAD_REQUEST.getValue())
          .end(generateResponse(BAD_REQUEST, INVALID_PARAM_URN).toString());
    }
    return Optional.of(queryParams);
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode code, ResponseUrn urn) {
    handleResponse(response, code, urn, code.getDescription());
  }

  private void handleResponse(
      HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn, String message) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, message).toString());
  }

  private boolean isValidTemporalQuery(MultiMap params) {
    return params.contains(JSON_TIMEREL)
        && params.contains(JSON_TIME)
        && params.contains(JSON_ENDTIME);
  }

  private boolean containsTemporalParams(MultiMap params) {
    return params.contains(JSON_TIMEREL)
        || params.contains(JSON_TIME)
        || params.contains(JSON_ENDTIME);
  }

  private boolean isValidGeoQuery(MultiMap params) {
    return params.contains(JSON_GEOPROPERTY)
        && params.contains(JSON_GEOREL)
        && params.contains(JSON_GEOMETRY)
        && params.contains(JSON_COORDINATES);
  }

  private boolean containsGeoParams(MultiMap params) {
    return params.contains(JSON_GEOPROPERTY)
        || params.contains(JSON_GEOREL)
        || params.contains(JSON_GEOMETRY)
        || params.contains(JSON_COORDINATES);
  }
}
