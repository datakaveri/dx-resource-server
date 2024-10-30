package iudx.resource.server.database.async;

import static iudx.resource.server.database.archives.Constants.RESPONSE_ATTRS;
import static iudx.resource.server.metering.util.Constants.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import iudx.resource.server.cache.CacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import com.amazonaws.regions.Regions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.configuration.Configuration;
import iudx.resource.server.database.async.util.S3FileOpsHelper;
import iudx.resource.server.database.async.util.Util;
import iudx.resource.server.database.elastic.ElasticClient;
import iudx.resource.server.database.postgres.PostgresService;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class AsyncServiceTest {
  private static final Logger LOGGER = LogManager.getLogger(AsyncServiceTest.class);

  private static AsyncServiceImpl asyncService;
  private static AsyncServiceImpl asyncServiceSpy;
  private static AsyncServiceImpl asyncServiceSpy2;
  private static Configuration config;
  private static ElasticClient client;
  private static PostgresService pgService;
  private static CacheService cacheSer;
  private static S3FileOpsHelper fileOpsHelper;
  private static Util util;
  private static File file;
  private static Regions clientRegion;
  private static JsonObject asyncConfig;
  private static String databaseIP;
  private static String user;
  private static String password;
  private static String timeLimit;
  private static int databasePort;
  private static String filePath;
  private static String bucketName;
  static AsyncServiceImpl asyncService2;
  private static AsyncResult<JsonObject> asyncResult1, asyncResult2;
  static AsyncFileScrollProgressListener listener;
  private static String tenantPrefix;
  @Mock
  static PostgresService postgresService;
  @Mock
  static CacheService cacheService;
  @Mock
  Throwable throwable;
  @Mock
  JsonArray jsonArray;
  @Mock
  JsonObject jsonObject;

  @BeforeAll
  @DisplayName("Initialize vertx and deploy async verticle")
  static void init(Vertx vertx, VertxTestContext testContext) throws MalformedURLException {
    config = new Configuration();
    asyncConfig = config.configLoader(8, vertx);
    asyncConfig.put("tenantPrefix","iudx");
    timeLimit = asyncConfig.getString("timeLimit");
    filePath = asyncConfig.getString("filePath");
    tenantPrefix = asyncConfig.getString("tenantPrefix");

    file = mock(File.class);
    client = mock(ElasticClient.class);
    pgService = mock(PostgresService.class);
    fileOpsHelper = mock(S3FileOpsHelper.class);
    util = mock(Util.class);
    cacheSer = mock(CacheService.class);

    asyncService =
        new AsyncServiceImpl(vertx, client, pgService, fileOpsHelper, filePath, tenantPrefix,cacheSer);
    asyncServiceSpy = spy(asyncService);
    asyncService2 = spy(asyncService);


    asyncResult1 = mock(AsyncResult.class);
    asyncResult2 = mock(AsyncResult.class);

    URL url = new URL("https://www.example.com");
    when(fileOpsHelper.generatePreSignedUrl(anyLong(), any())).thenReturn(url);

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult1);
                return null;
              }
            })
        .when(pgService)
        .executeQuery(any(), any());

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(5)).handle(asyncResult1);
                return null;
              }
            })
        .when(client)
        .asyncScroll(any(File.class), any(), any(),any(), any(), any(), anyString(), anyString());

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult2);
                return null;
              }
            })
        .when(fileOpsHelper)
        .s3Upload(any(File.class), any(), any());

    LOGGER.info("Async Test steup complete");
    testContext.completeNow();
  }

  @Test
  @DisplayName("testing setup")
  public void shouldSucceed(VertxTestContext testContext) {
    LOGGER.info("setup test is passing");
    testContext.completeNow();
  }

  public JsonObject query() {
    JsonObject query =
        new JsonObject()
            .put(
                "id",
                new JsonArray()
                    .add(
                        "83c2e5c2-3574-4e11-9530-2b1fbdfce832"))
            .put("time", "2020-10-10T14:20:00Z")
            .put("endtime", "2020-10-20T14:20:00Z")
            .put("timerel", "during")
            .put("searchType", "temporalSearch").put("resourceGroup","83c2e5c2-3574-4e11-9530-2b1fbdfce83")
                .put(RESPONSE_ATTRS,new JsonArray().add("attrs"))
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    return query;
  }

  public JsonArray record() {
    JsonArray record = new JsonArray();
    record.add(
        new JsonObject()
            .put("_id", "4c030b19-4954-4e56-868a-c36d80a77902")
                .put("search_id", "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe")
                .put("request_id", "efb0b92cd5b50d0a75a939ffa997c6e4fccdc62414ad0177a020eec98f69144e")
                .put("status", "COMPLETE")
                .put("s3_url", "https://example.com")
                .put("expiry", "2022-03-02T16:08:38.495665")
                .put("user_id", "15c7506f-c800-48d6-adeb-0542b03947c6")
                .put("object_id", "b8a47206-364c-4580-8885-45205118db57")
                .put("size", 0).put("progress",100));

    return record;
  }

  @Test
  @DisplayName("success - async search for existing request id")
  public void successfulAsyncSearchForExistingRecordTest(VertxTestContext testContext) {

    String requestId = "682a3a42aaa1c8adadea4cc9ea16d968993fc8eee4edfc299d00bccf28117965";
    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "18cc743b-59a4-4c26-9f54-e243986ed709";
    JsonArray record = record();
    JsonObject query = query();

    doAnswer(Answer -> Future.succeededFuture(record))
        .when(asyncServiceSpy)
        .getRecord4RequestId(any());
    doAnswer(Answer -> Future.succeededFuture()).when(asyncServiceSpy).executePgQuery(any());

    JsonObject providerJson =
            new JsonObject()
                    .put("provider", "8b95ab80-2aaf-4636-a65e-7f2563d0d371")
                    .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
                    .put("resourceGroup", "dummy_resource");

    when(cacheSer.get(any())).thenReturn(Future.succeededFuture(providerJson));
    asyncServiceSpy.asyncSearch(requestId, sub, searchId, query, "csv","consumer","","");
    testContext.completeNow();
  }
  //@Test
  @DisplayName("Failure case")
  public void failureAsyncSearchForExistingRecordTest(VertxTestContext testContext) {

    String requestId = "682a3a42aaa1c8adadea4cc9ea16d968993fc8eee4edfc299d00bccf28117965";
    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "18cc743b-59a4-4c26-9f54-e243986ed709";
    JsonArray record = record();
    JsonObject query = query();

    doAnswer(Answer -> Future.failedFuture("failed"))
            .when(asyncServiceSpy)
            .getRecord4RequestId(any());

    when(asyncResult1.succeeded()).thenReturn(true);
    when(asyncResult1.result()).thenReturn(jsonObject);

    when(client.asyncScroll(any(),anyString(),any(),any(),anyString(),any(),anyString(),anyString())).thenReturn(Future.failedFuture(""));

    asyncServiceSpy.asyncSearch(requestId, sub, searchId, query, "csv","consumer","","");

    testContext.completeNow();
  }

  //@Test
  @DisplayName("success case")
  public void successfulAsyncSearchForExistingRecordTest3(VertxTestContext testContext) {

    String requestId = "682a3a42aaa1c8adadea4cc9ea16d968993fc8eee4edfc299d00bccf28117965";
    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "18cc743b-59a4-4c26-9f54-e243986ed709";
    JsonArray record = record();
    JsonObject query = query();

    doAnswer(Answer -> Future.failedFuture("failed"))
            .when(asyncServiceSpy)
            .getRecord4RequestId(any());

    when(asyncResult1.succeeded()).thenReturn(true);
    when(asyncResult1.result()).thenReturn(jsonObject);

    JsonObject jsonObject2= new JsonObject()
            .put("_id", "4c030b19-4954-4e56-868a-c36d80a77902")
            .put("search_id", "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe")
            .put("request_id", "efb0b92cd5b50d0a75a939ffa997c6e4fccdc62414ad0177a020eec98f69144e")
            .put("status", "COMPLETE")
            .put("s3_url", "https://example.com")
            .put("expiry", "2022-03-02T16:08:38.495665")
            .put("user_id", "15c7506f-c800-48d6-adeb-0542b03947c6")
            .put("object_id", "b8a47206-364c-4580-8885-45205118db57")
            .put("size", 0);
    when(asyncResult2.succeeded()).thenReturn(true);
    when(asyncResult2.result()).thenReturn(jsonObject2);
    when(client.asyncScroll(any(),anyString(),any(),any(),anyString(),any(),anyString(),anyString())).thenReturn(Future.succeededFuture(jsonObject2));

    asyncServiceSpy.asyncSearch(requestId, sub, searchId, query, "csv","consumer","","");

    verify(asyncServiceSpy, times(1)).executePgQuery(any());
    testContext.completeNow();
  }
  @Test
  @DisplayName("fail - async search for existing request id")
  public void failAsyncSearchForExistingRecordTest(VertxTestContext testContext) {
    String requestId = "682a3a42aaa1c8adadea4cc9ea16d968993fc8eee4edfc299d00bccf28117965";
    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "18cc743b-59a4-4c26-9f54-e243986ed709";
    JsonArray record = record();
    JsonObject query = query();

    doAnswer(Answer -> Future.succeededFuture(record))
        .when(asyncServiceSpy)
        .getRecord4RequestId(any());
    doAnswer(Answer -> Future.failedFuture("fail")).when(asyncServiceSpy).executePgQuery(any());

    asyncServiceSpy.asyncSearch(requestId, sub, searchId, query, "csv","consumer","","");
    testContext.completeNow();
  }

  @Test
  @DisplayName("fail download from es - async search")
  public void failDownloadForNewRequestId(Vertx vertx, VertxTestContext testContext) {
    String requestId = "efb0b92cd5b50d0a75a939ffa997c6e4fccdc62414ad0177a020eec98f69144e";
    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe";
    JsonObject query = query();

    doAnswer(Answer -> Future.failedFuture("record doesn't exist"))
        .when(asyncServiceSpy)
        .getRecord4RequestId(any());

    when(asyncResult1.succeeded()).thenReturn(false);

    asyncServiceSpy.asyncSearch(requestId, sub, searchId, query, "csv","consumer","","");
    testContext.completeNow();
  }

