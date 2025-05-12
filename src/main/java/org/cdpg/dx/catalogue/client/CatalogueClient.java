package org.cdpg.dx.catalogue.client;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;

import java.util.Optional;

public interface CatalogueClient {

  Future<JsonArray> fetchCatalogueData();

  Future<Optional<JsonArray>> getCatalogueInfoForId(String id);

  Future<String> getProviderOwnerUserId(String id);
}
