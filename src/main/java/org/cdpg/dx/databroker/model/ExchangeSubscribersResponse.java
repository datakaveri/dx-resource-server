package org.cdpg.dx.databroker.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DataObject
public class ExchangeSubscribersResponse {
  private Map<String, List<String>> subscribers;

  public ExchangeSubscribersResponse() {}

  // Constructor to accept a map directly
  public ExchangeSubscribersResponse(Map<String, List<String>> subscribers) {
    this.subscribers = subscribers;
  }

  public ExchangeSubscribersResponse(JsonObject json) {
    this.subscribers =
        json.getMap().entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                      Object value = entry.getValue();
                      if (value instanceof JsonArray) {
                        return ((JsonArray) value)
                            .stream().map(Object::toString).collect(Collectors.toList());
                      } else if (value instanceof List) {
                        return ((List<?>) value)
                            .stream().map(Object::toString).collect(Collectors.toList());
                      } else {
                        return List.of(value.toString());
                      }
                    }));
  }

  // Convert object to JsonObject
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (subscribers != null && !subscribers.isEmpty()) {
      subscribers.forEach((key, value) -> json.put(key, new JsonArray(value)));
    }
    return json;
  }

  public Map<String, List<String>> getSubscribers() {
    return subscribers;
  }

  public void setSubscribers(Map<String, List<String>> subscribers) {
    this.subscribers = subscribers;
  }

  @Override
  public String toString() {
    return "ExchangeSubscribersResponse{" + "subscribers=" + subscribers + '}';
  }
}