//  @Test
//  @Disabled
//  @DisplayName("fail upload to s3 - async search")
//  public void failUploadForNewRequestId(Vertx vertx, VertxTestContext testContext) {
//    String requestId = "efb0b92cd5b50d0a75a939ffa997c6e4fccdc62414ad0177a020eec98f69144e";
//    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
//    String searchId = "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe";
//    JsonObject query = query();
//
//    doAnswer(Answer -> Future.failedFuture("record doesn't exist"))
//        .when(asyncServiceSpy)
//        .getRecord4RequestId(any());
//
//    when(asyncResult1.succeeded()).thenReturn(true);
//    when(asyncResult2.succeeded()).thenReturn(false);
//
//    asyncServiceSpy.asyncSearch(requestId, sub, searchId, query);
//    testContext.completeNow();
//  }

  @Test
  @DisplayName("success - async status")
  public void successfulAsyncStatus(VertxTestContext testContext) {

    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe";
    JsonArray record = record();

    when(asyncResult1.succeeded()).thenReturn(true);
    when(asyncResult1.result()).thenReturn(new JsonObject().put("result", record));

    asyncService.asyncStatus(
        sub,
        searchId,
        handler -> {
          if (handler.succeeded()) {
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @DisplayName("async status fail - incorrect searchID")
  public void asyncStatusIncorrectSearchID(VertxTestContext testContext) {

    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "4b25aa92-47bb-4c91-98c0-47a1c7a51234";

    when(asyncResult1.succeeded()).thenReturn(true);
    when(asyncResult1.result()).thenReturn(new JsonObject().put("result", new JsonArray()));

    asyncService.asyncStatus(
        sub,
        searchId,
        handler -> {
          if (handler.failed()) {
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @DisplayName("async status fail - incorrect user")
  public void asyncSearchIncorrectUser(VertxTestContext testContext) {

    String sub = "15c7506f-c800-48d6-adeb-0542b0394321";
    String searchId = "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe";
    JsonArray record = record();

    when(asyncResult1.succeeded()).thenReturn(true);
    when(asyncResult1.result()).thenReturn(new JsonObject().put("result", record));

    asyncService.asyncStatus(
        sub,
        searchId,
        handler -> {
          if (handler.failed()) {
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @DisplayName("success - get record for request id")
  public void testGetRecord4RequestId(VertxTestContext testContext) {
    String requestId = "682a3a42aaa1c8adadea4cc9ea16d968993fc8eee4edfc299d00bccf28117965";
    JsonArray record = record();

    when(asyncResult1.succeeded()).thenReturn(true);
    when(asyncResult1.result()).thenReturn(new JsonObject().put("result", record));

    asyncService
        .getRecord4RequestId(requestId)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                testContext.failNow("fail");
              }
            });
  }

  @Test
  @DisplayName("success - execute pg query")
  public void testExecutePGQuery(VertxTestContext testContext) {
    String query = "SELECT * FROM s3_upload_url";

    when(asyncResult1.succeeded()).thenReturn(true);

    asyncService
        .executePgQuery(query)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                testContext.failNow("fail");
              }
            });
  }

  @Test
  @DisplayName("Test getRecord4RequestId method : Failure")
  public void testGetRecord4RequestIdFailure(VertxTestContext vertxTestContext) {
    when(asyncResult2.succeeded()).thenReturn(true);
    when(asyncResult2.result()).thenReturn(jsonObject);
    when(jsonObject.getJsonArray(anyString())).thenReturn(jsonArray);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult2);
        return null;
      }
    }).when(postgresService).executeQuery(anyString(), any());
    when(jsonArray.isEmpty()).thenReturn(true);
    asyncService2 = new AsyncServiceImpl(Vertx.vertx(), client, postgresService, fileOpsHelper,
        filePath, tenantPrefix,cacheService);
    asyncService2.getRecord4RequestId("Dummy ID").onComplete(handler -> {
      if (handler.failed()) {
        assertEquals("Record doesn't exist in db for requestId.", handler.cause().getMessage());
        vertxTestContext.completeNow();
      } else {
        vertxTestContext.failNow(handler.cause());
      }
    });

  }

  @Test
  @DisplayName("Test executePGQuery method : failure")
  public void testExecutePgQueryFailure(VertxTestContext vertxTestContext) {
    asyncService2 = new AsyncServiceImpl(Vertx.vertx(), client, postgresService, fileOpsHelper,
        filePath, tenantPrefix,cacheService);
    when(asyncResult2.succeeded()).thenReturn(false);
    when(asyncResult2.cause()).thenReturn(throwable);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult2);
        return null;
      }
    }).when(postgresService).executeQuery(anyString(), any());

    asyncService2.executePgQuery("Dummy Query").onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.failNow(handler.cause());
      } else {
        assertEquals("failed query execution throwable", handler.cause().getMessage());
        vertxTestContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("Test scrollQuery method : for Invalid Query")
  public void testScrollQueryWithInvalidQuery(VertxTestContext vertxTestContext) {
    ProgressListener progressListener = mock(ProgressListener.class);
    asyncService2 = new AsyncServiceImpl(Vertx.vertx(), client, postgresService, fileOpsHelper,
        filePath, tenantPrefix,cacheService);
    when(jsonObject.put(anyString(), anyBoolean())).thenReturn(jsonObject);
    asyncService2.scrollQuery(file, jsonObject, "Dummy SearchID", progressListener, "csv", handler -> {
      if (handler.succeeded()) {
        vertxTestContext.failNow(handler.cause());
      } else {
        assertEquals("{\"type\":400,\"detail\":\"bad parameters\"}", handler.cause().getMessage());
        vertxTestContext.completeNow();
      }
    });
  }



  @ParameterizedTest
  @ValueSource(booleans = {true,false})
  @DisplayName("Test updateProgress method : Different boolean values")
  public void testUpdateProgressFailure(boolean value,VertxTestContext vertxTestContext) {
    AsyncFileScrollProgressListener.executionCounter = mock(AsyncFileScrollProgressListener.ExecutionCounter.class);
    when(asyncResult2.succeeded()).thenReturn(value);
    lenient().when(asyncResult2.cause()).thenReturn(throwable);
    lenient().doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult2);
        return null;
      }
    }).when(postgresService).executeQuery(anyString(), any());
    listener = new AsyncFileScrollProgressListener("Dummy search ID", postgresService);
    listener.updateProgress(0.55);
    vertxTestContext.completeNow();
  }
