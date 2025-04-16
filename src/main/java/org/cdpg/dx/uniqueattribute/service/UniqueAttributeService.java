package org.cdpg.dx.uniqueattribute.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface UniqueAttributeService {
  @GenIgnore
  static UniqueAttributeService createProxy(Vertx vertx, String address) {
    return new UniqueAttributeServiceVertxEBProxy(vertx, address);
  }

  Future<JsonObject> fetchUniqueAttributeInfo(String id);

  Future<Void> putUniqueAttributeInCache(String id, String value);
}
