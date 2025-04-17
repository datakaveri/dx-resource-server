package org.cdpg.dx.rs.search.util.validatorTypes;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Extracts parameters from a JSON object into a MultiMap.
 * This class encapsulates the conversion logic and supports nested objects.
 */
public class ParameterExtractor {
  public MultiMap extractParams(JsonObject requestJson) {
    MultiMap paramsMap = MultiMap.caseInsensitiveMultiMap();
    requestJson.forEach(entry -> {
      String key = entry.getKey();
      Object value = entry.getValue();
      // Handle nested objects for specific keys
      if (key.equalsIgnoreCase("geoQ") || key.equalsIgnoreCase("temporalQ")) {
        paramsMap.add(key, value.toString());
        ((JsonObject) value).forEach(innerEntry ->
                paramsMap.add(innerEntry.getKey(), innerEntry.getValue().toString()));
      } else if (key.equalsIgnoreCase("entities") && value instanceof JsonArray) {
        paramsMap.add(key, value.toString());
        JsonArray array = (JsonArray) value;
        if (!array.isEmpty() && array.getValue(0) instanceof JsonObject) {
          ((JsonObject) array.getValue(0)).forEach(innerEntry ->
                  paramsMap.add(innerEntry.getKey(), innerEntry.getValue().toString()));
        }
      } else {
        paramsMap.add(key, value.toString());
      }
    });
    return paramsMap;
  }
}