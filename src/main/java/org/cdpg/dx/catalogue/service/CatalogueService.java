package org.cdpg.dx.catalogue.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.util.CacheValue;

@VertxGen
@ProxyGen
public interface CatalogueService {
    @GenIgnore
    static CatalogueService createProxy(Vertx vertx, String address) {
        return new CatalogueServiceVertxEBProxy(vertx, address);
    }
    Future<JsonObject> fetchCatalogueInfo(String id);
}
