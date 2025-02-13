package iudx.resource.server.cache.util;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface IudxCache {

  Future<Void> put(String key, CacheValue<JsonObject> value);

  Future<CacheValue<JsonObject>> get(String key);

  Future<Void> refreshCache();

  CacheValue<JsonObject> createCacheValue(String key, String value);
}
