package iudx.resource.server.dataservice;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import iudx.resource.server.authenticator.authorization.IudxRole;
import iudx.resource.server.authenticator.model.AuthInfo;
import iudx.resource.server.dataLimitService.handler.DataAccessHandler;
import iudx.resource.server.dataLimitService.model.ConsumedDataInfo;
import iudx.resource.server.dataLimitService.model.UserAuthLimitInfo;
import iudx.resource.server.dataLimitService.service.DataAccessLimitService;
import iudx.resource.server.dataLimitService.util.ValidateDataAccessResult;
import iudx.resource.server.database.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class,MockitoExtension.class})
class DataAccessHandlerTest {

    @Mock
    Vertx vertx;

    @Mock
    DataAccessLimitService dataAccessLimitService;

    @Mock
    RoutingContext routingContext;

    @Mock
    AuthInfo authInfo;

    @Mock
    UserAuthLimitInfo userAuthLimitInfo;

    @Mock
    RedisService redisService;

    @Mock
    ValidateDataAccessResult validateDataAccessResult;

    @Mock
    HttpServerResponse response;

    @Mock
    ConsumedDataInfo consumedDataInfo;

    DataAccessHandler dataAccessHandler;

    @BeforeEach
    void setUp() {
        // Initialize DataAccessHandler with the required constructor parameters
        dataAccessHandler = new DataAccessHandler(vertx, true, dataAccessLimitService,redisService);

        // Set up common mock behavior
        when(authInfo.getAccessPolicy()).thenReturn("SECURE");
        when(authInfo.getRole()).thenReturn(IudxRole.CONSUMER);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("authInfo", authInfo);
        when(routingContext.data()).thenReturn(dataMap);
    }

    @Test
    public void testValidationCheckNotRequiredSuccess() {
        // Set up handler for not enabling limits

        dataAccessHandler = new DataAccessHandler(vertx, false, dataAccessLimitService,redisService);
        when(authInfo.getAccessPolicy()).thenReturn("OPEN");

        dataAccessHandler.handle(routingContext);

        verify(routingContext).next();
    }

    @Test
    public void testWithinLimitSuccess() {
        // Simulate data within limit
        when(validateDataAccessResult.isWithInLimit()).thenReturn(true);
        when(validateDataAccessResult.getConsumedDataInfo()).thenReturn(consumedDataInfo);
        when(dataAccessLimitService.validateDataAccess(any(UserAuthLimitInfo.class)))
                .thenReturn(Future.succeededFuture(validateDataAccessResult));

        // Call the handle method
        dataAccessHandler.handle(routingContext);

        // Verify interactions
        verify(dataAccessLimitService).validateDataAccess(any(UserAuthLimitInfo.class));
        verify(validateDataAccessResult).getConsumedDataInfo();
        verify(routingContext).next();
    }

    @Test
    void testValidationLimitExceeded() {
        setupResponseMocks(429, "Limit Exceeded");

        // Simulate limit exceeded scenario
        when(dataAccessLimitService.validateDataAccess(any(UserAuthLimitInfo.class)))
                .thenReturn(Future.succeededFuture(mock(ValidateDataAccessResult.class)));

        dataAccessHandler.handle(routingContext);

        // Verify interactions
        verify(routingContext, never()).next();
        verify(dataAccessLimitService).validateDataAccess(any(UserAuthLimitInfo.class));
        verify(routingContext.response()).setStatusCode(429);
    }

    @Test
    void testInternalServerError() {
        setupResponseMocks(500, "Internal Server Error");

        // Simulate internal server error
        when(dataAccessLimitService.validateDataAccess(any(UserAuthLimitInfo.class))).thenReturn(Future.failedFuture("Internal Server Error"));

        dataAccessHandler.handle(routingContext);

        // Verify interactions
        verify(routingContext, never()).next();
        verify(routingContext.response()).setStatusCode(500);
    }

    // Helper method to setup common response mocks
    private void setupResponseMocks(int statusCode, String responseMessage) {
        when(routingContext.response()).thenReturn(response);
        when(routingContext.get("statusCode")).thenReturn(statusCode);
        when(routingContext.get("response")).thenReturn(responseMessage);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(response.end(anyString())).thenReturn(Future.succeededFuture());
    }
    private JsonObject createRedisConfig() {
        return new JsonObject()
                .put("redisHost", "localhost")
                .put("redisPort", 6379)
                .put("redisPassword", "password")
                .put("redisUsername", "default");
    }
}
