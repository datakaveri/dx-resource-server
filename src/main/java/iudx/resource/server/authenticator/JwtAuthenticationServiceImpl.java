package iudx.resource.server.authenticator;

import static iudx.resource.server.authenticator.Constants.*;
import static iudx.resource.server.database.archives.Constants.ITEM_TYPES;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import iudx.resource.server.authenticator.authorization.AuthorizationContextFactory;
import iudx.resource.server.authenticator.authorization.AuthorizationRequest;
import iudx.resource.server.authenticator.authorization.AuthorizationStrategy;
import iudx.resource.server.authenticator.authorization.IudxRole;
import iudx.resource.server.authenticator.authorization.JwtAuthorization;
import iudx.resource.server.authenticator.authorization.Method;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cachelmpl.CacheType;
import iudx.resource.server.common.Api;
import iudx.resource.server.metering.MeteringService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JwtAuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);
  static WebClient catWebClient;
  final JWTAuth jwtAuth;
  final String host;
  final int port;
  final String path;
  final String audience;
  final CacheService cache;
  final MeteringService meteringService;
  final Api apis;
  final String catBasePath;
  boolean isLimitsEnabled;
  String relationshipCatPath;

  JwtAuthenticationServiceImpl(
      Vertx vertx,
      final JWTAuth jwtAuth,
      final JsonObject config,
      final CacheService cacheService,
      final MeteringService meteringService,
      final Api apis) {
    this.jwtAuth = jwtAuth;
    this.audience = config.getString("audience");
    this.host = config.getString("catServerHost");
    this.port = config.getInteger("catServerPort");
    this.catBasePath = config.getString("dxCatalogueBasePath");
    this.path = catBasePath + CAT_SEARCH_PATH;
    if (config.getBoolean("enableLimits") != null && config.getBoolean("enableLimits")) {
      this.isLimitsEnabled = config.getBoolean("enableLimits");
    }
    this.apis = apis;
    WebClientOptions options = new WebClientOptions();
    options.setTrustAll(true).setVerifyHost(false).setSsl(true);
    catWebClient = WebClient.create(vertx, options);
    this.cache = cacheService;
    this.meteringService = meteringService;
  }

  private static boolean isIngestionEntitiesEndpoint(JsonObject authenticationInfo) {
    return authenticationInfo
        .getString("apiEndpoint")
        .equalsIgnoreCase("/ngsi-ld/v1/ingestion/entities");
  }

  @Override
  public AuthenticationService tokenInterospect(
      JsonObject request, JsonObject authenticationInfo, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.info("authInfo " + authenticationInfo);
    String endPoint = authenticationInfo.getString("apiEndpoint");
    String id = authenticationInfo.getString("id");
    String token = authenticationInfo.getString("token");
    String method = authenticationInfo.getString("method");

    Future<JwtData> jwtDecodeFuture = decodeJwt(token);

    boolean skipResourceIdCheck =
        endPoint.equalsIgnoreCase(apis.getSubscriptionUrl())
                && (method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("DELETE"))
            || endPoint.equalsIgnoreCase(apis.getManagementBasePath())
            || endPoint.equalsIgnoreCase(apis.getIudxConsumerAuditUrl())
            || endPoint.equalsIgnoreCase("/admin/revokeToken")
            || endPoint.equalsIgnoreCase("/admin/resourceattribute")
            || endPoint.equalsIgnoreCase(apis.getIudxProviderAuditUrl())
            || endPoint.equalsIgnoreCase(apis.getIudxAsyncStatusApi())
            || endPoint.equalsIgnoreCase(apis.getIngestionPath())
            || endPoint.equalsIgnoreCase(apis.getMonthlyOverview())
            || endPoint.equalsIgnoreCase(apis.getSummaryPath());

    LOGGER.debug("checkResourceFlag " + skipResourceIdCheck);

    ResultContainer result = new ResultContainer();

    jwtDecodeFuture
        .compose(
            decodeHandler -> {
              result.jwtData = decodeHandler;
              return isValidAudienceValue(result.jwtData);
            })
        .compose(
            audienceHandler -> {
              if (!result.jwtData.getIss().equals(result.jwtData.getSub())) {
                return isRevokedClientToken(result.jwtData);
              } else {
                return Future.succeededFuture(true);
              }
            })
        .compose(
            revokeTokenHandler -> {
              if (!skipResourceIdCheck
                  && !result.jwtData.getIss().equals(result.jwtData.getSub())) {
                return isOpenResource(id);
              } else {
                return Future.succeededFuture("OPEN");
              }
            })
        .compose(
            openResourceHandler -> {
              LOGGER.debug("isOpenResource messahe {}", openResourceHandler);
              result.isOpen = openResourceHandler.equalsIgnoreCase("OPEN");

              if (endPoint.equalsIgnoreCase(apis.getIngestionPathEntities())) {
                return isValidId(result.jwtData, id);
              }

              if (result.isOpen && checkOpenEndPoints(endPoint)) {
                JsonObject json = new JsonObject();
                json.put(JSON_USERID, result.jwtData.getSub());
                return Future.succeededFuture(true);
              } else if (!skipResourceIdCheck
                  && (!result.isOpen
                      || endPoint.equalsIgnoreCase(apis.getSubscriptionUrl())
                      || endPoint.equalsIgnoreCase(apis.getIngestionPath()))) {
                return isValidId(result.jwtData, id);
              } else {
                return Future.succeededFuture(true);
              }
            })
        .compose(
            validIdHandler -> {
              if (isIngestionEntitiesEndpoint(authenticationInfo)) {
                return getProviderUserId(authenticationInfo.getString("id"));
              } else {
                return Future.succeededFuture("");
              }
            })
        .compose(
            providerUserHandler -> {
              if (isIngestionEntitiesEndpoint(authenticationInfo)) {
                return validateProviderUser(providerUserHandler, result.jwtData);
              } else {
                return Future.succeededFuture(true);
              }
            })
        .compose(
            validProviderHandler -> {
              if (result.jwtData.getIss().equals(result.jwtData.getSub())) {
                JsonObject jsonResponse = new JsonObject();
                jsonResponse.put(JSON_USERID, result.jwtData.getSub());
                jsonResponse.put(
                    JSON_EXPIRY,
                    LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(
                                Long.parseLong(result.jwtData.getExp().toString())),
                            ZoneId.systemDefault())
                        .toString());
                jsonResponse.put(ROLE, result.jwtData.getRole());
                jsonResponse.put(DRL, result.jwtData.getDrl());
                jsonResponse.put(DID, result.jwtData.getDid());
                return Future.succeededFuture(jsonResponse);
              } else {
                return validateAccess(result.jwtData, result.isOpen, authenticationInfo);
              }
            })
        .onSuccess(
            successHandler -> {
              handler.handle(Future.succeededFuture(successHandler));
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("error : " + failureHandler.getMessage());
              handler.handle(Future.failedFuture(failureHandler.getMessage()));
            });
    return this;
  }

  Future<JwtData> decodeJwt(String jwtToken) {
    Promise<JwtData> promise = Promise.promise();
    TokenCredentials creds = new TokenCredentials(jwtToken);

    jwtAuth
        .authenticate(creds)
        .onSuccess(
            user -> {
              JwtData jwtData = new JwtData(user.principal());
              jwtData.setExp(user.get("exp"));
              jwtData.setIat(user.get("iat"));
              promise.complete(jwtData);
            })
        .onFailure(
            err -> {
              LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
              promise.fail("failed");
            });

    return promise.future();
  }

  public Future<String> isOpenResource(String id) {
    LOGGER.trace("isOpenResource() started");

    JsonObject cacheRequest = new JsonObject();
    cacheRequest.put("type", CacheType.CATALOGUE_CACHE);
    cacheRequest.put("key", id);
    Future<JsonObject> resourceIdFuture = cache.get(cacheRequest);

    Promise<String> promise = Promise.promise();
    resourceIdFuture.onComplete(
        isResourceExistHandler -> {
          if (isResourceExistHandler.failed()) {
            promise.fail("Not Found  : " + id);
            return;
          } else {
            Set<String> type =
                new HashSet<String>(isResourceExistHandler.result().getJsonArray("type").getList());
            Set<String> itemTypeSet =
                type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
            itemTypeSet.retainAll(ITEM_TYPES);
            String groupId;
            if (!itemTypeSet.contains("Resource")) {
              groupId = id;
            } else {
              groupId = isResourceExistHandler.result().getString("resourceGroup");
            }
            JsonObject resourceGroupCacheRequest = cacheRequest.copy();
            resourceGroupCacheRequest.put("key", groupId);
            Future<JsonObject> groupIdFuture = cache.get(resourceGroupCacheRequest);
            groupIdFuture.onComplete(
                groupCacheResultHandler -> {
                  if (groupCacheResultHandler.failed()) {
                    if (resourceIdFuture.result() != null
                        && resourceIdFuture.result().containsKey("accessPolicy")) {
                      String acl = resourceIdFuture.result().getString("accessPolicy");
                      promise.complete(acl);
                    } else {
                      LOGGER.error("ACL not defined in group or resource item");
                      promise.fail("ACL not defined in group or resource item");
                      return;
                    }
                  } else {
                    String acl = null;

                    JsonObject groupCacheResult = groupCacheResultHandler.result();
                    if (groupCacheResult != null && groupCacheResult.containsKey("accessPolicy")) {
                      acl = groupIdFuture.result().getString("accessPolicy");
                    }

                    JsonObject resourceCacheResult = resourceIdFuture.result();
                    if (resourceCacheResult != null
                        && resourceCacheResult.containsKey("accessPolicy")) {
                      acl = resourceIdFuture.result().getString("accessPolicy");
                    }

                    if (acl == null) {
                      LOGGER.error("ACL not defined in group or resource item");
                      promise.fail("ACL not defined in group or resource item");
                      return;
                    } else {
                      promise.complete(acl);
                    }
                  }
                });
          }
        });

    return promise.future();
  }

  public Future<JsonObject> validateAccess(
      JwtData jwtData, boolean openResource, JsonObject authInfo) {
    LOGGER.trace("validateAccess() started");
    Promise<JsonObject> promise = Promise.promise();
    String jwtId = jwtData.getIid().split(":")[1];

    if (openResource && checkOpenEndPoints(authInfo.getString("apiEndpoint"))) {
      LOGGER.info("User access is allowed.");
      JsonObject jsonResponse = new JsonObject();
      jsonResponse.put(JSON_IID, jwtId);
      jsonResponse.put(JSON_USERID, jwtData.getSub());
      jsonResponse.put(ROLE, jwtData.getRole());
      jsonResponse.put(DRL, jwtData.getDrl());
      jsonResponse.put(DID, jwtData.getDid());
      jsonResponse.put(
          JSON_EXPIRY,
          LocalDateTime.ofInstant(
                  Instant.ofEpochSecond(Long.parseLong(jwtData.getExp().toString())),
                  ZoneId.systemDefault())
              .toString());

      return Future.succeededFuture(jsonResponse);
    }

    Method method = Method.valueOf(authInfo.getString("method"));
    String api = authInfo.getString("apiEndpoint");
    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);
    IudxRole role = IudxRole.fromRole(jwtData.getRole());
    AuthorizationContextFactory authFactory =
        new AuthorizationContextFactory(isLimitsEnabled, apis);

    AuthorizationStrategy authStrategy = authFactory.create(role);
    LOGGER.info("strategy : " + authStrategy.getClass().getSimpleName());
    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    LOGGER.info("endPoint : " + authInfo.getString("apiEndpoint"));

    OffsetDateTime startDateTime = OffsetDateTime.now(ZoneId.of("Z", ZoneId.SHORT_IDS));
    OffsetDateTime endDateTime = startDateTime.withHour(00).withMinute(00).withSecond(00);

    JsonObject meteringCountRequest = new JsonObject();
    meteringCountRequest.put("timeRelation", "during");
    meteringCountRequest.put("startTime", endDateTime.toString());
    meteringCountRequest.put("endTime", startDateTime.toString());
    meteringCountRequest.put("userid", jwtData.getSub());
    meteringCountRequest.put("endPoint", "/consumer/audit");
    meteringCountRequest.put("options", "count");

    LOGGER.debug("metering request : " + meteringCountRequest);
    if (isLimitsEnabled) {
      meteringService.executeReadQuery(
          meteringCountRequest,
          meteringCountHandler -> {
            if (meteringCountHandler.succeeded()) {
              JsonObject consumedApiCount = new JsonObject();
              LOGGER.info("metering response : " + meteringCountHandler.result());
              JsonObject meteringResponse = meteringCountHandler.result();
              consumedApiCount.put(
                  "api",
                  meteringResponse.getJsonArray("results").getJsonObject(0).getInteger("total"));
              if (jwtAuthStrategy.isAuthorized(authRequest, jwtData, consumedApiCount)) {
                LOGGER.info("User access is allowed.");
                promise.complete(createValidateAccessSuccessResponse(jwtData));
              } else {
                LOGGER.error("failed - no access provided to endpoint");
                JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
                promise.fail(result.toString());
              }
            } else {
              LOGGER.error("failed to get metering response");
              String failureMessage = meteringCountHandler.cause().getMessage();
              JsonObject failureJson = new JsonObject(failureMessage);
              int failureCode = failureJson.getInteger("type");
              if (failureCode == 204) {
                JsonObject consumedApiCount = new JsonObject();
                consumedApiCount.put("api", 0);
                if (jwtAuthStrategy.isAuthorized(authRequest, jwtData, consumedApiCount)) {
                  LOGGER.info("User access is allowed.");
                  promise.complete(createValidateAccessSuccessResponse(jwtData));
                } else {
                  LOGGER.error("failed - no access provided to endpoint");
                  JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
                  promise.fail(result.toString());
                }
              }

              //          JsonObject result = new JsonObject().put("401", "Access limit exceeds");
              //          promise.fail(result.toString());
            }
          });
    } else {
      if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
        LOGGER.info("User access is allowed.");
        promise.complete(createValidateAccessSuccessResponse(jwtData));
      } else {
        LOGGER.error("failed - no access provided to endpoint");
        JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
        promise.fail(result.toString());
      }
    }
    return promise.future();
  }

  private boolean checkOpenEndPoints(String endPoint) {
    for (String item : OPEN_ENDPOINTS) {
      if (endPoint.contains(item)) {
        return true;
      }
    }
    return false;
  }

  private JsonObject createValidateAccessSuccessResponse(JwtData jwtData) {
    String jwtId = jwtData.getIid().split(":")[1];
    JsonObject jsonResponse = new JsonObject();
    jsonResponse.put(JSON_USERID, jwtData.getSub());
    jsonResponse.put(JSON_IID, jwtId);
    jsonResponse.put(ROLE, jwtData.getRole());
    jsonResponse.put(DRL, jwtData.getDrl());
    jsonResponse.put(DID, jwtData.getDid());
    jsonResponse.put(
        JSON_EXPIRY,
        LocalDateTime.ofInstant(
                Instant.ofEpochSecond(Long.parseLong(jwtData.getExp().toString())),
                ZoneId.systemDefault())
            .toString());
    return jsonResponse;
  }

  Future<Boolean> isValidAudienceValue(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();
    if (audience != null && audience.equalsIgnoreCase(jwtData.getAud())) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect audience value in jwt");
      promise.fail("Incorrect audience value in jwt");
    }
    return promise.future();
  }

  Future<Boolean> isValidId(JwtData jwtData, String id) {
    Promise<Boolean> promise = Promise.promise();
    String jwtId = jwtData.getIid().split(":")[1];
    if (id.equalsIgnoreCase(jwtId)) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect id value in jwt");
      promise.fail("Incorrect id value in jwt");
    }

    return promise.future();
  }

  Future<String> getProviderUserId(String id) {
    LOGGER.trace("getProviderUserId () started");
    relationshipCatPath = catBasePath + "/relationship";
    Promise<String> promise = Promise.promise();
    LOGGER.debug("id: " + id);
    catWebClient
        .get(port, host, relationshipCatPath)
        .addQueryParam("id", id)
        .addQueryParam("rel", "provider")
        .expect(ResponsePredicate.JSON)
        .send(
            catHandler -> {
              if (catHandler.succeeded()) {
                JsonArray response = catHandler.result().bodyAsJsonObject().getJsonArray("results");
                response.forEach(
                    json -> {
                      JsonObject res = (JsonObject) json;
                      String providerUserId = null;
                      providerUserId = res.getString("providerUserId");
                      if (providerUserId == null) {
                        providerUserId = res.getString("ownerUserId");
                        LOGGER.info(" owneruserid : " + providerUserId);
                      }
                      promise.complete(providerUserId);
                    });

              } else {
                LOGGER.error(
                    "Failed to call catalogue  while getting provider user id {}",
                    catHandler.cause().getMessage());
              }
            });

    return promise.future();
  }

  Future<Boolean> validateProviderUser(String providerUserId, JwtData jwtData) {
    LOGGER.trace("validateProviderUser() started");
    Promise<Boolean> promise = Promise.promise();
    try {
      if (jwtData.getRole().equalsIgnoreCase("delegate")) {
        if (jwtData.getDid().equalsIgnoreCase(providerUserId)) {
          LOGGER.info("success");
          promise.complete(true);
        } else {
          LOGGER.error("fail");
          promise.fail("incorrect providerUserId");
        }
      } else if (jwtData.getRole().equalsIgnoreCase("provider")) {
        if (jwtData.getSub().equalsIgnoreCase(providerUserId)) {
          LOGGER.info("success");
          promise.complete(true);
        } else {
          LOGGER.error("fail");
          promise.fail("incorrect providerUserId");
        }
      }
    } catch (Exception e) {
      LOGGER.error("exception occurred while validating provider user : " + e.getMessage());
      promise.fail("exception occurred while validating provider user");
    }
    return promise.future();
  }

  Future<Boolean> isRevokedClientToken(JwtData jwtData) {
    LOGGER.trace("isRevokedClientToken started param : " + jwtData);
    Promise<Boolean> promise = Promise.promise();
    CacheType cacheType = CacheType.REVOKED_CLIENT;
    String subId = jwtData.getSub();
    JsonObject requestJson = new JsonObject().put("type", cacheType).put("key", subId);

    Future<JsonObject> cacheCallFuture = cache.get(requestJson);
    cacheCallFuture
        .onSuccess(
            successhandler -> {
              JsonObject responseJson = successhandler;
              LOGGER.debug("responseJson : " + responseJson);
              String timestamp = responseJson.getString("value");

              LocalDateTime revokedAt = LocalDateTime.parse(timestamp);
              LocalDateTime jwtIssuedAt =
                  LocalDateTime.ofInstant(
                      Instant.ofEpochSecond(jwtData.getIat()), ZoneId.systemDefault());

              if (jwtIssuedAt.isBefore(revokedAt)) {
                LOGGER.info("jwt issued at : " + jwtIssuedAt + " revokedAt : " + revokedAt);
                LOGGER.error("Privilages for client are revoked.");
                JsonObject result = new JsonObject().put("401", "revoked token passes");
                promise.fail(result.toString());
              } else {
                promise.complete(true);
              }
            })
        .onFailure(
            failureHandler -> {
              LOGGER.info("cache call result : [fail] " + failureHandler);
              promise.complete(true);
            });

    return promise.future();
  }

  // class to contain intermeddiate data for token interospection
  final class ResultContainer {
    JwtData jwtData;
    boolean isResourceExist;
    boolean isOpen;
  }
}
