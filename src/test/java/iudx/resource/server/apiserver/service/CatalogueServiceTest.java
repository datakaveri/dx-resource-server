package iudx.resource.server.apiserver.service;

import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.cache.CacheService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class CatalogueServiceTest {
  JsonObject config;
  CatalogueService catalogueService;
  @Mock CacheService cache;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    config = new JsonObject();
    config.put("catServerHost", "guest");
    config.put("catServerPort", 8443);
    JsonObject jsonObject = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    JsonArray jsonArray1 = new JsonArray();
    JsonObject jsonObject1 = new JsonObject();
    jsonObject1.put("id", "abcd/abcd/abcd/abcd");
    jsonObject1.put("iudxResourceAPIs", jsonArray1);
    jsonArray.add(jsonObject1);
    jsonObject.put("results", jsonArray);
    //        CatalogueService.catWebClient = mock(WebClient.class);

    //
    // when(CatalogueService.catWebClient.get(anyInt(),anyString(),anyString())).thenReturn(httpRequest);
    //        when(httpRequest.addQueryParam(anyString(),anyString())).thenReturn(httpRequest);
    //        when(httpRequest.expect(any())).thenReturn(httpRequest);
    //        when(asyncResult.succeeded()).thenReturn(true);
    //        when(asyncResult.result()).thenReturn(httpResponse);
    //        when(httpResponse.bodyAsJsonObject()).thenReturn(jsonObject);
    //        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
    //            @Override
    //            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws
    // Throwable {
    //
    //
    // ((Handler<AsyncResult<HttpResponse<Buffer>>>)arg0.getArgument(0)).handle(asyncResult);
    //                return null;
    //            }
    //        }).when(httpRequest).send(any());
    catalogueService = new CatalogueService(cache);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test getApplicableFilters method Success [Group level resource]")
  public void testGetApplicableFiltersSuccessGroup(VertxTestContext vertxTestContext) {
    String id = "abcd/abcd/abcd";
    JsonObject cacheReply = new JsonObject();
    cacheReply.put("iudxResourceAPIs", new JsonArray());

    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject =
        new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name", "dummy_name")
            .put("resourceGroup", "5b7556b5-0779-4c47-9cf2-3f209779aa22");
    when(cache.get(any()))
        .thenReturn(Future.succeededFuture(jsonObject))
        .thenReturn(Future.succeededFuture(cacheReply))
        .thenReturn(Future.succeededFuture(new JsonObject()));
    catalogueService
        .getApplicableFilters(id)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(cache, times(3)).get(any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test getApplicableFilters method Success [Resource level]")
  public void testGetApplicableFiltersSuccessItem(VertxTestContext vertxTestContext) {
    String id = "abcd/abcd/abcd";
    JsonObject cacheReply = new JsonObject();
    cacheReply.put("iudxResourceAPIs", new JsonArray());
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject =
        new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name", "dummy_name")
            .put("resourceGroup", "5b7556b5-0779-4c47-9cf2-3f209779aa22");

    when(cache.get(any()))
        .thenReturn(Future.succeededFuture(jsonObject))
        .thenReturn(Future.succeededFuture(new JsonObject()))
        .thenReturn(Future.succeededFuture(cacheReply));

    catalogueService
        .getApplicableFilters(id)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(cache, times(3)).get(any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  //@Test
  @DisplayName("Test getApplicableFilters method fail [Resource level]")
  public void testGetApplicableFiltersFailItem(VertxTestContext vertxTestContext) {
    String id = "abcd/abcd/abcd";
    JsonObject cacheReply = new JsonObject();
    cacheReply.put("iudxResourceAPIs", new JsonArray());
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject =
        new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name", "dummy_name")
            .put("resourceGroup", "5b7556b5-0779-4c47-9cf2-3f209779aa22");
    when(cache.get(any()))
        .thenReturn(Future.succeededFuture(jsonObject))
        .thenReturn(Future.succeededFuture(new JsonObject()))
        .thenReturn(Future.failedFuture("Failed"));

    catalogueService
        .getApplicableFilters(id)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                verify(cache, times(3)).get(any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test getApplicableFilters method Failed [Resource level]")
  public void testGetApplicableFiltersFailedItem(VertxTestContext vertxTestContext) {
    String id = "abcd/abcd/abcd";
    JsonObject cacheReply = new JsonObject();
    cacheReply.put("iudxResourceAPIs", new JsonArray());
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject =
        new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name", "dummy_name")
            .put("resourceGroup", "5b7556b5-0779-4c47-9cf2-3f209779aa22");

    when(cache.get(any()))
        .thenReturn(Future.succeededFuture(jsonObject))
        .thenReturn(Future.failedFuture("failed"));

    catalogueService
        .getApplicableFilters(id)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                verify(cache, times(3)).get(any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Testing Success for isItemExist method with List of String IDs")
  public void testIsItemExistSuccess(VertxTestContext vertxTestContext) {
    List<String> idList = new ArrayList<>();
    idList.add("abcd/abcd/abcd/abcd");
    idList.add("efgh/efgh/efgh/efgh");
    idList.add("asdf/asdf/asfd/asdf");
    //        JsonObject responseJSonObject = new JsonObject();
    //        responseJSonObject.put("type","urn:dx:cat:Success");
    //        responseJSonObject.put("totalHits", 10);
    //        when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
    JsonObject cacheReply = new JsonObject();
    cacheReply.put("iudxResourceAPIs", new JsonArray());
    when(cache.get(any())).thenReturn(Future.succeededFuture(cacheReply));

    catalogueService
        .isItemExist(idList)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                // assertTrue(handler.result());
                verify(cache, times(3)).get(any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    //        verify(CatalogueService.catWebClient,times(4)).get(anyInt(),anyString(),anyString());
    //        verify(httpRequest,times(6)).addQueryParam(anyString(),anyString());
    //        verify(httpRequest,times(4)).send(any());

  }

  @Test
  @DisplayName("Testing Failure for isItemExist method with List of String IDs")
  public void testIsItemExistFailure(VertxTestContext vertxTestContext) {
    List<String> idList = new ArrayList<>();
    idList.add("abcd/abcd/abcd/abcd");
    idList.add("efgh/efgh/efgh/efgh");
    idList.add("asdf/asdf/asfd/asdf");
    //        when(asyncResult.succeeded()).thenReturn(false);

    when(cache.get(any())).thenReturn(Future.failedFuture(""));

    catalogueService
        .isItemExist(idList)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(cache, times(3)).get(any());
                vertxTestContext.failNow(handler.cause());
              } else {
                vertxTestContext.completeNow();
              }
            });
    //        verify(CatalogueService.catWebClient,times(4)).get(anyInt(),anyString(),anyString());
    //        verify(httpRequest,times(6)).addQueryParam(anyString(),anyString());
    //        verify(httpRequest,times(4)).send(any());
  }
}
