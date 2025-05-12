package org.cdpg.dx.common.util;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class EntityUtil {
  public static void putIfPresent(Map<String, Object> map, String key, Optional<?> value) {
    value.ifPresent(v -> map.put(key, v));
  }

  public static void putIfNonEmpty(Map<String, Object> map, String key, String value) {
    if (value != null && !value.isEmpty()) {
      map.put(key, value);
    }
  }

  public static Optional<UUID> parseUUID(String value) {
    return Optional.ofNullable(value).map(UUID::fromString);
  }
}
