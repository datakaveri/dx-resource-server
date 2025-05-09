package org.cdpg.dx.rs.subscription.model;

import static org.cdpg.dx.rs.subscription.util.Constants.SUBSCRIPTION_TABLE;

import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.cdpg.dx.common.util.DateTimeHelper;
import org.cdpg.dx.database.postgres.base.enitty.BaseEntity;

public record SubscriptionDTO(
    String _id,
    String _type,
    String queue_name,
    String entity,
    String expiry,
    String dataset_name,
    JsonObject dataset_json,
    String user_id,
    String resource_group,
    String provider_id,
    String delegator_id,
    String item_type,
    Optional<LocalDateTime> created_at,
    Optional<LocalDateTime> modified_at)
    implements BaseEntity<SubscriptionDTO> {

  public static SubscriptionDTO fromJson(JsonObject json) {
    return new SubscriptionDTO(
        json.getString("_id"),
        json.getString("_type"),
        json.getString("queue_name"),
        json.getString("entity"),
        json.getString("expiry"),
        json.getString("dataset_name"),
        json.getJsonObject("dataset_json"),
        json.getString("user_id"),
        json.getString("resource_group"),
        json.getString("provider_id"),
        json.getString("delegator_id"),
        json.getString("item_type"),
        DateTimeHelper.parse(json.getString("created_at")),
        DateTimeHelper.parse(json.getString("modified_at")));
  }

  @Override
  public Map<String, Object> toNonEmptyFieldsMap() {
    Map<String, Object> map = new HashMap<>();
    if (_id != null) map.put("_id", _id);
    if (_type != null) map.put("_type", _type);
    if (queue_name != null) map.put("queue_name", queue_name);
    if (entity != null) map.put("entity", entity);
    if (expiry != null) map.put("expiry", expiry);
    if (dataset_name != null) map.put("dataset_name", dataset_name);
    if (dataset_json != null) map.put("dataset_json", dataset_json);
    if (user_id != null) map.put("user_id", user_id);
    if (resource_group != null) map.put("resource_group", resource_group);
    if (provider_id != null) map.put("provider_id", provider_id);
    if (delegator_id != null) map.put("delegator_id", delegator_id);
    if (item_type != null) map.put("item_type", item_type);
    return map;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (_id != null) json.put("_id", _id);
    if (_type != null) json.put("_type", _type);
    if (queue_name != null) json.put("queue_name", queue_name);
    if (entity != null) json.put("entityId", entity);
    if (expiry != null) json.put("expiry", expiry);
    if (dataset_name != null) json.put("dataset_name", dataset_name);
    if (dataset_json != null) json.put("dataset_json", dataset_json);
    if (user_id != null) json.put("user_id", user_id);
    if (resource_group != null) json.put("resource_group", resource_group);
    if (provider_id != null) json.put("provider_id", provider_id);
    if (delegator_id != null) json.put("delegator_id", delegator_id);
    if (item_type != null) json.put("item_type", item_type);
    created_at.ifPresent(v -> json.put("created_at", v.toString()));
    modified_at.ifPresent(v -> json.put("modified_at", v.toString()));
    return json;
  }

  @Override
  public String getTableName() {
    return SUBSCRIPTION_TABLE;
  }
}
