package iudx.resource.server.apiserver.admin.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class AdminServiceImplTest {

  private AdminServiceImpl adminService;
  private PostgresService postgresService;
  private DataBrokerService dataBrokerService;

  @BeforeEach
  void setUp() {
    postgresService = mock(PostgresService.class);
    dataBrokerService = mock(DataBrokerService.class);
    adminService = new AdminServiceImpl(postgresService, dataBrokerService);
  }

  @Test
  void testRevokedTokenRequest_Success(VertxTestContext testContext) {
    String userId = "testUser";
    when(postgresService.executeQuery(anyString())).thenReturn(Future.succeededFuture());
    when(dataBrokerService.publishMessage(any(JsonObject.class), anyString(), anyString()))
        .thenReturn(Future.succeededFuture());

    adminService
        .revokedTokenRequest(userId)
        .onComplete(
            ar -> {
              assertTrue(ar.succeeded());
              testContext.completeNow();
            });
  }

  @Test
  void testRevokedTokenRequest_Failure(VertxTestContext testContext) {
    String userId = "testUser";
    when(postgresService.executeQuery(anyString())).thenReturn(Future.failedFuture("DB error"));

    adminService
        .revokedTokenRequest(userId)
        .onComplete(
            ar -> {
              assertTrue(ar.failed());
              assertEquals("DB error", ar.cause().getMessage());
              testContext.completeNow();
            });
  }

  @Test
  void testCreateUniqueAttribute_Success(VertxTestContext testContext) {
    when(postgresService.executeQuery(anyString())).thenReturn(Future.succeededFuture());
    when(dataBrokerService.publishMessage(any(JsonObject.class), anyString(), anyString()))
        .thenReturn(Future.succeededFuture());

    adminService
        .createUniqueAttribute("id1", "attribute1")
        .onComplete(
            ar -> {
              assertTrue(ar.succeeded());
              testContext.completeNow();
            });
  }

  @Test
  void testUpdateUniqueAttribute_Failure(VertxTestContext testContext) {
    when(postgresService.executeQuery(anyString()))
        .thenReturn(Future.failedFuture("Update failed"));

    adminService
        .updateUniqueAttribute("id1", "attribute1")
        .onComplete(
            ar -> {
              assertTrue(ar.failed());
              assertEquals("Update failed", ar.cause().getMessage());
              testContext.completeNow();
            });
  }

  @Test
  void testDeleteUniqueAttribute_Success(VertxTestContext testContext) {
    when(postgresService.executePreparedQuery(anyString(), any(JsonObject.class)))
        .thenReturn(Future.succeededFuture());
    when(dataBrokerService.publishMessage(any(JsonObject.class), anyString(), anyString()))
        .thenReturn(Future.succeededFuture());

    adminService
        .deleteUniqueAttribute("id1")
        .onComplete(
            ar -> {
              assertTrue(ar.succeeded());
              testContext.completeNow();
            });
  }

  @Test
  void testCreateUniqueAttribute_Failure(VertxTestContext testContext) {
    String userId = "testUser";
    when(postgresService.executeQuery(anyString())).thenReturn(Future.failedFuture("DB error"));

    adminService
        .createUniqueAttribute(userId, "attribute1")
        .onComplete(
            ar -> {
              assertTrue(ar.failed());
              assertEquals("DB error", ar.cause().getMessage());
              testContext.completeNow();
            });
  }

  @Test
  void testDeleteUniqueAttribute_Failure(VertxTestContext testContext) {
    String id = "testId";

    // Mock the failure scenario for PostgresService
    when(postgresService.executePreparedQuery(anyString(), any(JsonObject.class)))
        .thenReturn(Future.failedFuture("Database error"));

    // Call the method under test
    adminService
        .deleteUniqueAttribute(id)
        .onComplete(
            ar -> {
              assertTrue(ar.failed()); // Verify that the future failed
              assertEquals(
                  "Database error", ar.cause().getMessage()); // Check the exact error message
              testContext.completeNow();
            });
  }

  @Test
  void testUpdateUniqueAttribute_Success(VertxTestContext testContext) {
    when(postgresService.executeQuery(anyString()))
        .thenReturn(Future.succeededFuture()); // Ensure DB query succeeds

    when(dataBrokerService.publishMessage(any(JsonObject.class), anyString(), anyString()))
        .thenReturn(Future.succeededFuture()); // Ensure RMQ publish succeeds

    adminService
        .updateUniqueAttribute("id1", "attribute1")
        .onComplete(
            ar -> {
              assertTrue(ar.succeeded(), "The method should succeed");
              testContext.completeNow();
            });
  }
}
