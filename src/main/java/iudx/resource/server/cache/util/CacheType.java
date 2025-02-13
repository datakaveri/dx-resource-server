package iudx.resource.server.cache.util;

public enum CacheType {
  REVOKED_CLIENT("revoked_client"),
  UNIQUE_ATTRIBUTE("unique_attribute"),
  CATALOGUE_CACHE("catalogue_cache");

  String cacheName;

  CacheType(String name) {
    this.cacheName = name;
  }
}
