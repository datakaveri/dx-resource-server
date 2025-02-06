package iudx.resource.server.apiserver.ingestion.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;

public class IngestionServiceImpl implements IngestionService {
    @Override
    public Future<JsonObject> registerAdapter(JsonObject json, DataBrokerService dataBroker, CacheService cacheService, PostgresService postgresService) {
        return null;
    }

    @Override
    public Future<JsonObject> deleteAdapter(String adapterId, String userId, DataBrokerService dataBroker, PostgresService postgresService) {
        return null;
    }

    @Override
    public Future<JsonObject> getAdapterDetails(String adapterId, DataBrokerService databroker) {
        return null;
    }

    @Override
    public Future<JsonObject> publishHeartbeat(JsonObject json, DataBrokerService databroker) {
        return null;
    }

    @Override
    public Future<JsonObject> publishDataFromAdapter(JsonArray json, DataBrokerService databroker) {
        return null;
    }

    @Override
    public Future<JsonObject> getAllAdapterDetailsForUser(JsonObject json, PostgresService postgresService) {
        return null;
    }
}
