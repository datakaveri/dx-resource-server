package iudx.resource.server.authenticator.handler.authorization;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.JSON_DETAIL;
import static iudx.resource.server.authenticator.Constants.OPEN_ENDPOINTS;
import static iudx.resource.server.common.ResponseUrn.*;
import static iudx.resource.server.database.archives.Constants.ITEM_TYPES;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.service.CatalogueService;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cachelmpl.CacheType;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.RoutingContextHelper;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthValidationHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(AuthValidationHandler.class);
  final CacheService cache;
  private Api apis;
  private String audience;
  private CatalogueService catalogueService;

  public AuthValidationHandler(
      Api api, final CacheService cache, String audience, CatalogueService catalogueService) {
    this.apis = api;
    this.cache = cache;
    this.audience = audience;
    this.catalogueService = catalogueService;
  }

  /**
   * @param event
   */
  @Override
  public void handle(RoutingContext event) {
    String endPoint = RoutingContextHelper.getEndPoint(event);
    LOGGER.info("handle with constraints started");
    String method = RoutingContextHelper.getMethod(event);
    JwtData jwtData = RoutingContextHelper.getJwtData(event);

    boolean skipResourceIdCheck =
        endPoint.equalsIgnoreCase(apis.getSubscriptionUrl())
                && (method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("DELETE"))
            || endPoint.equalsIgnoreCase(apis.getManagementApiPath())
            || endPoint.equalsIgnoreCase(apis.getIudxConsumerAuditUrl())
            || endPoint.equalsIgnoreCase(apis.getAdminRevokeToken())
            || endPoint.equalsIgnoreCase(apis.getAdminUniqueAttributeOfResource())
            || endPoint.equalsIgnoreCase(apis.getIudxProviderAuditUrl())
            || endPoint.equalsIgnoreCase(apis.getIudxAsyncStatusApi())
            || endPoint.equalsIgnoreCase(apis.getIngestionPath())
            || endPoint.equalsIgnoreCase(apis.getMonthlyOverview())
            || endPoint.equalsIgnoreCase(apis.getSummaryPath());
    LOGGER.debug("checkResourceFlag " + skipResourceIdCheck);

    String id = RoutingContextHelper.getId(event);
    Future<Boolean> isValidAudience = isValidAudienceValue(audience, jwtData);
    isValidAudience
        .compose(
            audienceHandler -> {
              if (!skipResourceIdCheck && !jwtData.getIss().equals(jwtData.getSub())) {
                return isOpenResource(id);
              } else {
                return Future.succeededFuture("OPEN");
              }
            })
        .compose(
            openResourceHandler -> {
              LOGGER.debug("isOpenResource message {}", openResourceHandler);
              boolean isOpen = openResourceHandler.equalsIgnoreCase("OPEN");
              if (endPoint.equalsIgnoreCase(apis.getIngestionPathEntities())) {
                return isValidId(jwtData, id);
              }
              if (isOpen && checkOpenEndPoints(endPoint)) {
                return Future.succeededFuture(true);
              } else if (!skipResourceIdCheck
                  && (!isOpen
                      || endPoint.equalsIgnoreCase(apis.getSubscriptionUrl())
                      || endPoint.equalsIgnoreCase(apis.getIngestionPath()))) {
                return isValidId(jwtData, id);
              } else {
                return Future.succeededFuture(true);
              }
            })
        .compose(
            validIdHandler -> {
              if (endPoint.equalsIgnoreCase(apis.getIngestionPathEntities())) {
                return catalogueService.getProviderUserId(id);
              } else {
                return Future.succeededFuture("");
              }
            })
        .compose(
            providerUserHandler -> {
              if (endPoint.equalsIgnoreCase(apis.getIngestionPathEntities())) {
                return validateProviderUser(providerUserHandler, jwtData);
              } else {
                return Future.succeededFuture(true);
              }
            })
        .onSuccess(
            successHandler -> {
              event.next();
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("error : " + failureHandler.getMessage());
              processAuthFailure(event, failureHandler.getMessage());
            });
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

  private void processAuthFailure(RoutingContext ctx, String result) {
    if (result.contains("Not Found")) {
      LOGGER.error("Error : Item Not Found");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(404);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(RESOURCE_NOT_FOUND_URN, statusCode).toString());
    } else if (result.contains("Entity IDs do not match")
        || result.contains("Error processing the request body")) {
      LOGGER.error("Entity IDs do not match");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(400);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(BAD_REQUEST_URN, statusCode).toString());
    } else {
      LOGGER.error("Error : Authentication Failure");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(INVALID_TOKEN_URN, statusCode).toString());
    }
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }

  public Future<JwtData> validateAccess(JwtData jwtData, boolean openResource, String endpoint) {
    LOGGER.trace("validateAccess() started");
    //    Promise<JwtData> promise = Promise.promise();
    /*TODO: why is this used ? open resource and open end point*/
    if (openResource && checkOpenEndPoints(endpoint)) {
      LOGGER.info("User access is allowed.");
    }
    return Future.succeededFuture(jwtData);
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

  private boolean checkOpenEndPoints(String endPoint) {
    for (String item : OPEN_ENDPOINTS) {
      if (endPoint.contains(item)) {
        return true;
      }
    }
    return false;
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
                      String acl =
                          resourceIdFuture.result().getString("accessPolicy"); // OPEN, SECURE, PII
                      promise.complete(acl);
                    } else {
                      LOGGER.error("ACL not defined in group or resource item");
                      promise.fail("ACL not defined in group or resource item");
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
                    } else {
                      promise.complete(acl);
                    }
                  }
                });
          }
        });

    return promise.future();
  }

  Future<Boolean> isValidAudienceValue(String audience, JwtData jwtData) {
    Promise<Boolean> promise = Promise.promise();
    if (audience != null && audience.equalsIgnoreCase(jwtData.getAud())) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect audience value in jwt");
      promise.fail("Incorrect audience value in jwt");
    }
    return promise.future();
  }
}
