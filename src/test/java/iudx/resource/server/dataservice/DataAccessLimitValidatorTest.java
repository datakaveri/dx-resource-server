package iudx.resource.server.dataservice;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.authorization.IudxRole;
import iudx.resource.server.authenticator.model.AuthInfo;
import iudx.resource.server.dataLimitService.model.ConsumedDataInfo;
import iudx.resource.server.dataLimitService.model.UserAuthLimitInfo;
import iudx.resource.server.dataLimitService.util.DataAccessLimitValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataAccessLimitValidatorTest {

    private AuthInfo authInfo;
    private UserAuthLimitInfo userAuthLimitInfo;
    private JsonObject accessMock;
    private DataAccessLimitValidator dataAccessLimitValidator;
    @BeforeEach
    void setUp() {
        authInfo = mock(AuthInfo.class);
        accessMock = mock(JsonObject.class);
        dataAccessLimitValidator=new DataAccessLimitValidator();
        when(authInfo.getAccess()).thenReturn(accessMock);
    }

    // this allows us to retest the same code with different params
    @ParameterizedTest
    @CsvSource({
            "api, CONSUMER, 10, 5, true",
            "api, CONSUMER, 1, 10, false",
            "async, CONSUMER, 20, 15, true",
            "async, CONSUMER, 10, 15, false"
    })
    //below test will be tested 4 times with 4 different set of params
    void shouldReturnExpectedResultWhenUsageIsWithinOrExceedsLimits(String accessPolicy, IudxRole role, int limit, long consumedData, boolean expectedResult) {
        setupAuthInfo(accessPolicy, role, limit);
        userAuthLimitInfo = new UserAuthLimitInfo(authInfo);

        ConsumedDataInfo consumedDataInfo = createMockConsumedDataInfo(accessPolicy, consumedData);

        boolean result = dataAccessLimitValidator.isUsageWithinLimits(userAuthLimitInfo, consumedDataInfo);
        assertEquals(expectedResult, result, String.format("%s usage should %s limits", accessPolicy, expectedResult ? "be within" : "exceed"));
    }

    @Test
    void successWhenSubUsageWithinLimits() {
        setupAuthInfo("sub", IudxRole.CONSUMER, 10);
        userAuthLimitInfo = new UserAuthLimitInfo(authInfo);

        ConsumedDataInfo consumedDataInfo = createMockConsumedDataInfo("sub", 10L);

        boolean result = dataAccessLimitValidator.isUsageWithinLimits(userAuthLimitInfo, consumedDataInfo);

        assertTrue(result, "Sub usage should always be within limits");
    }

    @Test
    void successWhenNoLimitEnabledForAdminRole() {
        setupAuthInfo("sub", IudxRole.ADMIN, 0); // No limit for ADMIN role
        userAuthLimitInfo = new UserAuthLimitInfo(authInfo);

        ConsumedDataInfo consumedDataInfo = createMockConsumedDataInfo("sub", 0L);

        boolean result = dataAccessLimitValidator.isUsageWithinLimits(userAuthLimitInfo, consumedDataInfo);

        assertTrue(result, "Usage should always be allowed when limits are not enabled");
    }

    @Test
    void failureWhenInvalidAccessPolicy() {
        setupAuthInfo("invalidAccessType", IudxRole.CONSUMER, 10);
        userAuthLimitInfo = new UserAuthLimitInfo(authInfo);

        ConsumedDataInfo consumedDataInfo = createMockConsumedDataInfo("invalidPolicy", 0L);

        boolean result = dataAccessLimitValidator.isUsageWithinLimits(userAuthLimitInfo, consumedDataInfo);
        assertFalse(result, "Unrecognized access policies should default to exceed limits");
    }

    @Test
    void failureWhenNegativeLimitIsSet() {
        setupAuthInfo("api", IudxRole.CONSUMER, -1);
        userAuthLimitInfo = new UserAuthLimitInfo(authInfo);

        ConsumedDataInfo consumedDataInfo = createMockConsumedDataInfo("api", 5L);

        boolean result = dataAccessLimitValidator.isUsageWithinLimits(userAuthLimitInfo, consumedDataInfo);

        assertFalse(result, "Negative limits should always result in exceeding usage");
    }

    @Test
    void successWhenOpenResource() {
        setupAuthInfo("OPEN", IudxRole.CONSUMER, -1);
        userAuthLimitInfo = new UserAuthLimitInfo(authInfo);

        ConsumedDataInfo consumedDataInfo = createMockConsumedDataInfo("api", 5L);

        boolean result = dataAccessLimitValidator.isUsageWithinLimits(userAuthLimitInfo, consumedDataInfo);

        assertTrue(result, "Negative limits should always result in exceeding usage");
    }

    // Helper method to set up common AuthInfo mock behavior
    private void setupAuthInfo(String accessPolicy, IudxRole role, int limit) {
        when(authInfo.getAccessPolicy()).thenReturn(accessPolicy);
        when(authInfo.getRole()).thenReturn(role);
        when(accessMock.getJsonObject(accessPolicy)).thenReturn(new JsonObject().put("limit", limit));
    }

    // Helper method to mock ConsumedDataInfo for different access policies
    private ConsumedDataInfo createMockConsumedDataInfo(String accessPolicy, long consumedData) {
        ConsumedDataInfo consumedDataInfo = mock(ConsumedDataInfo.class);
        if ("api".equals(accessPolicy)) {
            when(consumedDataInfo.getApiCount()).thenReturn(consumedData);
        } else if ("async".equals(accessPolicy)) {
            when(consumedDataInfo.getConsumedData()).thenReturn(consumedData);
        }
        return consumedDataInfo;
    }
}
