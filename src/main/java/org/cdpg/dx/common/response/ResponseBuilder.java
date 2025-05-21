package org.cdpg.dx.common.response;


import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.cdpg.dx.common.HttpStatusCode;
import org.cdpg.dx.common.response.DxResponse;

public class ResponseBuilder {

    public static <T> DxResponse<T> success(String detail, T result) {
        HttpStatusCode code = HttpStatusCode.SUCCESS;
        return new DxResponse<>(code.getUrn(), code.getDescription(), detail, result);
    }

    public static DxResponse<Void> success(String detail) {
        return success(detail, null);
    }

    public static <T> void send(RoutingContext ctx, HttpStatusCode status, String detail, T result) {
        DxResponse<T> response = new DxResponse<>(status.getUrn(), status.getDescription(), detail, result);
        ctx.response()
                .setStatusCode(status.getValue())
                .putHeader("Content-Type", "application/json")
                .end(JsonObject.mapFrom(response).encode());
    }

    public static void sendSuccess(RoutingContext ctx, String detail) {
        send(ctx, HttpStatusCode.SUCCESS, detail, null);
    }

    public static <T> void sendSuccess(RoutingContext ctx, T result) {
        send(ctx, HttpStatusCode.SUCCESS, null, result);
    }
    public static void sendNoContent(RoutingContext ctx) {
        send(ctx, HttpStatusCode.NO_CONTENT, null, null);
    }
}

