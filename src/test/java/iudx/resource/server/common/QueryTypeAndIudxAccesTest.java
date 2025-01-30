package iudx.resource.server.common;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.authenticator.model.DxAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(VertxExtension.class)
public class QueryTypeAndIudxAccesTest {

    @ParameterizedTest
    @EnumSource
    public void test(QueryType queryType, VertxTestContext vertxTestContext) {
        assertNotNull(queryType);
        vertxTestContext.completeNow();
    }

    @Test
    public void testAccess(VertxTestContext vertxTestContext) {
        assertNull(DxAccess.fromAccess("provider"));
        vertxTestContext.completeNow();
    }
}
