package org.cdpg.dx.rs.search.util;

public enum RequestType {
  ENTITY("entity"),
  TEMPORAL("temporal"),
  POST_TEMPORAL("post_temporal_schema.json"),
  POST_ENTITIES("post_entities_schema.json"),;

  private String filename;

  RequestType(String fileName) {
    this.filename = fileName;
  }

  public String getFilename() {
    return this.filename;
  }
}