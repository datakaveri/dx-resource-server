package org.cdpg.dx.revoked.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface RevokedService {
  @GenIgnore
  static RevokedService createProxy(Vertx vertx, String address) {
    return new RevokedServiceVertxEBProxy(vertx, address);
  }

  Future<JsonObject> fetchRevokedInfo(String id);
  Future<Void> putRevokedInCache(String id, String value);
}
