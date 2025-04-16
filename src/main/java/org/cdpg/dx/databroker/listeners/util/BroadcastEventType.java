package org.cdpg.dx.databroker.listeners.util;

import java.util.HashMap;
import java.util.Map;

public enum BroadcastEventType {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete");

  private static Map<String, BroadcastEventType> eventsMap;
  public final String eventType;

  BroadcastEventType(String eventType) {
    this.eventType = eventType;
  }

  public static BroadcastEventType from(String text) {
    if (eventsMap == null) {
      eventsMap = new HashMap<>();
      for (BroadcastEventType event : values()) {
        eventsMap.put(event.toString(), event);
      }
    }
    return eventsMap.get(text);
  }
}
