package org.cdpg.dx.common.models;

import io.vertx.core.json.JsonObject;

public record User(String userId, DxRole userRole, String emailId,
                   String firstName, String lastName, String resourceServerUrl) {

//    @JsonCreator
//    public User(@JsonProperty("userId") String userId,
//                @JsonProperty("userRole") String userRole,
//                @JsonProperty("emailId") String emailId,
//                @JsonProperty("firstName") String firstName,
//                @JsonProperty("lastName") String lastName,
//                @JsonProperty("resourceServerUrl") String resourceServerUrl) {
//        this(userId, DxRole.fromString(userRole), emailId, firstName, lastName, resourceServerUrl);
//    }
//
//    public static User fromJson(JsonObject json) {
//        return new User(json.getString("userId"),
//                        json.getString("userRole"),
//                        json.getString("emailId"),
//                        json.getString("firstName"),
//                        json.getString("lastName"),
//                        json.getString("resourceServerUrl"));
//    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }
}
