package org.cdpg.dx.common.models;

import io.vertx.core.json.JsonObject;

public record JwtData(
        String accessToken,
        String sub,
        String iss,
        String aud,
        Long exp,
        Long iat,
        String iid,
        String role,
        JsonObject cons,
        String drl,
        String did,
        String expiry
) {
    public JwtData(JsonObject json) {
        this(
                requireNonNull(json.getString("access_token"), "Missing 'access_token'"),
                requireNonNull(json.getString("sub"), "Missing 'sub'"),
                requireNonNull(json.getString("iss"), "Missing 'iss'"),
                requireNonNull(json.getString("aud"), "Missing 'aud'"),
                requireNonNull(json.getLong("exp"), "Missing 'exp'"),
                requireNonNull(json.getLong("iat"), "Missing 'iat'"),
                json.getString("iid"),
                json.getString("role"),
                json.getJsonObject("cons", new JsonObject()), // Default to empty object
                json.getString("drl"),
                json.getString("did"),
                json.getString("expiry")
        );
    }

    private static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    @Override
    public String toString() {
        return "JwtData{" +
                "accessToken='" + accessToken + '\'' +
                ", sub='" + sub + '\'' +
                ", iss='" + iss + '\'' +
                ", aud='" + aud + '\'' +
                ", exp=" + exp +
                ", iat=" + iat +
                ", iid='" + iid + '\'' +
                ", role='" + role + '\'' +
                ", cons=" + cons +
                ", drl='" + drl + '\'' +
                ", did='" + did + '\'' +
                ", expiry='" + expiry + '\'' +
                '}';
    }
}