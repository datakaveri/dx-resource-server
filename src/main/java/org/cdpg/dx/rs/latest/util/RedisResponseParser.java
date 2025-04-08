// RedisResponseParser.java
package org.cdpg.dx.rs.latest.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.stream.Collectors;

public class RedisResponseParser {
    public JsonArray parseResponse(String key, JsonObject result, boolean groupSnapshot) {
        if (groupSnapshot) {
            result.remove(key);
            return new JsonArray(result.stream()
                .map(entry -> entry.getValue())
                .collect(Collectors.toList()));
        }
        return new JsonArray().add(result);
    }
}
