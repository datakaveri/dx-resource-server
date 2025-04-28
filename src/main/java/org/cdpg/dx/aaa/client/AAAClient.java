package org.cdpg.dx.aaa.client;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface AAAClient {
    Future<JsonObject> fetchUserData(String userId, String role, String resourceServer);

     Future<String> fetchCertKey();
}