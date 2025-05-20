package org.cdpg.dx.rs.usermanagement.model;

import io.vertx.core.json.JsonObject;

public record ResetPassword(String userId, String password) {
    public JsonObject toJson() {
        return new JsonObject()
            .put("username", userId)
            .put("apiKey", password);
    }
}
