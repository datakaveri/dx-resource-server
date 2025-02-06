package iudx.resource.server.apiserver.async.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Async Service.
 *
 * <h1>Async Service</h1>
 *
 * <p>The Async Service in the IUDX Resource Server defines the operations to be performed with the
 * IUDX Async Server.
 *
 * @see ProxyGen
 * @see VertxGen
 * @version 1.0
 * @since 2022-02-08
 */
@VertxGen
@ProxyGen
public interface AsyncService {

  @GenIgnore
  static AsyncService createProxy(Vertx vertx, String address) {
    return new AsyncServiceVertxEBProxy(vertx, address);
  }

  Future<Void> asyncSearch(
      String requestId,
      String sub,
      String searchId,
      JsonObject query,
      String format,
      String role,
      String drl,
      String did);

  Future<JsonObject> asyncStatus(String sub, String searchId);
}
