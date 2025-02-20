package iudx.resource.server.apiserver.subscription.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.subscription.model.*;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.database.postgres.model.PostgresResultModel;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.model.SubscriptionResponseModel;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock private PostgresService postgresService;
    @Mock private DataBrokerService dataBrokerService;
    @Mock private CacheService cacheService;

    @InjectMocks private SubscriptionServiceImpl subscriptionService;

    @Test
    void testCreateSubscription() {
        // Arrange
        PostModelSubscription postModelSubscription = new PostModelSubscription("user123", "STREAMING", "instance1", "urn:entity:1234", "testSubscription", "2025-12-31", "delegator1");
        SubscriptionImplModel subscriptionImplModel = new SubscriptionImplModel(postModelSubscription, "STREAMING", "urn:resourceGroup:1234");

        JsonObject cacheResult = new JsonObject()
                .put("id", "urn:entity:1234")
                .put("type", new JsonArray().add("iudx:Resource"))
                .put("resourceGroup", "urn:resourceGroup:1234")
                .put("name", "Test Resource")
                .put("provider", "test provider");

        SubscriptionResponseModel subscriptionResponseModel = new SubscriptionResponseModel("subscription123", "apiKey123", "subscriptionQueue", "http://test.url", 8080, "vHostTest");

        when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResult));
        when(dataBrokerService.registerStreamingSubscription(any(SubscriptionImplModel.class)))
                .thenReturn(Future.succeededFuture(subscriptionResponseModel));
        //when(postgresService.executeQuery1(any())).thenReturn(Future.succeededFuture(new PostgresResultModel("success", "query executed", new JsonArray())));
        when(postgresService.executeQuery1(any()))
                .thenReturn(Future.succeededFuture(new PostgresResultModel("success", "query executed", new JsonArray().add(new JsonObject().put("id", "subscriptionQueue")))));

        lenient().when(dataBrokerService.deleteStreamingSubscription(any(), any())).thenReturn(Future.succeededFuture());

        // Act
        Future<SubscriptionData> result = subscriptionService.createSubscription(postModelSubscription);

        result.onComplete(ar -> {
            if (ar.failed()) {
                System.err.println("Subscription creation failed: " + ar.cause().getMessage());
            }
            assertTrue(ar.succeeded(), "Future should succeed");
        });
        // Assert
        //assertTrue(result.succeeded(), "Future should succeed");
        SubscriptionData subscriptionData = result.result();
        assertNotNull(subscriptionData, "Subscription data should not be null");
        assertEquals("subscription123", subscriptionData.streamingResult().getUserId(), "Incorrect subscription ID");
        assertEquals("urn:entity:1234", subscriptionData.cacheResult().getString("id"), "Incorrect cache ID");
    }

    @Test
    void testGetSubscription() {
        String subscriptionID = "sub123";
        String subType = "STREAMING";
        List<String> mockList = Arrays.asList(subscriptionID);
        GetResultModel mockResult = new GetResultModel(mockList);

        //when(postgresService.executeQuery1(any())).thenReturn(Future.succeededFuture(new PostgresResultModel("success", "query executed", new JsonArray().add(subscriptionID))));
        when(postgresService.executeQuery1(any()))
                .thenReturn(Future.succeededFuture(
                        new PostgresResultModel("success", "query executed",
                                new JsonArray().add(new JsonObject().put("entity", subscriptionID))) // Ensure "entity" key is set
                ));

        when(dataBrokerService.listStreamingSubscription(any())).thenReturn(Future.succeededFuture(mockList));

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

        //when(postgresService.executeQuery1(any())).thenReturn(Future.succeededFuture(new PostgresResultModel("success", "query executed", new JsonArray())));
        when(postgresService.executeQuery1(any()))
                .thenReturn(Future.succeededFuture(
                        new PostgresResultModel("success", "query executed",
                                new JsonArray().add(new JsonObject().put("entity", subscriptionID))) // Ensure "entity" key is set
                ));
        when(dataBrokerService.deleteStreamingSubscription(any(), any())).thenReturn(Future.succeededFuture());

        Future<DeleteSubsResultModel> result = subscriptionService.deleteSubscription(subscriptionID, subType, userId);

        assertTrue(result.succeeded(), "Future should succeed");
        assertNotNull(result.result());
    }


    @Test
    void testGetAllSubscriptions() {
        String userID = "user123";

        when(postgresService.executeQuery1(any())).thenReturn(Future.succeededFuture(new PostgresResultModel("success", "query executed", new JsonArray().add(userID))));

        Future<PostgresResultModel> result = subscriptionService.getAllSubscriptionQueueForUser(userID);

        assertTrue(result.succeeded());
        assertNotNull(result.result());
    }

    @Test
    void testAppendSubscription() {
        String subscriptionID = "sub123";
        PostModelSubscription postModelSubscription = new PostModelSubscription("user123", "STREAMING", "instance1", "urn:entity:1234", "testSubscription", "2025-12-31", "delegator1");
        JsonObject cacheResult = new JsonObject()
                .put("id", "urn:entity:1234")
                .put("type", new JsonArray().add("iudx:Resource"))
                .put("resourceGroup", "urn:resourceGroup:1234")
                .put("name", "Test Resource")
                .put("provider", "test provider");

        when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResult));
        when(postgresService.executeQuery1(any())).thenReturn(Future.succeededFuture(new PostgresResultModel("success", "query executed", new JsonArray())));
        when(dataBrokerService.appendStreamingSubscription(any(), any())).thenReturn(Future.succeededFuture(List.of(subscriptionID)));

        Future<GetResultModel> result = subscriptionService.appendSubscription(postModelSubscription, subscriptionID);

        assertTrue(result.succeeded());
        assertNotNull(result.result());
    }

    @Test
    void testUpdateSubscription() {
        String subscriptionID = "sub123";
        String entities = "urn:entity:1234";
        String expiry = "2025-12-31";

        when(postgresService.executeQuery1(any()))
                .thenReturn(Future.succeededFuture(
                        new PostgresResultModel("success", "query executed",
                                new JsonArray().add(new JsonObject().put("entity", subscriptionID))) // Ensure "entity" key is set
                ));

        Future<GetResultModel> result = subscriptionService.updateSubscription(entities, subscriptionID, expiry);

        assertTrue(result.succeeded());
        assertNotNull(result.result());
    }
}