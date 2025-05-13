package org.cdpg.dx.rs.ingestion.model;

import static org.cdpg.dx.rs.ingestion.util.Constants.INGESTION_TABLE;

import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.cdpg.dx.database.postgres.base.enitty.BaseEntity;

public record IngestionDTO(
    Optional<UUID> _id,
    String exchange_name,
    String resource_id,
    String dataset_name,
    JsonObject dataset_details_json,
    String user_id,
    Optional<LocalDateTime> created_at,
    Optional<LocalDateTime> modified_at,
    UUID providerid)
    implements BaseEntity<IngestionDTO> {

  public static IngestionDTO fromJson(JsonObject json) {
    return new IngestionDTO(
        Optional.ofNullable(json.getString("_id")).map(UUID::fromString),
        json.getString("exchange_name"),
        json.getString("resource_id"),
        json.getString("dataset_name"),
        json.getJsonObject("dataset_details_json"),
        json.getString("user_id"),
        Optional.ofNullable(json.getString("created_at")).map(LocalDateTime::parse),
        Optional.ofNullable(json.getString("modified_at")).map(LocalDateTime::parse),
        UUID.fromString(json.getString("providerid")));
  }

  @Override
  public Map<String, Object> toNonEmptyFieldsMap() {
    Map<String, Object> map = new HashMap<>();
    if (exchange_name != null) map.put("exchange_name", exchange_name);
    if (resource_id != null) map.put("resource_id", resource_id);
    if (dataset_name != null) map.put("dataset_name", dataset_name);
    if (dataset_details_json != null) map.put("dataset_details_json", dataset_details_json);
    if (user_id != null) map.put("user_id", user_id);
    if (providerid != null) map.put("providerid", providerid.toString());
    return map;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    _id.ifPresentOrElse(v -> json.put("_id", v.toString()), null);
    if (exchange_name != null) json.put("exchange_name", exchange_name);
    if (resource_id != null) json.put("resource_id", resource_id);
    if (dataset_name != null) json.put("dataset_name", dataset_name);
    if (dataset_details_json != null) json.put("dataset_details_json", dataset_details_json);
    if (user_id != null) json.put("user_id", user_id);
    if (providerid != null) json.put("providerid", providerid.toString());
    created_at.ifPresent(v -> json.put("created_at", v.toString()));
    modified_at.ifPresent(v -> json.put("modified_at", v.toString()));
    return json;
  }

  @Override
  public String getTableName() {
    return INGESTION_TABLE;
  }
}
