package iudx.resource.server.apiserver.response;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
class ResponseUtilTest {
    ResponseUtil responseUtil;

    @Test
    void generateResponseWithMesssageTet(VertxTestContext vertxTestContext) {
        responseUtil = new ResponseUtil();
        JsonObject response = responseUtil.generateResponse(HttpStatusCode.SUCCESS, ResponseUrn.SUCCESS_URN, "Success");
        assertEquals("urn:dx:rs:success", response.getString("type"));
        assertEquals("Success", response.getString("title"));
        assertEquals("Success", response.getString("detail"));
        vertxTestContext.completeNow();
    }
    @Test
    void generateResponseWithoutMesssageTet(VertxTestContext vertxTestContext) {
        responseUtil = new ResponseUtil();
        JsonObject response = responseUtil.generateResponse(HttpStatusCode.SUCCESS, ResponseUrn.SUCCESS_URN);
        assertEquals("urn:dx:rs:success", response.getString("type"));
        assertEquals("Success", response.getString("title"));
        assertEquals("urn:dx:rs:Success", response.getString("detail"));
        vertxTestContext.completeNow();
    }

}
