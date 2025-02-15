package iudx.resource.server.apiserver;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.common.CatalogueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.List;

import static iudx.resource.server.apiserver.util.Constants.MSG_BAD_QUERY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class ParamsValidatorTest {

    ParamsValidator paramsValidator;
    @Mock
    MultiMap paramsMap;
    @Mock
    Future<List<String>> listFuture;
    @Mock
    AsyncResult<List<String>> listAsyncResult;
    @Mock
    List<String> stringList;
    @Mock
    CatalogueService catalogueService;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        paramsValidator = new ParamsValidator(catalogueService);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test validate method : Success")
    public void test_validate(VertxTestContext vertxTestContext) {

        when(paramsMap.get(anyString())).thenReturn("Dummy/string/value", null);
        when(catalogueService.getApplicableFilters(anyString())).thenReturn(listFuture);
        when(listFuture.result()).thenReturn(stringList);
        when(listAsyncResult.succeeded()).thenReturn(true);
        when(stringList.contains(anyString())).thenReturn(true);
        when(paramsMap.contains(anyString())).thenReturn(true);
        doAnswer(new Answer<AsyncResult<List<String>>>() {
            @Override
            public AsyncResult<List<String>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<List<String>>>) arg0.getArgument(0)).handle(listAsyncResult);
                return null;
            }
        }).when(listFuture).onComplete(any());
        paramsValidator.validate(paramsMap).onComplete(handler -> {
            if (handler.succeeded()) {
                assertTrue(handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEMPORAL", "SPATIAL", "ATTR"})
    @DisplayName("Test isValidQueryWithFilters method : with different filters")
    public void test_isValidQueryWithFilters(String value, VertxTestContext vertxTestContext) {

        when(paramsMap.get(anyString())).thenReturn("Dummy/string/value", null);
        when(catalogueService.getApplicableFilters(anyString())).thenReturn(listFuture);
        when(listFuture.result()).thenReturn(stringList);
        when(listAsyncResult.succeeded()).thenReturn(true);
        if (value.equals("SPATIAL")) {
            when(stringList.contains("TEMPORAL")).thenReturn(true);
        } else {
            when(stringList.contains("TEMPORAL")).thenReturn(true);
            lenient().when(stringList.contains("SPATIAL")).thenReturn(true);

        }
        when(stringList.contains(value)).thenReturn(false);
        when(paramsMap.contains(anyString())).thenReturn(true);
        doAnswer(new Answer<AsyncResult<List<String>>>() {
            @Override
            public AsyncResult<List<String>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<List<String>>>) arg0.getArgument(0)).handle(listAsyncResult);
                return null;
            }
        }).when(listFuture).onComplete(any());
        paramsValidator.validate(paramsMap).onComplete(handler -> {
            if (handler.failed()) {
                switch (value) {
                    case "TEMPORAL":
                        assertEquals("Temporal parameters are not supported by RS group/Item.", handler.cause().getMessage());
                        break;
                    case "SPATIAL":
                        assertEquals("Spatial parameters are not supported by RS group/Item.", handler.cause().getMessage());
                        break;
                    default:
                        assertEquals("Attribute parameters are not supported by RS group/Item.", handler.cause().getMessage());
                        break;
                }
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test isValidQueryWithFilters method : Failure")
    public void test_isValidQueryWithFilters_failure(VertxTestContext vertxTestContext) {
        when(paramsMap.get(anyString())).thenReturn("Dummy/string/value", null);
        when(catalogueService.getApplicableFilters(anyString())).thenReturn(listFuture);
        when(listAsyncResult.succeeded()).thenReturn(false);
        doAnswer(new Answer<AsyncResult<List<String>>>() {
            @Override
            public AsyncResult<List<String>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<List<String>>>) arg0.getArgument(0)).handle(listAsyncResult);
                return null;
            }
        }).when(listFuture).onComplete(any());
        paramsValidator.validate(paramsMap).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("fail to get filters for validation",handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

@ParameterizedTest
@ValueSource(strings = {"point","polygon","linestring","bbox"})
    @DisplayName("Test isValidCoordinatesForGeometry method: For invalid geo param")
    public void test_isValidCoordinatesForGeometry_with_DxRuntimeException(String geo,VertxTestContext vertxTestContext)
    {
        when(paramsMap.get(anyString())).thenReturn("Dummy/string/value", geo,"[{ \"Polygon\" : \"some_value\"},{ \"Point\" : \"some_value\"},{ \"LineString\" : \"some_value\"},{ \"Polygon\" : \"some_value\"}]");
        when(catalogueService.getApplicableFilters(anyString())).thenReturn(listFuture);
        when(listFuture.result()).thenReturn(stringList);
        when(listAsyncResult.succeeded()).thenReturn(true);
        when(stringList.contains(anyString())).thenReturn(true);
        when(paramsMap.contains(anyString())).thenReturn(true);
        doAnswer(new Answer<AsyncResult<List<String>>>() {
            @Override
            public AsyncResult<List<String>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<List<String>>>) arg0.getArgument(0)).handle(listAsyncResult);
                return null;
            }
        }).when(listFuture).onComplete(any());
       assertThrows(DxRuntimeException.class,()->{
           paramsValidator.validate(paramsMap);
       });
        vertxTestContext.completeNow();

    }

    @Test
    @DisplayName("Test validate method : with bad query")
    public void test_validate_for_invalid_query(VertxTestContext vertxTestContext)
    {
        when(paramsMap.get(anyString())).thenReturn("Dummy/string/value", "geo","[{ \"Polygon\" : \"some_value\"},{ \"Point\" : \"some_value\"},{ \"LineString\" : \"some_value\"},{ \"Polygon\" : \"some_value\"}]");
        when(catalogueService.getApplicableFilters(anyString())).thenReturn(listFuture);
        when(listFuture.result()).thenReturn(stringList);
        when(listAsyncResult.succeeded()).thenReturn(true);
        when(stringList.contains(anyString())).thenReturn(true);
        when(paramsMap.contains(anyString())).thenReturn(true);
        doAnswer(new Answer<AsyncResult<List<String>>>() {
            @Override
            public AsyncResult<List<String>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<List<String>>>) arg0.getArgument(0)).handle(listAsyncResult);
                return null;
            }
        }).when(listFuture).onComplete(any());

        paramsValidator.validate(paramsMap).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals(MSG_BAD_QUERY,handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }
    @Test
    @DisplayName("Test isValidCoordinatesForGeometry method: For invalid geo param bbox")
    public void test_isValidCoordinatesForGeometry_for_bbox(VertxTestContext vertxTestContext)
    {
        when(paramsMap.get(anyString())).thenReturn("Dummy/string/value", "bbox","[{ \"Polygon\" : \"some_value\"},{ \"Point\" : \"some_value\"},{ \"LineString\" : \"some_value\"}]");
        when(catalogueService.getApplicableFilters(anyString())).thenReturn(listFuture);
        when(listFuture.result()).thenReturn(stringList);
        when(listAsyncResult.succeeded()).thenReturn(true);
        when(stringList.contains(anyString())).thenReturn(true);
        when(paramsMap.contains(anyString())).thenReturn(true);
        doAnswer(new Answer<AsyncResult<List<String>>>() {
            @Override
            public AsyncResult<List<String>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<List<String>>>) arg0.getArgument(0)).handle(listAsyncResult);
                return null;
            }
        }).when(listFuture).onComplete(any());
        assertThrows(DxRuntimeException.class,()->{
            paramsValidator.validate(paramsMap);
        });
        vertxTestContext.completeNow();

    }

}
