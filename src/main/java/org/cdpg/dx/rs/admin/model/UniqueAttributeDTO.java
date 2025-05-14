package org.cdpg.dx.rs.admin.model;

import static org.cdpg.dx.rs.admin.util.Constants.UNIQUE_ATTR_TABLE;

import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.cdpg.dx.database.postgres.base.enitty.BaseEntity;

public record UniqueAttributeDTO(
    Optional<UUID> _id,
    String unique_attribute,
    String resource_id,
    Optional<LocalDateTime> created_at,
    Optional<LocalDateTime> modified_at)
    implements BaseEntity<UniqueAttributeDTO> {

  public static UniqueAttributeDTO fromJson(JsonObject json) {
    return new UniqueAttributeDTO(
        Optional.ofNullable(json.getString("_id")).map(UUID::fromString),
        json.getString("unique_attribute"),
        json.getString("resource_id"),
        Optional.ofNullable(json.getString("created_at")).map(LocalDateTime::parse),
        Optional.ofNullable(json.getString("modified_at")).map(LocalDateTime::parse));
  }

  @Override
  public Map<String, Object> toNonEmptyFieldsMap() {
    Map<String, Object> map = new HashMap<>();
    if (unique_attribute != null) map.put("unique_attribute", unique_attribute);
    if (resource_id != null) map.put("resource_id", resource_id);
    return map;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    _id.ifPresent(v -> json.put("_id", v.toString()));
    if (unique_attribute != null) json.put("unique_attribute", unique_attribute);
    if (resource_id != null) json.put("resource_id", resource_id);
    created_at.ifPresent(v -> json.put("created_at", v.toString()));
    modified_at.ifPresent(v -> json.put("modified_at", v.toString()));
    return json;
  }

  @Override
  public String getTableName() {
    return UNIQUE_ATTR_TABLE;
  }
}
