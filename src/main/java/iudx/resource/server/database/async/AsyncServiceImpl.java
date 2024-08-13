package iudx.resource.server.database.async;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.DID;
import static iudx.resource.server.apiserver.util.Constants.DRL;
import static iudx.resource.server.apiserver.util.Constants.ID;
import static iudx.resource.server.authenticator.Constants.*;
import static iudx.resource.server.cache.cachelmpl.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.common.Constants.METERING_SERVICE_ADDRESS;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.ResponseUrn.LIMIT_EXCEED_URN;
import static iudx.resource.server.common.ResponseUrn.URL_EXPIRED_URN;
import static iudx.resource.server.database.archives.Constants.*;
import static iudx.resource.server.database.async.util.Constants.*;
import static iudx.resource.server.database.async.util.Constants.STATUS;
import static iudx.resource.server.database.async.util.Constants.USER_ID;
import static iudx.resource.server.database.postgres.Constants.*;
import static iudx.resource.server.database.postgres.Constants.UPDATE_ISAUDITED_SQL;
import static iudx.resource.server.metering.util.Constants.*;
import static iudx.resource.server.metering.util.Constants.API;
import static iudx.resource.server.metering.util.Constants.TYPE_KEY;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.archives.ResponseBuilder;
import iudx.resource.server.database.async.util.QueryProgress;
import iudx.resource.server.database.async.util.S3FileOpsHelper;
import iudx.resource.server.database.async.util.Util;
import iudx.resource.server.database.elastic.ElasticClient;
import iudx.resource.server.database.elastic.QueryDecoder;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.metering.MeteringService;
import iudx.resource.server.metering.util.Constants;
import java.io.File;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AsyncServiceImpl implements AsyncService {

  private static final Logger LOGGER = LogManager.getLogger(AsyncServiceImpl.class);

  private final ElasticClient client;
  private final PostgresService pgService;
  private final S3FileOpsHelper s3FileOpsHelper;
  private final Util util;
  private final Vertx vertx;
  private final MeteringService meteringService;
  public CacheService cacheService;
  private ResponseBuilder responseBuilder;
  private String filePath;
  private String tenantPrefix;
  private QueryDecoder queryDecoder = new QueryDecoder();

  public AsyncServiceImpl(
      Vertx vertx,
      ElasticClient client,
      PostgresService pgService,
      S3FileOpsHelper s3FileOpsHelper,
      String filePath,
      String tenantPrefix,
      CacheService cacheService) {
    this.vertx = vertx;
    this.client = client;
    this.pgService = pgService;
    this.s3FileOpsHelper = s3FileOpsHelper;
    this.filePath = filePath;
    this.util = new Util(pgService);
    this.meteringService = MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);
    this.tenantPrefix = tenantPrefix;
    this.cacheService = cacheService;
  }

  @Override
  public AsyncService asyncStatus(
      JsonObject authInfo, String searchId, Handler<AsyncResult<JsonObject>> handler) {
    String sub = authInfo.getString("userid");
    authInfo.put("searchId", searchId);
    StringBuilder query = new StringBuilder(SELECT_S3_STATUS_SQL.replace("$1", searchId));
    LOGGER.debug("query: {}", query);

    pgService.executeQuery(
        query.toString(),
        pgHandler -> {
          if (pgHandler.succeeded()) {
            JsonArray results = pgHandler.result().getJsonArray("result");
            if (results.isEmpty()) {
              responseBuilder =
                  new ResponseBuilder("failed")
                      .setTypeAndTitle(400, BAD_REQUEST.getUrn())
                      .setMessage("Fail: Incorrect search ID");
              handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            } else {
              JsonObject answer = results.getJsonObject(0);
              LOGGER.debug("answer {}", answer);

              String userId = answer.getString("user_id");
              if (sub.equals(userId)) {
                String status = answer.getString(STATUS);
                if (status.equalsIgnoreCase(QueryProgress.COMPLETE.toString())) {
                  if (isExpired(answer)) {
                    responseBuilder =
                        new ResponseBuilder("failed")
                            .setTypeAndTitle(410, URL_EXPIRED_URN.getUrn())
                            .setMessage(
                                "The requested url/resource is expired and no longer available");
                    handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
                    return;
                  }
                  if (!isUsageWithinLimits(authInfo, answer.getLong("size"))) {
                    responseBuilder =
                        new ResponseBuilder("failed")
                            .setTypeAndTitle(429, LIMIT_EXCEED_URN.getUrn())
                            .setMessage(LIMIT_EXCEED_URN.getMessage());
                    handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
                    return;
                  }
                  answer.put(FILE_DOWNLOAD_URL, answer.getValue(S3_URL));
                  if (!answer.getBoolean("isaudited")) {
                    Future.future(fu -> updateAuditTable(authInfo, answer.getLong("size")));
                  }
                  answer.remove("size");
                  answer.remove("isaudited");
                } else {
                  answer.remove("expiry");
                  answer.remove("size");
                  answer.remove("isaudited");
                }
                answer.put("searchId", answer.getString("search_id"));
                answer.put("userId", userId);

                answer.remove(S3_URL);
                answer.remove("search_id");
                answer.remove(USER_ID);

                JsonObject response =
                    new JsonObject()
                        .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                        .put("title", ResponseUrn.SUCCESS_URN.getMessage());
                response.put("results", new JsonArray().add(answer));
                handler.handle(Future.succeededFuture(response));
              } else {
                responseBuilder =
                    new ResponseBuilder("failed")
                        .setTypeAndTitle(400, ResponseUrn.BAD_REQUEST_URN.getUrn())
                        .setMessage(
                            "Please use same user token to check status as "
                                + "used while calling search API");
                handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
              }
            }
          }
        });
    return this;
  }

  @Override
  public AsyncService asyncSearch(
      String requestId, String searchId, JsonObject query, String format) {
    getRecord4RequestId(requestId)
        .onSuccess(
            handler -> {
              process4ExistingRequestId(searchId, handler);
            })
        .onFailure(
            handler -> {
              updateQueryExecutionStatus(searchId, QueryProgress.IN_PROGRESS)
                  .onSuccess(
                      statusHandler -> {
                        process4NewRequestId(searchId, query, format);
                      })
                  .onFailure(
                      statusHandler -> {
                        LOGGER.error("");
                      });
            });

    return this;
  }

  private Future<Void> updateQueryExecutionStatus(String searchId, QueryProgress status) {
    Promise<Void> promise = Promise.promise();
    StringBuilder querySb =
        new StringBuilder(
            UPDATE_STATUS_SQL.replace("$1", status.toString()).replace("$2", searchId));
    pgService.executeQuery(
        querySb.toString(),
        handler -> {
          if (handler.succeeded()) {
            LOGGER.debug("status : {} update for search id : {}", status.toString(), searchId);
            promise.complete();
          } else {
            promise.fail("failprocess4ExistingRequestId to update query status in database");
          }
        });
    return promise.future();
  }

  /**
   * This method will fetch results from database for a provided requestId, and status="COMPLETE".
   * This method returns a failed future if no record exist, else it will return a successful
   *
   * @param requestId String
   * @return Future
   */
  Future<JsonArray> getRecord4RequestId(String requestId) {
    Promise<JsonArray> promise = Promise.promise();

    StringBuilder query =
        new StringBuilder(
            SELECT_S3_SEARCH_SQL
                .replace("$1", requestId)
                .replace("$2", QueryProgress.COMPLETE.toString()));

    pgService.executeQuery(
        query.toString(),
        pgHandler -> {
          if (pgHandler.succeeded()) {
            JsonArray results = pgHandler.result().getJsonArray("result");
            if (results.isEmpty()) {
              promise.fail("Record doesn't exist in db for requestId.");
            } else {
              //          LOGGER.debug("record : " + results);
              promise.complete(results);
            }
          }
        });
    return promise.future();
  }

  Future<Void> executePgQuery(String query) {
    Promise<Void> promise = Promise.promise();

    pgService.executeQuery(
        query,
        handler -> {
          if (handler.succeeded()) {
            promise.complete();
          } else {
            promise.fail("failed query execution " + handler.cause());
          }
        });

    return promise.future();
  }

  void process4ExistingRequestId(String searchId, JsonArray record) {
    String objectId = record.getJsonObject(0).getString(OBJECT_ID);
    String expiry = LocalDateTime.now().plusDays(1).toString();
    long fileSize = record.getJsonObject(0).getLong(SIZE_KEY);
    long urlExpiry = ZonedDateTime.now().toEpochSecond() * 1000 + TimeUnit.DAYS.toMillis(1);
    URL s3Url = s3FileOpsHelper.generatePreSignedUrl(urlExpiry, objectId);

    StringBuilder queryStringBuilder =
        new StringBuilder(
            UPDATE_S3_URL_SQL
                .replace("$1", s3Url.toString())
                .replace("$2", expiry)
                .replace("$3", QueryProgress.COMPLETE.toString())
                .replace("$4", objectId)
                .replace("$5", String.valueOf(100.0d))
                .replace("$6", String.valueOf(fileSize))
                .replace("$7", searchId));

    executePgQuery(queryStringBuilder.toString())
        .onSuccess(
            handler -> {
              LOGGER.info("Query completed with existing requestId & objectId");
            })
        .onFailure(
            handler -> {
              LOGGER.error("Query execution failed for insert with existing requestId & objectId");
            });
  }

  private void process4NewRequestId(String searchId, JsonObject query, String format) {
    if (format == null) {
      format = "json";
    }
    File file = new File(filePath + "/" + searchId + "." + format);
    String objectId = UUID.randomUUID().toString();

    ProgressListener progressListener = new AsyncFileScrollProgressListener(searchId, pgService);

    scrollQuery(
        file,
        query,
        searchId,
        progressListener,
        format,
        scrollHandler -> {
          if (scrollHandler.succeeded()) {
            s3FileOpsHelper.s3Upload(
                file,
                objectId,
                s3UploadHandler -> {
                  if (s3UploadHandler.succeeded()) {
                    JsonObject uploadResult = s3UploadHandler.result();
                    String s3Url = uploadResult.getString("s3_url");
                    String expiry = LocalDateTime.now().plusDays(1).toString();
                    Long fileSize = file.length();
                    // update DB for search ID and requestId;
                    progressListener.finish();
                    StringBuilder updateQuery =
                        new StringBuilder(
                            UPDATE_S3_URL_SQL
                                .replace("$1", s3Url)
                                .replace("$2", expiry)
                                .replace("$3", QueryProgress.COMPLETE.toString())
                                .replace("$4", objectId)
                                .replace("$5", String.valueOf(100.0))
                                .replace("$6", String.valueOf(fileSize))
                                .replace("$7", searchId));

                    executePgQuery(updateQuery.toString())
                        .onSuccess(
                            recordUpdateHandler -> {
                              LOGGER.debug("updated status in postgres");
                              try {
                                vertx.fileSystem().deleteBlocking(filePath + "/" + file.getName());
                              } catch (Exception ex) {
                                LOGGER.error(
                                    "File deletion operation failed for fileName : "
                                        + "{} try to delete manually to reclaim disk-space",
                                    file.getName());
                              }
                            })
                        .onFailure(
                            recordInsertFailure -> {
                              LOGGER.error(
                                  "Postgres insert failure[COMPLETE status] {}",
                                  recordInsertFailure);
                            });

                  } else {
                    LOGGER.error("File upload to S3 failed for fileName : {}", file.getName());
                    StringBuilder updateFailQuery =
                        new StringBuilder(
                            UPDATE_STATUS_SQL
                                .replace("$1", QueryProgress.ERROR.toString())
                                .replace("$2", searchId));
                    Future.future(fu -> util.writeToDb(updateFailQuery));
                  }
                });
          } else {
            LOGGER.error("Scroll API operation failed for searchId : " + searchId);
            StringBuilder updateFailQuery =
                new StringBuilder(
                    UPDATE_STATUS_SQL
                        .replace("$1", QueryProgress.ERROR.toString())
                        .replace("$2", searchId));
            Future.future(fu -> util.writeToDb(updateFailQuery));
          }
        });
  }

  public AsyncService scrollQuery(
      File file,
      JsonObject request,
      String searchId,
      ProgressListener progressListener,
      String format,
      Handler<AsyncResult<JsonObject>> handler) {

    Query query;
    request.put("search", true);
    if (!util.isValidQuery(request)) {
      responseBuilder =
          new ResponseBuilder("fail").setTypeAndTitle(400).setMessage("bad parameters");
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }
    LOGGER.info("tenant {}", tenantPrefix);
    StringBuilder tenantBuilder = new StringBuilder(tenantPrefix);
    final String searchIndex;
    String resourceGroup = request.getString("resourceGroup");
    if (!this.tenantPrefix.equals("none")) {
      searchIndex = String.valueOf(tenantBuilder.append("__").append(resourceGroup));
    } else {
      searchIndex = resourceGroup;
    }
    /*
     * Example: searchIndex =
     * iudx__datakaveri.org__b8bd3e3f39615c8ec96722131ae95056b5938f2f__rs.iudx.io__pune-env-aqm
     */
    LOGGER.info("Index name: " + searchIndex);

    try {
      query = queryDecoder.getQuery(request, true);
    } catch (Exception e) {
      LOGGER.error(e);
      e.printStackTrace();
      responseBuilder = new ResponseBuilder("fail").setTypeAndTitle(400).setMessage(e.getMessage());
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }

    LOGGER.debug("Info: index: " + searchIndex);
    LOGGER.debug("Info: Query constructed: " + query.toString());

    SourceConfig sourceFilter = queryDecoder.getSourceConfigFilters(request);
    Future<JsonObject> asyncFuture =
        client.asyncScroll(
            file, searchIndex, query, sourceFilter, searchId, progressListener, format, filePath);
    asyncFuture.onComplete(
        scrollHandler -> {
          if (scrollHandler.succeeded()) {
            handler.handle(Future.succeededFuture());
          } else {
            handler.handle(Future.failedFuture(scrollHandler.cause()));
          }
        });
    return this;
  }

  private Future<Void> updateAuditTable(JsonObject authInfo, long size) {
    LOGGER.trace("updateAuditTable() started");

    Promise<Void> promise = Promise.promise();
    JsonObject cacheRequest = createCacheRequest(authInfo);

    cacheService
        .get(cacheRequest)
        .onSuccess(
            cacheResult -> {
              JsonObject request = createRequest(authInfo, cacheResult, size);
              meteringService.insertMeteringValuesInRmq(
                  request,
                  meteringHandler -> {
                    if (meteringHandler.succeeded()) {
                      LOGGER.info("Audit message published in RMQ.");
                      Future.future(fu -> updateDb(authInfo.getString("searchId")));
                    } else {
                      LOGGER.error(
                          "Failed to publish audit message : {}",
                          meteringHandler.cause().getMessage());
                    }
                  });
            })
        .onFailure(
            failure -> {
              LOGGER.error("Failed to publish audit message : {}", failure.getMessage());
            });

    return promise.future();
  }

  private JsonObject createCacheRequest(JsonObject authInfo) {
    return new JsonObject()
        .put("type", CATALOGUE_CACHE)
        .put("key", authInfo.getString("resourceId"));
  }

  private JsonObject createRequest(JsonObject authInfo, JsonObject cacheResult, long size) {
    JsonObject request = new JsonObject();
    String resourceGroup =
        cacheResult.containsKey(RESOURCE_GROUP)
            ? cacheResult.getString(RESOURCE_GROUP)
            : cacheResult.getString(ID);
    ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    String role = authInfo.getString(ROLE);
    String drl = authInfo.getString(DRL);

    if ("delegate".equalsIgnoreCase(role) && drl != null) {
      request.put(DELEGATOR_ID, authInfo.getString(DID));
    } else {
      request.put(DELEGATOR_ID, authInfo.getString("userid"));
    }

    String type = cacheResult.containsKey(RESOURCE_GROUP) ? "RESOURCE" : "RESOURCE_GROUP";
    long time = zst.toInstant().toEpochMilli();
    String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
    String providerId = cacheResult.getString("provider");

    request
        .put(RESOURCE_GROUP, resourceGroup)
        .put(TYPE_KEY, type)
        .put(EPOCH_TIME, time)
        .put(ISO_TIME, isoTime)
        .put(Constants.USER_ID, authInfo.getValue("userid"))
        .put(ID, authInfo.getValue("resourceId"))
        .put(API, authInfo.getValue(API_ENDPOINT))
        .put(RESPONSE_SIZE, size)
        .put(PROVIDER_ID, providerId)
        .put("accessType", "async");

    return request;
  }

  private boolean isExpired(JsonObject answer) {

    String expiry = answer.getString("expiry");
    LOGGER.debug("expiry {}", expiry);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    try {
      LocalDateTime expiryDateTime = LocalDateTime.parse(expiry, formatter);
      LocalDateTime now = LocalDateTime.now();
      LOGGER.info("isExpired: " + expiryDateTime.isBefore(now));
      return expiryDateTime.isBefore(now);
    } catch (DateTimeParseException e) {
      LOGGER.error("Invalid expiry format: " + e.getMessage());
      return false;
    }
  }

  private boolean isUsageWithinLimits(JsonObject authInfo, long currentSize) {
    LOGGER.debug("isUsageWithinLimits () started");

    if (!authInfo.getString(ROLE).equalsIgnoreCase("CONSUMER")
        || authInfo.getString("accessPolicy").equalsIgnoreCase("OPEN")
        || !authInfo.getBoolean("enableLimits")) {
      return true;
    }
    boolean isUsageWithinLimits = false;
    JsonObject access = authInfo.getJsonObject("access").getJsonObject("async");
    JsonObject quotaConsumed = authInfo.getJsonObject("meteringData");
    LOGGER.trace("access " + access);
    LOGGER.debug("quotaConsumed " + quotaConsumed);

    long allowedData = access.getInteger("limit");
    long consumedData = quotaConsumed.getInteger("consumed_data");

    if (consumedData + currentSize < allowedData) {
      LOGGER.info("usage limits within defined limits");
      isUsageWithinLimits = true;
    } else {
      LOGGER.info("usage limits exceeds defined limits");
    }
    return isUsageWithinLimits;
  }

  private Future<Void> updateDb(String searchId) {
    Promise<Void> promise = Promise.promise();
    StringBuilder query = new StringBuilder(UPDATE_ISAUDITED_SQL.replace("$1", searchId));
    pgService.executeQuery(
        query.toString(),
        pgHandler -> {
          if (pgHandler.succeeded()) {
            LOGGER.info("Audit status updated in database");
          } else {
            LOGGER.error("Failed to update audit status");
          }
        });

    return promise.future();
  }
}
