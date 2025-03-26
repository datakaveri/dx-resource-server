package org.cdpg.dx.catalogue.client;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.UUID;
public interface CatalogueClient {

  Future<JsonObject> fetchCatalogueData(String id);

  }
