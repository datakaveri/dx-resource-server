package iudx.resource.server.authenticator;

import static iudx.resource.server.authenticator.Constants.*;
import static iudx.resource.server.common.HttpStatusCode.*;
import static iudx.resource.server.common.ResponseUrn.*;
import static iudx.resource.server.database.archives.Constants.ITEM_TYPES;
import static iudx.resource.server.metering.util.Constants.ID;

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
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.metering.MeteringService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
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
  final PostgresService postgresService;
  final Api apis;
  final String catBasePath;
  boolean isLimitsEnabled;
  String relationshipCatPath;
  JsonObject meteringData = new JsonObject();
  String accessPolicy;
  String resourceId;

  JwtAuthenticationServiceImpl(
      Vertx vertx,
      final JWTAuth jwtAuth,
      final JsonObject config,
      final CacheService cacheService,
      final MeteringService meteringService,
      final PostgresService postgresService,
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
    this.postgresService = postgresService;
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
            || endPoint.equalsIgnoreCase(apis.getIngestionPath())
            || endPoint.equalsIgnoreCase(apis.getMonthlyOverview())
            || endPoint.equalsIgnoreCase(apis.getSummaryPath());

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
                if (endPoint.matches(ASYNC_SEARCH_RGX)) {
                  return getIdFromDb(authenticationInfo)
                      .compose(
                          idHandler -> {
                            authenticationInfo.put(ID, idHandler);
                            return isOpenResource(idHandler);
                          });

                } else {
                  return isOpenResource(id);
                }
              } else {
                return Future.succeededFuture("OPEN");
              }
            })
        .compose(
            openResourceHandler -> {
              LOGGER.debug("isOpenResource: {}", openResourceHandler);
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
                return isValidId(result.jwtData, authenticationInfo.getString(ID));
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
                JsonArray accessibleAttrs = result.jwtData.getCons().getJsonArray("attrs");
                if (accessibleAttrs == null || accessibleAttrs.isEmpty()) {
                  jsonResponse.put(ACCESSIBLE_ATTRS, new JsonArray());
                } else {
                  jsonResponse.put(ACCESSIBLE_ATTRS, accessibleAttrs);
                }
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
              Response response =
                  new Response.Builder()
                      .withUrn(ResponseUrn.INVALID_TOKEN_URN.getUrn())
                      .withStatus(HttpStatus.SC_UNAUTHORIZED)
                      .withTitle(UNAUTHORIZED.getDescription())
                      .withDetail(err.getLocalizedMessage())
                      .build();
              promise.fail(response.toString());
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
            LOGGER.info(isResourceExistHandler.result());
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
                      accessPolicy = acl;
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
                      accessPolicy = acl;
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
    if (openResource && checkOpenEndPoints(authInfo.getString("apiEndpoint"))) {
      LOGGER.info("User access is allowed.");
      return Future.succeededFuture(
          createValidateAccessSuccessResponse(jwtData, authInfo.getString("apiEndpoint")));
    }

    Method method = Method.valueOf(authInfo.getString("method"));
    String api = authInfo.getString("apiEndpoint");
    AuthorizationRequest authRequest = new AuthorizationRequest(method, api);
    IudxRole role = IudxRole.fromRole(jwtData.getRole());
    AuthorizationContextFactory authFactory =
        new AuthorizationContextFactory(isLimitsEnabled, apis);

    AuthorizationStrategy authStrategy = authFactory.create(role);
    LOGGER.trace("strategy : {}", authStrategy.getClass().getSimpleName());
    JwtAuthorization jwtAuthStrategy = new JwtAuthorization(authStrategy);
    LOGGER.trace("endPoint : {}", authInfo.getString("apiEndpoint"));

    OffsetDateTime startDateTime = OffsetDateTime.now(ZoneId.of("Z", ZoneId.SHORT_IDS));
    OffsetDateTime endDateTime = startDateTime.withHour(00).withMinute(00).withSecond(00);

    JsonObject meteringCountRequest = new JsonObject();
    meteringCountRequest.put("startTime", endDateTime.toString());
    meteringCountRequest.put("endTime", startDateTime.toString());
    meteringCountRequest.put("userid", jwtData.getSub());
    meteringCountRequest.put("accessType", ACCESS_MAP.get(authInfo.getString("apiEndpoint")));
    meteringCountRequest.put("resourceId", authInfo.getValue(ID));

    LOGGER.trace("metering request : {}", meteringCountRequest);

    if (isLimitsEnabled) {

      meteringService.getConsumedData(
          meteringCountRequest,
          meteringCountHandler -> {
            if (meteringCountHandler.succeeded()) {
              JsonObject meteringResponse = meteringCountHandler.result();
              JsonObject consumedData = meteringResponse.getJsonArray("result").getJsonObject(0);
              meteringData = consumedData;
              LOGGER.info("consumedData: {}", consumedData);

              try {
                if (jwtAuthStrategy.isAuthorized(authRequest, jwtData, consumedData)) {
                  LOGGER.info("User access is allowed.");
                  promise.complete(
                      createValidateAccessSuccessResponse(
                          jwtData, authInfo.getString("apiEndpoint")));
                } else {
                  LOGGER.error("failed - no access provided to endpoint");
                  Response response =
                      new Response.Builder()
                          .withUrn(ResponseUrn.UNAUTHORIZED_ENDPOINT_URN.getUrn())
                          .withTitle(UNAUTHORIZED.getDescription())
                          .withDetail("no access provided to endpoint")
                          .build();
                  promise.fail(response.toString());
                }
              } catch (RuntimeException e) {
                LOGGER.error("Authorization error: yeh wala {}", e.getMessage());
                promise.fail(e.getMessage());
              }
            } else {
              String failureMessage = meteringCountHandler.cause().getMessage();
              LOGGER.error("failed to get metering response: {}", failureMessage);
              JsonObject result = new JsonObject().put("401", "no access provided to endpoint");
              promise.fail(result.toString());
            }
          });
    } else {
      try {
        if (jwtAuthStrategy.isAuthorized(authRequest, jwtData)) {
          LOGGER.info("User access is allowed.");
          promise.complete(
              createValidateAccessSuccessResponse(jwtData, authInfo.getString("apiEndpoint")));
        } else {
          LOGGER.error("failed - no access provided to endpoint");
          Response response =
              new Response.Builder()
                  .withUrn(ResponseUrn.UNAUTHORIZED_ENDPOINT_URN.getUrn())
                  .withStatus(HttpStatus.SC_UNAUTHORIZED)
                  .withTitle(UNAUTHORIZED.getDescription())
                  .withDetail(UNAUTHORIZED_ENDPOINT_URN.getMessage())
                  .build();
          promise.fail(response.toString());
        }
      } catch (RuntimeException e) {
        LOGGER.error("Authorization error: {}", e.getMessage());
        promise.fail(e.getMessage());
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

  private JsonObject createValidateAccessSuccessResponse(JwtData jwtData, String endPoint) {
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
    JsonArray accessibleAttrs = jwtData.getCons().getJsonArray("attrs");
    if (accessibleAttrs == null || accessibleAttrs.isEmpty()) {
      jsonResponse.put(ACCESSIBLE_ATTRS, new JsonArray());
    } else {
      jsonResponse.put(ACCESSIBLE_ATTRS, accessibleAttrs);
    }
    JsonObject access =
        jwtData.getCons() != null ? jwtData.getCons().getJsonObject("access") : null;
    jsonResponse.put("access", access);
    jsonResponse.put("meteringData", meteringData);
    jsonResponse.put("accessPolicy", accessPolicy);
    jsonResponse.put("accessType", ACCESS_MAP.get(endPoint));
    jsonResponse.put("resourceId", resourceId);
    jsonResponse.put("enableLimits", isLimitsEnabled); // for async status auditing
    LOGGER.info(jsonResponse);
    return jsonResponse;
  }

  Future<Boolean> isValidAudienceValue(JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();
    if (audience != null && audience.equalsIgnoreCase(jwtData.getAud())) {
      promise.complete(true);
    } else {
      Response response =
          new Response.Builder()
              .withUrn(ResponseUrn.INVALID_TOKEN_URN.getUrn())
              .withStatus(HttpStatus.SC_UNAUTHORIZED)
              .withTitle(UNAUTHORIZED.getDescription())
              .withDetail("Incorrect audience value in jwt")
              .build();
      promise.fail(response.toString());
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
      Response response =
          new Response.Builder()
              .withUrn(ResponseUrn.UNAUTHORIZED_RESOURCE_URN.getUrn())
              .withStatus(HttpStatus.SC_UNAUTHORIZED)
              .withTitle(UNAUTHORIZED.getDescription())
              .withDetail(UNAUTHORIZED_RESOURCE_URN.getMessage())
              .build();
      promise.fail(response.toString());
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
      } else {
        LOGGER.error("fail");
        promise.fail("invalid role");
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
              String timestamp = responseJson.getString("value");

              LocalDateTime revokedAt = LocalDateTime.parse(timestamp);
              LocalDateTime jwtIssuedAt =
                  LocalDateTime.ofInstant(
                      Instant.ofEpochSecond(jwtData.getIat()), ZoneId.systemDefault());

              if (jwtIssuedAt.isBefore(revokedAt)) {
                LOGGER.info("jwt issued at : " + jwtIssuedAt + " revokedAt : " + revokedAt);
                LOGGER.error("Privilages for client are revoked.");

                // JsonObject result = new JsonObject().put("401", "revoked token passes");
                Response response =
                    new Response.Builder()
                        .withUrn(ResponseUrn.INVALID_TOKEN_URN.getUrn())
                        .withStatus(HttpStatus.SC_UNAUTHORIZED)
                        .withTitle(UNAUTHORIZED.getDescription())
                        .withDetail("revoked token passes")
                        .build();
                promise.fail(response.toString());
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

  private Future<String> getIdFromDb(JsonObject authInfo) {
    LOGGER.info("getIdFromDb() started");
    Promise<String> promise = Promise.promise();

    String query = GET_QUERY_FROM_S3_TABLE.replace("$1", authInfo.getString("searchId"));
    LOGGER.info(query);
    postgresService.executeQuery(
        query,
        pgHandler -> {
          if (pgHandler.succeeded()) {
            JsonObject pgResult = pgHandler.result();
            LOGGER.info(pgHandler.result());
            if (pgResult.getJsonArray("result").isEmpty()) {
              LOGGER.info("No result");
              Response response =
                  new Response.Builder()
                      .withUrn(BAD_REQUEST.getUrn())
                      .withStatus(HttpStatus.SC_BAD_REQUEST)
                      .withDetail("SearchId doesn't exist")
                      .build();
              promise.fail(response.toString());
              return;
            }
            JsonObject jsonObject = pgResult.getJsonArray("result").getJsonObject(0);
            LOGGER.info(jsonObject);
            JsonObject queryObject = jsonObject.getJsonObject("query");
            JsonArray idArray = queryObject.getJsonArray("id");
            String idValue = idArray.getString(0);
            LOGGER.trace("idHandler: {}", idValue);
            resourceId = idValue;
            promise.complete(idValue);
          } else {
            LOGGER.error(pgHandler.cause().getMessage());
            promise.fail(pgHandler.cause().getMessage());
          }
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
