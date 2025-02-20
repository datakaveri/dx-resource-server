package iudx.resource.server.apiserver.ingestion.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface IngestionService {

  Future<JsonObject> registerAdapter(
      JsonObject json);

  Future<JsonObject> deleteAdapter(
      String adapterId,
      String userId);

  Future<JsonObject> getAdapterDetails(String adapterId);

  Future<JsonObject> publishDataFromAdapter(JsonArray json);

  Future<JsonObject> getAllAdapterDetailsForUser(String iid);
}
