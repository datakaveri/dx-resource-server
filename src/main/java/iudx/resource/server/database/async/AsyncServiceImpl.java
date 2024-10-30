package iudx.resource.server.database.async;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.ID;
import static iudx.resource.server.common.Constants.METERING_SERVICE_ADDRESS;
import static iudx.resource.server.database.archives.Constants.*;
import static iudx.resource.server.database.async.util.Constants.*;
import static iudx.resource.server.database.postgres.Constants.*;
import static iudx.resource.server.metering.util.Constants.*;
import static iudx.resource.server.metering.util.Constants.API;
import static iudx.resource.server.metering.util.Constants.TYPE_KEY;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cachelmpl.CacheType;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.archives.ResponseBuilder;
import iudx.resource.server.database.async.util.*;
import iudx.resource.server.database.elastic.ElasticClient;
import iudx.resource.server.database.elastic.QueryDecoder;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.metering.MeteringService;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
      String sub, String searchId, Handler<AsyncResult<JsonObject>> handler) {
    StringBuilder query = new StringBuilder(SELECT_S3_STATUS_SQL.replace("$1", searchId));

    pgService.executeQuery(
        query.toString(),
        pgHandler -> {
          if (pgHandler.succeeded()) {
            JsonArray results = pgHandler.result().getJsonArray("result");
            if (results.isEmpty()) {
              sendErrorResponse(handler, ResponseUrn.BAD_REQUEST_URN, "Fail: Incorrect search ID");
              return;
            }

            AsyncStatusQueryResult queryResult =
                new AsyncStatusQueryResult(results.getJsonObject(0));
            LOGGER.debug(queryResult.toJson());
            if (!sub.equals(queryResult.getUserId())) {
              sendErrorResponse(
                  handler,
                  ResponseUrn.BAD_REQUEST_URN,
                  "Please use the same user token to check status as used while calling the search API");
              return;
            }
            processQueryResult(queryResult, handler);
          }
        });
    return this;
  }

  private void processQueryResult(
      AsyncStatusQueryResult queryResult, Handler<AsyncResult<JsonObject>> handler) {
    if (queryResult.getStatus().equalsIgnoreCase(QueryProgress.COMPLETE.toString())) {
      queryResult.setFileDownloadUrl(queryResult.getFileDownloadUrl());
    } else {
      queryResult.setFileDownloadUrl(null);
    }

    queryResult.setSearchId(queryResult.getSearchId());

    AsyncStatusServiceResult response = new AsyncStatusServiceResult();
    response.setType("urn:dx:rs:success");
    response.setTitle(ResponseUrn.SUCCESS_URN.getMessage());
    response.setResult(queryResult);

    handler.handle(Future.succeededFuture(response.toJson()));
  }

  private void sendErrorResponse(
      Handler<AsyncResult<JsonObject>> handler, ResponseUrn badRequestUrn, String message) {
    responseBuilder =
        new ResponseBuilder("failed")
            .setTypeAndTitle(400, badRequestUrn.getUrn())
            .setMessage(message);
    handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
  }

  @Override
  public AsyncService asyncSearch(
      String requestId,
      String sub,
      String searchId,
      JsonObject query,
      String format,
      String role,
      String drl,
      String did) {
    String id = query.getJsonArray(ID).getString(0);
    getRecord4RequestId(requestId)
        .onSuccess(
            record -> {
              process4ExistingRequestId(id, sub, searchId, record, role, drl, did);
            })
        .onFailure(
            throwable -> {
              handleRecordNotFound(requestId, searchId, query, format, role, drl, did);
            });

    return this;
  }

  private void handleRecordNotFound(
      String requestId,
      String searchId,
      JsonObject query,
      String format,
      String role,
      String drl,
      String did) {
    updateQueryExecutionStatus(searchId, QueryProgress.IN_PROGRESS)
        .onSuccess(
            status -> {
              process4NewRequestId(searchId, requestId, query, format, role, drl, did);
            })
        .onFailure(
            throwable -> {
              LOGGER.error("Failed to update query execution status", throwable);
            });
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

  void process4ExistingRequestId(
      String id,
      String sub,
      String searchId,
      JsonArray record,
      String role,
      String drl,
      String did) {
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
              Future.future(fu -> updateAuditTable(id, sub, fileSize, role, drl, did));
            })
        .onFailure(
            handler -> {
              LOGGER.error("Query execution failed for insert with existing requestId & objectId");
            });
  }

  private void process4NewRequestId(
      String searchId,
      String userId,
      JsonObject query,
      String format,
      String role,
      String drl,
      String did) {
    if (format == null) {
      format = "json";
    }
    File file = new File(filePath + "/" + searchId + "." + format);
    String objectId = UUID.randomUUID().toString();
    String id = query.getJsonArray(ID).getString(0);

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
                              Future.future(
                                  fu -> updateAuditTable(id, userId, fileSize, role, drl, did));
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
     * iudx__6c2a05e3-fc5a-4ce9-bede-f1bde41c55e9
     */
    LOGGER.info("Index name: " + searchIndex);

    try {
      query = new QueryDecoder().getQuery(request, true);
    } catch (Exception e) {
      LOGGER.error(e);
      e.printStackTrace();
      responseBuilder = new ResponseBuilder("fail").setTypeAndTitle(400).setMessage(e.getMessage());
      handler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
      return this;
    }

    LOGGER.debug("Info: index: " + searchIndex);
    LOGGER.debug("Info: Query constructed: " + query.toString());

    String[] sourceFilters = null;
    if (request.containsKey(RESPONSE_ATTRS)) {
      JsonArray responseFilters = request.getJsonArray(RESPONSE_ATTRS);
      sourceFilters = new String[responseFilters.size()];
      for (int i = 0; i < sourceFilters.length; i++) {
        sourceFilters[i] = responseFilters.getString(i);
        LOGGER.debug(sourceFilters[i]);
      }
    }
    Future<JsonObject> asyncFuture =
        client.asyncScroll(
            file, searchIndex, query, sourceFilters, searchId, progressListener, format, filePath);
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

  private Future<Void> updateAuditTable(
      String id, String userId, long fileSize, String role, String drl, String did) {
    Promise<Void> promise = Promise.promise();
    JsonObject request = new JsonObject();
    JsonObject cacheRequests = new JsonObject();
    cacheRequests.put("type", CacheType.CATALOGUE_CACHE);
    cacheRequests.put("key", id);
    cacheService
        .get(cacheRequests)
        .onComplete(
            relHandler -> {
              if (relHandler.succeeded()) {
                JsonObject cacheResult = relHandler.result();

                String type =
                    cacheResult.containsKey(RESOURCE_GROUP) ? "RESOURCE" : "RESOURCE_GROUP";
                String resourceGroup =
                    cacheResult.containsKey(RESOURCE_GROUP)
                        ? cacheResult.getString(RESOURCE_GROUP)
                        : cacheResult.getString(ID);
                ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
                if (role.equalsIgnoreCase("delegate") && drl != null) {
                  request.put(DELEGATOR_ID, did);
                } else {
                  request.put(DELEGATOR_ID, userId);
                }
                String providerId = cacheResult.getString("provider");
                long time = zst.toInstant().toEpochMilli();
                String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
                request.put(RESOURCE_GROUP, resourceGroup);
                request.put(TYPE_KEY, type);
                request.put(EPOCH_TIME, time);
                request.put(ISO_TIME, isoTime);
                request.put(Constants.USER_ID, userId);
                request.put(ID, id);
                request.put(API, IUDX_ASYNC_SEARCH_API);
                request.put(RESPONSE_SIZE, fileSize);
                request.put(PROVIDER_ID, providerId);
                meteringService.insertMeteringValuesInRmq(
                    request,
                    handler -> {
                      if (handler.succeeded()) {
                        LOGGER.info("message published in RMQ.");
                        promise.complete();
                      } else {
                        LOGGER.error("failed to publish message in RMQ.");
                        promise.complete();
                      }
                    });
              } else {
                LOGGER.debug("Item not found and failed to call metering service");
              }
            });

    return promise.future();
  }
}
