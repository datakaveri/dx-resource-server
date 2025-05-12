package org.cdpg.dx.catalogue.client;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;

public interface CatalogueClient {

  Future<JsonArray> fetchCatalogueData();

  Future<JsonArray> getCatalogueInfoForId(String id);

  Future<String> getProviderOwnerUserId(String id);
}
