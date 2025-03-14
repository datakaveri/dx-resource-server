package iudx.resource.server.apiserver.subscription.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import iudx.resource.server.apiserver.subscription.model.*;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.database.postgres.model.PostgresResultModel;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
class SubscriptionServiceImplTest {

  @Mock private PostgresService postgresService;
  @Mock private DataBrokerService dataBrokerService;
  @Mock private CacheService cacheService;

  @InjectMocks private SubscriptionServiceImpl subscriptionService;

  @Test
  void testCreateSubscription() {
    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            "user123",
            "STREAMING",
            "instance1",
            "urn:entity:1234",
            "testSubscription",
            "2025-12-31",
            "delegator1");
    JsonObject cacheResult =
        new JsonObject()
            .put("id", "urn:entity:1234")
            .put("type", new JsonArray().add("iudx:Resource"))
            .put("resourceGroup", "urn:resourceGroup:1234")
            .put("name", "Test Resource")
            .put("provider", "test provider");

    SubscriptionResponseModel subscriptionResponseModel =
        new SubscriptionResponseModel(
            "subscription123",
            "apiKey123",
            "subscriptionQueue",
            "http://test.url",
            8080,
            "vHostTest");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResult));
    when(dataBrokerService.registerStreamingSubscription(any(SubscriptionImplModel.class)))
        .thenReturn(Future.succeededFuture(subscriptionResponseModel));

    when(postgresService.executeQuery1(any()))
        .thenReturn(
            Future.succeededFuture(
                new PostgresResultModel(
                    "success",
                    "query executed",
                    new JsonArray().add(new JsonObject().put("id", "subscriptionQueue")))));

    lenient()
        .when(dataBrokerService.deleteStreamingSubscription(any(), any()))
        .thenReturn(Future.succeededFuture());

    Future<SubscriptionData> result = subscriptionService.createSubscription(postSubscriptionModel);

    result.onComplete(
        ar -> {
          if (ar.failed()) {
            System.err.println("Subscription creation failed: " + ar.cause().getMessage());
          }
          assertTrue(ar.succeeded(), "Future should succeed");
        });

    SubscriptionData subscriptionData = result.result();
    assertNotNull(subscriptionData, "Subscription data should not be null");
    assertEquals(
        "subscription123",
        subscriptionData.streamingResult().getUserId(),
        "Incorrect subscription ID");
    assertEquals(
        "urn:entity:1234", subscriptionData.cacheResult().getString("id"), "Incorrect cache ID");
  }

  @Test
  void testCreateSubscription2() {
    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            "user123",
            "STREAMING",
            "instance1",
            "urn:entity:1234",
            "testSubscription",
            "2025-12-31",
            "delegator1");
    JsonObject cacheResult =
        new JsonObject()
            .put("id", "urn:entity:1234")
            .put("type", new JsonArray().add("iudx:ResourceGroup"))
            .put("name", "Test Resource")
            .put("provider", "test provider")
            .put("resourceGroup", "urn:entity:1234");

    SubscriptionResponseModel subscriptionResponseModel =
        new SubscriptionResponseModel(
            "subscription123",
            "apiKey123",
            "subscriptionQueue",
            "http://test.url",
            8080,
            "vHostTest");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResult));
    when(dataBrokerService.registerStreamingSubscription(any(SubscriptionImplModel.class)))
        .thenReturn(Future.succeededFuture(subscriptionResponseModel));

    when(postgresService.executeQuery1(any()))
        .thenReturn(
            Future.succeededFuture(
                new PostgresResultModel(
                    "success",
                    "query executed",
                    new JsonArray().add(new JsonObject().put("id", "subscriptionQueue")))));

    lenient()
        .when(dataBrokerService.deleteStreamingSubscription(any(), any()))
        .thenReturn(Future.succeededFuture());

    Future<SubscriptionData> result = subscriptionService.createSubscription(postSubscriptionModel);

    result.onComplete(
        ar -> {
          if (ar.failed()) {
            System.err.println("Subscription creation failed: " + ar.cause().getMessage());
          }
          assertTrue(ar.succeeded(), "Future should succeed");
        });

    SubscriptionData subscriptionData = result.result();
    assertNotNull(subscriptionData, "Subscription data should not be null");
    assertEquals(
        "subscription123",
        subscriptionData.streamingResult().getUserId(),
        "Incorrect subscription ID");
    assertEquals(
        "urn:entity:1234", subscriptionData.cacheResult().getString("id"), "Incorrect cache ID");
  }

  @Test
  void testGetSubscription() {
    String subscriptionID = "sub123";
    String subType = "STREAMING";
    List<String> mockList = List.of(subscriptionID);
    GetResultModel mockResult = new GetResultModel(mockList, "postgresSuccess");

    when(postgresService.executeQuery1(any()))
        .thenReturn(
            Future.succeededFuture(
                new PostgresResultModel(
                    "success",
                    "query executed",
                    new JsonArray().add(new JsonObject().put("entity", subscriptionID)))));

    when(dataBrokerService.listStreamingSubscription(any()))
        .thenReturn(Future.succeededFuture(mockList));

    Future<GetResultModel> result = subscriptionService.getSubscription(subscriptionID, subType);

    assertTrue(result.succeeded());
    assertNotNull(result.result());
    assertEquals(mockResult.listString(), result.result().listString());
  }

  @Test
  void testDeleteSubscription() {
    String subscriptionID = "sub123";
    String subType = "STREAMING";
    String userId = "user123";

    when(postgresService.executeQuery1(any()))
        .thenReturn(
            Future.succeededFuture(
                new PostgresResultModel(
                    "success",
                    "query executed",
                    new JsonArray().add(new JsonObject().put("entity", subscriptionID)))));
    when(dataBrokerService.deleteStreamingSubscription(any(), any()))
        .thenReturn(Future.succeededFuture());

    Future<String> result =
        subscriptionService.deleteSubscription(subscriptionID, subType, userId);

    assertTrue(result.succeeded(), "Future should succeed");
    assertNotNull(result.result());
  }

  @Test
  void testGetAllSubscriptions() {
    String userID = "user123";

    when(postgresService.executeQuery1(any()))
        .thenReturn(
            Future.succeededFuture(
                new PostgresResultModel("success", "query executed", new JsonArray().add(userID))));

    Future<PostgresResultModel> result = subscriptionService.getAllSubscriptionQueueForUser(userID);

    assertTrue(result.succeeded());
    assertNotNull(result.result());
  }

  @Test
  void testAppendSubscription() {
    String subscriptionID = "sub123";
    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            "user123",
            "STREAMING",
            "instance1",
            "urn:entity:1234",
            "testSubscription",
            "2025-12-31",
            "delegator1");
    JsonObject cacheResult =
        new JsonObject()
            .put("id", "urn:entity:1234")
            .put("type", new JsonArray().add("iudx:Resource"))
            .put("resourceGroup", "urn:resourceGroup:1234")
            .put("name", "Test Resource")
            .put("provider", "test provider");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResult));
    when(postgresService.executeQuery1(any()))
        .thenReturn(
            Future.succeededFuture(
                new PostgresResultModel("success", "query executed", new JsonArray())));
    when(dataBrokerService.appendStreamingSubscription(any(), any()))
        .thenReturn(Future.succeededFuture(List.of(subscriptionID)));

    Future<GetResultModel> result =
        subscriptionService.appendSubscription(postSubscriptionModel, subscriptionID);

    assertTrue(result.succeeded());
    assertNotNull(result.result());
  }

  @Test
  void testAppendSubscription2() {
    String subscriptionID = "sub123";
    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            "user123",
            "STREAMING",
            "instance1",
            "urn:entity:1234",
            "testSubscription",
            "2025-12-31",
            "delegator1");
    JsonObject cacheResult =
        new JsonObject()
            .put("id", "urn:entity:1234")
            .put("type", new JsonArray().add("iudx:ResourceGroup"))
            .put("resourceGroup", "urn:resourceGroup:1234")
            .put("name", "Test Resource")
            .put("provider", "test provider");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResult));
    when(postgresService.executeQuery1(any()))
        .thenReturn(
            Future.succeededFuture(
                new PostgresResultModel("success", "query executed", new JsonArray())));
    when(dataBrokerService.appendStreamingSubscription(any(), any()))
        .thenReturn(Future.succeededFuture(List.of(subscriptionID)));

    Future<GetResultModel> result =
        subscriptionService.appendSubscription(postSubscriptionModel, subscriptionID);

    assertTrue(result.succeeded());
    assertNotNull(result.result());
  }

  @Test
  void testUpdateSubscription() {
    String subscriptionID = "sub123";
    String entities = "urn:entity:1234";
    String expiry = "2025-12-31";

    when(postgresService.executeQuery1(any()))
        .thenReturn(
            Future.succeededFuture(
                new PostgresResultModel(
                    "success",
                    "query executed",
                    new JsonArray().add(new JsonObject().put("entity", subscriptionID)))));

    Future<GetResultModel> result =
        subscriptionService.updateSubscription(entities, subscriptionID, expiry);

    assertTrue(result.succeeded());
    assertNotNull(result.result());
  }

  @Test
  void testUpdateSubscription_fail() {
    String subscriptionID = "sub123";
    String entities = "urn:entity:1234";
    String expiry = "2025-12-31";

    when(postgresService.executeQuery1(any()))
        .thenReturn(
            Future.succeededFuture(
                new PostgresResultModel("success", "query executed", new JsonArray())));

    Future<GetResultModel> result =
        subscriptionService.updateSubscription(entities, subscriptionID, expiry);

    assertTrue(result.failed());
    assertNotNull(result.cause());
    assertTrue(result.cause().getMessage().contains("Subscription not found"));
  }

  @Test
  void testCreateSubscriptionCacheFailure() {
    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            "user123",
            "STREAMING",
            "instance1",
            "urn:entity:1234",
            "testSubscription",
            "2025-12-31",
            "delegator1");

    when(cacheService.get(any())).thenReturn(Future.failedFuture("Cache fetch failed"));

    Future<SubscriptionData> result = subscriptionService.createSubscription(postSubscriptionModel);

    assertTrue(result.failed(), "Future should fail");
    assertEquals("Cache fetch failed", result.cause().getMessage());
  }

  @Test
  void testCreateSubscriptionDataBrokerFailure() {
    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            "user123",
            "STREAMING",
            "instance1",
            "urn:entity:1234",
            "testSubscription",
            "2025-12-31",
            "delegator1");

    JsonObject cacheResult =
        new JsonObject()
            .put("id", "urn:entity:1234")
            .put("type", new JsonArray().add("iudx:Resource"))
            .put("resourceGroup", "urn:resourceGroup:1234")
            .put("name", "Test Resource")
            .put("provider", "test provider");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResult));
    when(dataBrokerService.registerStreamingSubscription(any()))
        .thenReturn(Future.failedFuture("DataBroker failure"));

    Future<SubscriptionData> result = subscriptionService.createSubscription(postSubscriptionModel);

    assertTrue(result.failed(), "Future should fail");
    assertEquals("DataBroker failure", result.cause().getMessage());
  }

  @Test
  void testCreateSubscriptionPostgresFailure() {
    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            "user123",
            "STREAMING",
            "instance1",
            "urn:entity:1234",
            "testSubscription",
            "2025-12-31",
            "delegator1");

    JsonObject cacheResult =
        new JsonObject()
            .put("id", "urn:entity:1234")
            .put("type", new JsonArray().add("iudx:Resource"))
            .put("resourceGroup", "urn:resourceGroup:1234")
            .put("name", "Test Resource")
            .put("provider", "test provider");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResult));
    when(dataBrokerService.registerStreamingSubscription(any()))
        .thenReturn(
            Future.succeededFuture(
                new SubscriptionResponseModel(
                    "sub123", "apiKey", "queue", "http://url", 8080, "vHost")));

    when(postgresService.executeQuery1(any()))
        .thenReturn(Future.failedFuture("Postgres insert failed"));
    Future<SubscriptionData> result = subscriptionService.createSubscription(postSubscriptionModel);

    assertTrue(result.failed(), "Future should fail");
    assertEquals(
            "Postgres insert failed", result.cause().getMessage());
  }

  @Test
  void testGetSubscriptionDatabaseFailure() {
    // Mock failed response from Postgres
    AsyncResult<PostgresResultModel> failedAsyncResult = mock(AsyncResult.class);
    lenient().when(failedAsyncResult.result()).thenReturn(null); // Explicitly return null
    lenient().when(failedAsyncResult.failed()).thenReturn(true);
    lenient().when(failedAsyncResult.cause()).thenReturn(new Throwable("Database fetch error"));

    when(postgresService.executeQuery1(any()))
        .thenAnswer(
            invocation -> {
              Promise<PostgresResultModel> promise = Promise.promise();
              promise.fail("Database fetch error"); // Ensures the failure is properly handled
              return promise.future();
            });

    Future<GetResultModel> result = subscriptionService.getSubscription("", "STREAMING");

    assertTrue(result.failed());
    assertEquals("Not Found", result.cause().getMessage());
  }

  @Test
  void testDeleteSubscriptionDataBrokerFailure() {
    when(postgresService.executeQuery1(any()))
        .thenReturn(
            Future.succeededFuture(
                new PostgresResultModel(
                    "success",
                    "query executed",
                    new JsonArray().add(new JsonObject().put("entity", "sub123")))));

    when(dataBrokerService.deleteStreamingSubscription(any(), any()))
        .thenReturn(Future.failedFuture("DataBroker delete failed"));

    Future<String> result =
        subscriptionService.deleteSubscription("sub123", "STREAMING", "user123");

    assertTrue(result.failed());
    assertEquals("DataBroker delete failed", result.cause().getMessage());
  }

  @Test
  void testGetAllSubscriptionsDatabaseFailure() {
    when(postgresService.executeQuery1(any())).thenReturn(Future.failedFuture("DB error"));

    Future<PostgresResultModel> result =
        subscriptionService.getAllSubscriptionQueueForUser("user123");

    assertTrue(result.failed());
    assertEquals("Internal Server Error", result.cause().getMessage());
  }

  @Test
  void testAppendSubscriptionCacheFailure() {
    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            "user123",
            "STREAMING",
            "instance1",
            "urn:entity:1234",
            "testSubscription",
            "2025-12-31",
            "delegator1");

    when(cacheService.get(any())).thenReturn(Future.failedFuture("Cache error"));

    Future<GetResultModel> result =
        subscriptionService.appendSubscription(postSubscriptionModel, "sub123");

    assertTrue(result.failed());
    assertEquals("Cache error", result.cause().getMessage());
  }

  @Test
  void testAppendSubscriptionDataBrokerFailure() {
    PostSubscriptionModel postSubscriptionModel =
        new PostSubscriptionModel(
            "user123",
            "STREAMING",
            "instance1",
            "urn:entity:1234",
            "testSubscription",
            "2025-12-31",
            "delegator1");

    JsonObject cacheResult =
        new JsonObject()
            .put("id", "urn:entity:1234")
            .put("type", new JsonArray().add("iudx:Resource"))
            .put("resourceGroup", "urn:resourceGroup:1234")
            .put("name", "Test Resource")
            .put("provider", "test provider");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResult));
    when(dataBrokerService.appendStreamingSubscription(any(), any()))
        .thenReturn(Future.failedFuture("DataBroker append failed"));

    Future<GetResultModel> result =
        subscriptionService.appendSubscription(postSubscriptionModel, "sub123");

    assertTrue(result.failed());
    assertEquals("DataBroker append failed", result.cause().getMessage());
  }

  @Test
  void testUpdateSubscriptionDatabaseFailure() {
    when(postgresService.executeQuery1(any()))
        .thenReturn(Future.failedFuture("Database update error"));

    Future<GetResultModel> result =
        subscriptionService.updateSubscription("urn:entity:1234", "sub123", "2025-12-31");

    assertTrue(result.failed());
    assertEquals("Database update error", result.cause().getMessage());
  }
}
