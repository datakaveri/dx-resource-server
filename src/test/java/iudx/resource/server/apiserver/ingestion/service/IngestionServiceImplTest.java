package iudx.resource.server.apiserver.ingestion.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.ingestion.model.*;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.database.postgres.model.PostgresResultModel;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.model.ExchangeSubscribersResponse;
import iudx.resource.server.databroker.model.IngestionResponseModel;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IngestionServiceImplTest {

  @Mock private CacheService cacheService;

  @Mock private DataBrokerService dataBrokerService;

  @Mock private PostgresService postgresService;

  @InjectMocks private IngestionServiceImpl ingestionService;

  @BeforeEach
  void setUp() {
    ingestionService = new IngestionServiceImpl(cacheService, dataBrokerService, postgresService);
  }

  @Test
  void testRegisterAdapterSuccess() {
    JsonObject cacheResponse =
        new JsonObject()
            .put("id", "resource-id")
            .put("type", new JsonArray().add("iudx:Resource"))
            .put("resourceGroup", "group-id")
            .put("name", "dataset-name")
            .put("provider", "provider-id");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResponse));
    when(postgresService.executeQuery(anyString())).thenReturn(Future.succeededFuture());
    when(dataBrokerService.registerAdaptor(any()))
        .thenReturn(Future.succeededFuture(new IngestionResponseModel()));

    ingestionService
        .registerAdapter("entities", "instanceId", "userId")
        .onComplete(
            result -> {
              assert result.succeeded();
              assert result.result() != null;
            });
  }

  @Test
  void testRegisterAdapterSuccess2() {
    JsonObject cacheResponse =
        new JsonObject()
            .put("id", "resource-id")
            .put("type", new JsonArray().add("iudx:ResourceGroup"))
            .put("resourceGroup", "group-id")
            .put("name", "dataset-name")
            .put("provider", "provider-id");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResponse));
    when(postgresService.executeQuery(anyString())).thenReturn(Future.succeededFuture());
    when(dataBrokerService.registerAdaptor(any()))
        .thenReturn(Future.succeededFuture(new IngestionResponseModel()));

    ingestionService
        .registerAdapter("entities", "instanceId", "userId")
        .onComplete(
            result -> {
              assert result.succeeded();
              assert result.result() != null;
            });
  }

  @Test
  void testDeleteAdapterSuccess() {
    when(dataBrokerService.deleteAdaptor(anyString(), anyString()))
        .thenReturn(Future.succeededFuture());
    when(postgresService.executeQuery(anyString())).thenReturn(Future.succeededFuture());

    ingestionService
        .deleteAdapter("adapterId", "userId")
        .onComplete(
            result -> {
              assert result.succeeded();
            });
  }

  @Test
  void testGetAdapterDetailsSuccess() {
    when(dataBrokerService.listAdaptor(anyString()))
        .thenReturn(Future.succeededFuture(new ExchangeSubscribersResponse()));

    ingestionService
        .getAdapterDetails("adapterId")
        .onComplete(
            result -> {
              assert result.succeeded();
              assert result.result() != null;
            });
  }

  @Test
  void testPublishDataFromAdapterSuccess() {
    JsonObject cacheResponse = new JsonObject().put("resourceGroup", "group-id");
    JsonArray requestData =
        new JsonArray().add(new JsonObject().put("entities", new JsonArray().add("entityId")));

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResponse));
    when(dataBrokerService.publishFromAdaptor(anyString(), anyString(), any()))
        .thenReturn(Future.succeededFuture("success"));

    ingestionService
        .publishDataFromAdapter(requestData)
        .onComplete(
            result -> {
              assert result.succeeded();
              assert "Item Published".equals(result.result().details());
            });
  }

  @Test
  void testGetAllAdapterDetailsForUserSuccess() {
    JsonObject cacheResponse = new JsonObject().put("provider", "provider-id");
    PostgresResultModel postgresResult = new PostgresResultModel();

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResponse));
    when(postgresService.executeQuery1(anyString()))
        .thenReturn(Future.succeededFuture(postgresResult));

    ingestionService
        .getAllAdapterDetailsForUser("instanceId")
        .onComplete(
            result -> {
              assert result.succeeded();
              assert result.result() != null;
            });
  }

  @Test
  void testRegisterAdapterFailure_CacheServiceFails() {
    when(cacheService.get(any())).thenReturn(Future.failedFuture("Cache service failure"));

    ingestionService
        .registerAdapter("entities", "instanceId", "userId")
        .onComplete(
            result -> {
              assert result.failed();
              assertEquals("Cache service failure", result.cause().getMessage());
            });
  }

  @Test
  void testDeleteAdapterFailure_DataBrokerFails() {
    when(dataBrokerService.deleteAdaptor(anyString(), anyString()))
        .thenReturn(Future.failedFuture("Broker failure"));

    ingestionService
        .deleteAdapter("adapterId", "userId")
        .onComplete(
            result -> {
              assert result.failed();
              assertEquals("Broker failure", result.cause().getMessage());
            });
  }

  @Test
  void testGetAdapterDetailsFailure() {
    when(dataBrokerService.listAdaptor(anyString()))
        .thenReturn(Future.failedFuture("Adapter not found"));

    ingestionService
        .getAdapterDetails("adapterId")
        .onComplete(
            result -> {
              assert result.failed();
              assertEquals("Adapter not found", result.cause().getMessage());
            });
  }

  @Test
  void testPublishDataFromAdapterFailure_CacheFails() {
    when(cacheService.get(any())).thenReturn(Future.failedFuture("Cache lookup failed"));
    JsonArray requestData =
        new JsonArray().add(new JsonObject().put("entities", new JsonArray().add("entityId")));

    ingestionService
        .publishDataFromAdapter(requestData)
        .onComplete(
            result -> {
              assert result.failed();
            });
  }

  @Test
  void testPublishDataFromAdapterFailure_DataBrokerFails() {
    JsonObject cacheResponse = new JsonObject().put("resourceGroup", "group-id");
    JsonArray requestData =
        new JsonArray().add(new JsonObject().put("entities", new JsonArray().add("entityId")));
    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResponse));
    when(dataBrokerService.publishFromAdaptor(anyString(), anyString(), any()))
        .thenReturn(Future.failedFuture("Publish error"));

    ingestionService
        .publishDataFromAdapter(requestData)
        .onComplete(
            result -> {
              assert result.failed();
              assertEquals("Internal Server Error", result.cause().getMessage());
            });
  }

  @Test
  void testGetAllAdapterDetailsForUserFailure() {
    when(cacheService.get(any())).thenReturn(Future.failedFuture("Cache error"));

    ingestionService
        .getAllAdapterDetailsForUser("instanceId")
        .onComplete(
            result -> {
              assert result.failed();
              assertEquals("Internal Server Error", result.cause().getMessage());
            });
  }

  @Test
  void testRegisterAdapterFailure_BrokerFailsAndRollbackSucceeds() {
    JsonObject cacheResponse =
        new JsonObject()
            .put("id", "resource-id")
            .put("type", new JsonArray().add("iudx:Resource"))
            .put("resourceGroup", "group-id")
            .put("name", "dataset-name")
            .put("provider", "provider-id");

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(cacheResponse));

    when(dataBrokerService.registerAdaptor(any()))
        .thenReturn(Future.failedFuture("Broker failure"));
    lenient()
        .when(postgresService.executeQuery(any()))
        .thenReturn(Future.succeededFuture(new JsonObject()));

    ingestionService
        .registerAdapter("entities", "instanceId", "userId")
        .onComplete(
            result -> {
              assertTrue(result.failed());
              assertEquals("Broker failure", result.cause().getMessage());
              verify(postgresService, times(2)).executeQuery(any());
            });
  }
}
