package org.cdpg.dx.rs.subscription.model;

import io.vertx.core.json.JsonObject;

public record SubscriptionDTO(String _id, String _type, String queue_name, String entityId, String expiry, String dataset_name,
                              String dataset_json, String user_id, String resource_group, String provider_id, String delegator_id, String item_type) {
}
