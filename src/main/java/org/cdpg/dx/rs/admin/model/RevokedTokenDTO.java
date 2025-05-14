package org.cdpg.dx.rs.admin.model;

import static org.cdpg.dx.rs.admin.util.Constants.REVOKED_TOKEN_TABLE;

import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.cdpg.dx.database.postgres.base.enitty.BaseEntity;

public record RevokedTokenDTO(
    UUID _id,
    LocalDateTime expiry,
    Optional<LocalDateTime> created_at,
    Optional<LocalDateTime> modified_at)
    implements BaseEntity<RevokedTokenDTO> {

  public static RevokedTokenDTO fromJson(JsonObject json) {
    return new RevokedTokenDTO(
        json.getString("_id") != null ? UUID.fromString(json.getString("_id")) : null,
        json.getString("expiry") != null ? LocalDateTime.parse(json.getString("expiry")) : null,
        Optional.ofNullable(json.getString("created_at")).map(LocalDateTime::parse),
        Optional.ofNullable(json.getString("modified_at")).map(LocalDateTime::parse));
  }

  @Override
  public Map<String, Object> toNonEmptyFieldsMap() {
    Map<String, Object> map = new HashMap<>();
    if (_id != null) map.put("_id", _id.toString());
    if (expiry != null) map.put("expiry", expiry.toString());
    return map;
  }


  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (_id != null) json.put("_id", _id.toString());
    if (expiry != null) json.put("expiry", expiry.toString());
    created_at.ifPresent(v -> json.put("created_at", v.toString()));
    modified_at.ifPresent(v -> json.put("modified_at", v.toString()));
    return json;
  }

  @Override
  public String getTableName() {
    return REVOKED_TOKEN_TABLE;
  }
}
