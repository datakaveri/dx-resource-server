/*
package iudx.resource.server.apiserver.usermanagement.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.usermanagement.model.ResetPasswordModel;
import iudx.resource.server.apiserver.usermanagement.model.ResetResponseModel;
import iudx.resource.server.apiserver.usermanagement.service.UserManagementServiceImpl;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class UserManagementServiceImplTest {

    private UserManagementServiceImpl userManagementService;
    private DataBrokerService dataBrokerService;

    @BeforeEach
    void setUp() {
        dataBrokerService = mock(DataBrokerService.class);
        userManagementService = new UserManagementServiceImpl(dataBrokerService);
    }

    @Test
    void testResetPassword_Success(VertxTestContext testContext) {
        String userId = "testUser";

        when(dataBrokerService.resetPassword(userId)).thenReturn(Future.succeededFuture(new ResetPasswordModel()));

        userManagementService.resetPassword(userId).onComplete(ar -> {
            assertTrue(ar.succeeded());
            assertNotNull(ar.result());
            assertEquals("Successfully changed the password", ar.result().getDetail());
            testContext.completeNow();
        });
    }

    @Test
    void testResetPassword_Failure(VertxTestContext testContext) {
        String userId = "testUser";
        String errorMessage = "Password reset failed";

        when(dataBrokerService.resetPassword(userId)).thenReturn(Future.failedFuture(errorMessage));

        userManagementService.resetPassword(userId).onComplete(ar -> {
            assertTrue(ar.failed());
            assertEquals(errorMessage, ar.cause().getMessage());
            testContext.completeNow();
        });
    }
}

*/
