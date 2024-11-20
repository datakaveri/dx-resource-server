package iudx.resource.server.dataservice;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.authenticator.authorization.IudxRole;
import iudx.resource.server.authenticator.model.AuthInfo;
import iudx.resource.server.dataLimitService.model.ConsumedDataInfo;
import iudx.resource.server.dataLimitService.model.UserAuthLimitInfo;
import iudx.resource.server.dataLimitService.service.DataAccessLimitService;
import iudx.resource.server.dataLimitService.util.UniqueKeyUtil;
import iudx.resource.server.database.redis.RedisService;
import iudx.resource.server.database.redis.model.ResponseModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class DataAccessLimitServiceTest {

    String redisApiCountKey;
    String redisTotalSizeKey;
    private Vertx vertx;
    @Mock
    private RedisService redisService;
    @Mock
    private AuthInfo authInfo;
    private DataAccessLimitService dataAccessLimitService;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        dataAccessLimitService = new DataAccessLimitService(vertx, true, redisService);
        setUpAuthInfoMock();
    }

    private void setUpAuthInfoMock() {
        when(authInfo.getUserid()).thenReturn("testUser");
        when(authInfo.getResourceId()).thenReturn("testResource");
        when(authInfo.getRole()).thenReturn(IudxRole.CONSUMER);
        redisApiCountKey = UniqueKeyUtil.generateUniqueKey(authInfo.getUserid(), authInfo.getResourceId(), "apiCount");
        redisTotalSizeKey = UniqueKeyUtil.generateUniqueKey(authInfo.getUserid(), authInfo.getResourceId(), "totalSize");
    }

    @Test
    void testValidateDataAccessWithinLimits(VertxTestContext testContext) {
        setUpAuthInfoAccess("api", 200);
        UserAuthLimitInfo userAuthLimitInfo = new UserAuthLimitInfo(authInfo);

        ConsumedDataInfo consumedDataInfo = createConsumedDataInfo(100, 50);
        mockRedisServiceSuccess(consumedDataInfo);

        dataAccessLimitService.validateDataAccess(userAuthLimitInfo).onSuccess(result -> {
            assertTrue(result.isWithInLimit());
//            assertEquals(consumedDataInfo.getConsumedData(), result.getConsumedDataInfo().getConsumedData());
            verify(redisService).getFromRedis(anyList());
            testContext.completeNow();
        }).onFailure(testContext::failNow);
    }

    @Test
    void testValidateDataAccessExceedsLimits(VertxTestContext testContext) {
        setUpAuthInfoAccess("async", 20);
        UserAuthLimitInfo userAuthLimitInfo = new UserAuthLimitInfo(authInfo);
        ConsumedDataInfo consumedDataInfo = createConsumedDataInfo(20, 30);

        mockRedisServiceSuccess(consumedDataInfo);

        dataAccessLimitService.validateDataAccess(userAuthLimitInfo).onSuccess(result -> {
            assertFalse(result.isWithInLimit());
            assertEquals(consumedDataInfo.getApiCount(), result.getConsumedDataInfo().getApiCount());
            verify(redisService).getFromRedis(anyList());
            testContext.completeNow();
        }).onFailure(testContext::failNow);
    }

    @Test
    void testValidateDataAccessNonConsumerRole(VertxTestContext testContext) {
        when(authInfo.getRole()).thenReturn(IudxRole.ADMIN);

        UserAuthLimitInfo userAuthLimitInfo = new UserAuthLimitInfo(authInfo);

        dataAccessLimitService.validateDataAccess(userAuthLimitInfo).onSuccess(result -> {
            assertTrue(result.isWithInLimit());
            testContext.completeNow();
        }).onFailure(testContext::failNow);
    }

    @Test
    void testValidateDataAccessLimitNotEnabled(VertxTestContext testContext) {
        dataAccessLimitService = new DataAccessLimitService(vertx, false, redisService);
        UserAuthLimitInfo userAuthLimitInfo = new UserAuthLimitInfo(authInfo);

        dataAccessLimitService.validateDataAccess(userAuthLimitInfo).onSuccess(result -> {
            assertTrue(result.isWithInLimit());
            testContext.completeNow();
        }).onFailure(testContext::failNow);
    }

    @Test
    void testValidateDataAccessRedisFailure(VertxTestContext testContext) {
        UserAuthLimitInfo userAuthLimitInfo = new UserAuthLimitInfo(authInfo);

        doReturn(Future.failedFuture("Redis connection error")).when(redisService).getFromRedis(anyList());

        dataAccessLimitService.validateDataAccess(userAuthLimitInfo).onSuccess(result -> testContext.failNow("Expected a failure but got a success response.")).onFailure(failureHandler -> {
            verify(redisService).getFromRedis(anyList());
            assertEquals("Redis connection error", failureHandler.getMessage());
            testContext.completeNow();
        });
    }

    private void setUpAuthInfoAccess(String policy, int limit) {
        when(authInfo.getAccessPolicy()).thenReturn(policy);
        when(authInfo.getAccess()).thenReturn(new JsonObject().put(policy, new JsonObject().put("limit", limit)));
    }

    private ConsumedDataInfo createConsumedDataInfo(int consumedData, int apiCount) {
        ConsumedDataInfo consumedDataInfo = new ConsumedDataInfo();
        consumedDataInfo.setConsumedData(consumedData);
        consumedDataInfo.setApiCount(apiCount);
        return consumedDataInfo;
    }

    private void mockRedisServiceSuccess(ConsumedDataInfo consumedDataInfo) {
        ResponseModel responseModel = mock(ResponseModel.class);
        when(responseModel.getValueFromKey(redisApiCountKey)).thenReturn(consumedDataInfo.getApiCount());
        when(responseModel.getValueFromKey(redisTotalSizeKey)).thenReturn(consumedDataInfo.getConsumedData());
        List<String> keyList = Arrays.asList(redisApiCountKey, redisTotalSizeKey);
        doReturn(Future.succeededFuture(responseModel)).when(redisService).getFromRedis(keyList);
    }
}