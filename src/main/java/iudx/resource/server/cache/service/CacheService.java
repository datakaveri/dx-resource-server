package iudx.resource.server.cache.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;


@ProxyGen
@VertxGen
public interface CacheService {

  @GenIgnore
  static CacheService createProxy(Vertx vertx, String address) {
    return new CacheServiceVertxEBProxy(vertx, address);
  }

  /**
   * get value from cache passing a json object specifying cache name (in case of multiple caches
   * are configured) and key.
   *
   * <pre>
   * json ex (type and key both are required)
   * {
   *    "type": "cache name",
   *    "key" : "cache key to fetch value"
   * }
   * </pre>
   *
   * <p>in case of success method returns a value abstracted in json object else handler will fail.
   *
   * <pre>
   * {
   *    "value":"value for key"
   * }
   * </pre>
   *
   * @param request valid json request
   * @return Future
   */
  Future<JsonObject> get(JsonObject request);

  /**
   * put value in cache passing a json object specifying cache name (in case of multiple caches are
   * configured), key and value
   *
   * <pre>
   * json ex (type, key & value are required)
   * {
   *    "type": "cache name",
   *    "key" : "cache key to fetch value",
   *    "value": "value for cache key"
   * }
   * </pre>
   *
   * <p>in case of success a json object will be returned else handler will fail.
   *
   * <pre>
   * {
   *    "key":"value"
   * }
   * </pre>
   *
   * @param request valid json request
   * @return in case of success a json object will be returned else handler will fail.
   *     <pre>
   *     {
   *        "key":"value"
   *     }
   *     </pre>
   */
  Future<JsonObject> put(JsonObject request);

  /**
   * method used to refresh content of cache specifying the name of cache, key(optional) and
   * value(optional). When Key and Value are not provided cache will be refreshed from source
   * (DB/external cache etc.) else key and value are directly placed into cache without calling
   * source.
   *
   * <pre>
   * {
   *    "type":"cache name",
   *    "key":"cache key",
   *    "value":"cache value"
   * }
   * </pre>
   *
   * @param request valid json request
   * @return Future
   */
  Future<JsonObject> refresh(JsonObject request);
}
