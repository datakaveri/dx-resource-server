package iudx.resource.server.common;

public enum RequestType {
  ENTITY("entity"),
  TEMPORAL("temporal"),
  LATEST("latest"),
  ASYNC_SEARCH("async_search"),
  ASYNC_STATUS("async_status"),
  POST_TEMPORAL("post_temporal_schema.json"),
  POST_ENTITIES("post_entities_schema.json"),
  SUBSCRIPTION("subscription_schema.json"),
  OVERVIEW("overview");

  private String filename;

  RequestType(String fileName) {
    this.filename = fileName;
  }

  public String getFilename() {
    return this.filename;
  }
}
