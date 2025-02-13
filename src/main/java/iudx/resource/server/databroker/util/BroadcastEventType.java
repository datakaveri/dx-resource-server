package iudx.resource.server.databroker.util;

import java.util.HashMap;
import java.util.Map;

public enum BroadcastEventType {

    CREATE("create"), UPDATE("update"), DELETE("delete");

    public final String eventType;

    BroadcastEventType(String eventType) {
        this.eventType = eventType;
    }

    private static Map<String, BroadcastEventType> eventsMap;

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
