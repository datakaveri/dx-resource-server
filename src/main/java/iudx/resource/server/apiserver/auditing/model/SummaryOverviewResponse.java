package iudx.resource.server.apiserver.auditing.model;

import io.vertx.core.json.JsonObject;

public class SummaryOverviewResponse {

  private String resourceid;
  private String resource_label;
  private String publisher;
  private String publisher_id;
  private String city;
  private long count;

  public SummaryOverviewResponse() {}

  // Constructor from JsonObject
  public SummaryOverviewResponse(JsonObject json) {
    this.resourceid = json.getString("id");
    this.resource_label = json.getString("description");
    this.publisher = json.getString("name");
    this.publisher_id = json.getString("provider");
    this.city = json.getString("instance");
    this.count = json.getLong("count", 0L);
  }

  // Convert object to JsonObject
  public JsonObject toJson() {
    return new JsonObject()
        .put("resourceid", resourceid)
        .put("resource_label", resource_label)
        .put("publisher", publisher)
        .put("publisher_id", publisher_id)
        .put("city", city)
        .put("count", count);
  }

  // Getters and Setters
  public String getResourceid() {
    return resourceid;
  }

  public void setResourceid(String resourceid) {
    this.resourceid = resourceid;
  }

  public String getResource_label() {
    return resource_label;
  }

  public void setResource_label(String resource_label) {
    this.resource_label = resource_label;
  }

  public String getPublisher() {
    return publisher;
  }

  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }

  public String getPublisher_id() {
    return publisher_id;
  }

  public void setPublisher_id(String publisher_id) {
    this.publisher_id = publisher_id;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }
}