//@Test
//@DisplayName("s3Upload upload successfully")
//public void failDownloadForNewRequestI(Vertx vertx, VertxTestContext testContext) {
//  String requestId = "efb0b92cd5b50d0a75a939ffa997c6e4fccdc62414ad0177a020eec98f69144e";
//  String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
//  String searchId = "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe";
//  JsonObject query = query();
//
//  doAnswer(Answer -> Future.failedFuture("fail"))
//          .when(asyncServiceSpy)
//          .getRecord4RequestId(any());
//
//  when(asyncResult1.succeeded()).thenReturn(true);
//  when(asyncResult1.result()).thenReturn(jsonObject);
//  when(jsonObject.getString(anyString())).thenReturn("url");
//
//
//  Mockito.doAnswer(
//                  new Answer<AsyncResult<JsonObject>>() {
//                    @SuppressWarnings("unchecked")
//                    @Override
//                    public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
//                      ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult1);
//                      return null;
//                    }
//                  })
//          .when(fileOpsHelper)
//          .s3Upload(any(), any(), any());
//
//
//  asyncServiceSpy.asyncSearch(requestId, sub, searchId, query);
//  testContext.completeNow();
//}
@Test
@DisplayName("success - async search for existing request id")
public void successfulAsyncSearchForExistingRecordTest2(VertxTestContext testContext) {

  String requestId = "682a3a42aaa1c8adadea4cc9ea16d968993fc8eee4edfc299d00bccf28117965";
  String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
  String searchId = "18cc743b-59a4-4c26-9f54-e243986ed709";
  JsonArray record = record();
  JsonObject query = query();

  doAnswer(Answer -> Future.succeededFuture(record))
          .when(asyncServiceSpy)
          .getRecord4RequestId(any());
  doAnswer(Answer -> Future.succeededFuture()).when(asyncServiceSpy).executePgQuery(any());

  JsonObject providerJson =
          new JsonObject()
                  .put("provider", "8b95ab80-2aaf-4636-a65e-7f2563d0d371")
                  .put("id", "5b7556b5-0779-4c47-9cf2-3f209779aa22")
                  .put("resourceGroup", "dummy_resource");

  when(cacheSer.get(any())).thenReturn(Future.succeededFuture(providerJson));
  asyncServiceSpy.asyncSearch(requestId, sub, searchId, query, "csv","delegate","dummy","dummy");
  testContext.completeNow();
}
}
