package org.cdpg.dx.common.response;

import io.vertx.core.json.JsonObject;

public class DxErrorResponse {
    private final String  type;
    private final String title;
    private final String detail;

    public DxErrorResponse(String type, String title, String detail) {
        this.type = type;
        this.title = title;
        this.detail = detail;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("type", type)
                .put("title", title)
                .put("detail", detail);
    }
}
