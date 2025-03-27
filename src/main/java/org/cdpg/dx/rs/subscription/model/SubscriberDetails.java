package org.cdpg.dx.rs.subscription.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class SubscriberDetails {
    private String queueName;
    private String entity;
    private JsonObject catItem;

    public SubscriberDetails() {
        // Default constructor
    }

    public SubscriberDetails(String queueName, String entity, JsonObject catItem) {
        this.queueName = queueName;
        this.entity = entity;
        this.catItem = catItem;
    }

    public SubscriberDetails(JsonObject json) {
        fromJson(json);
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("queueName", queueName)
                .put("entity", entity)
                .put("catItem", catItem);
    }

    public void fromJson(JsonObject json) {
        this.queueName = json.getString("queueName");
        this.entity = json.getString("entity");
        this.catItem = json.getJsonObject("catItem", new JsonObject());
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public JsonObject getCatItem() {
        return catItem;
    }

    public void setCatItem(JsonObject catItem) {
        this.catItem = catItem;
    }
}
