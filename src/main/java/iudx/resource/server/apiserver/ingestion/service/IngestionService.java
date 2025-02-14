package iudx.resource.server.apiserver.ingestion.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;

public interface IngestionService {

  Future<JsonObject> registerAdapter(
      JsonObject json,
      DataBrokerService dataBroker,
      CacheService cacheService,
      PostgresService postgresService);

  Future<JsonObject> deleteAdapter(
      String adapterId,
      String userId,
      DataBrokerService dataBroker,
      PostgresService postgresService);

  Future<JsonObject> getAdapterDetails(String adapterId, DataBrokerService databroker);

  Future<JsonObject> publishHeartbeat(JsonObject json, DataBrokerService databroker);

  Future<JsonObject> publishDataFromAdapter(JsonArray json, DataBrokerService databroker);

  Future<JsonObject> getAllAdapterDetailsForUser(JsonObject json, PostgresService postgresService);
}
