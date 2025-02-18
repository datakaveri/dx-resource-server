package iudx.resource.server.cache.cachelmpl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.cache.service.type.UniqueAttributeCache;
import iudx.resource.server.database.postgres.service.PostgresService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.testcontainers.shaded.com.google.common.cache.Cache;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class UniqueAttributeCacheTest {


    UniqueAttributeCache uniqueAttributeCache;
    @Mock
    Vertx vertx;
    @Mock
    PostgresService postgresService;
    @Mock
    Cache cache;
    @Mock
    AsyncResult<JsonObject> asyncResult;
    @Mock
    JsonObject json;
    @Mock
    JsonArray jsonArray;
    @Mock
    Object e;


    @Test
    @DisplayName("refresh cache success")
    public void refreshCacheTest(VertxTestContext vertxTestContext){

        JsonObject jsonObject=new JsonObject();
        jsonObject.put("resource_id","dummy id");
        jsonObject.put("unique_attribute","dummy string");
        uniqueAttributeCache=new UniqueAttributeCache(vertx,postgresService);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);
        when(json.getJsonArray(any())).thenReturn(jsonArray);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString());

        uniqueAttributeCache.refreshCache();
        vertxTestContext.completeNow();



    }
}
