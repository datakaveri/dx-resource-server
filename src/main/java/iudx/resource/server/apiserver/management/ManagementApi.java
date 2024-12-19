package iudx.resource.server.apiserver.management;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;

public interface ManagementApi {
  /**
   * register a adapter.
   *
   * @param json request json
   * @param dataBroker DataBrokerService object
   * @param cacheService CacheService object
   * @param postgresService PostgresService object
   * @return Future
   */
  Future<JsonObject> registerAdapter(
      JsonObject json,
      DataBrokerService dataBroker,
      CacheService cacheService,
      PostgresService postgresService);

  /**
   * delete a adapter.
   *
   * @param adapterId adapter id to be deleted
   * @return Future
   */
  Future<JsonObject> deleteAdapter(
      String adapterId,
      String userId,
      DataBrokerService dataBroker,
      PostgresService postgresService);

  /**
   * get adapter details.
   *
   * @param adapterId adapter id to get details
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> getAdapterDetails(String adapterId, DataBrokerService databroker);

  /**
   * publish heartbeat data.
   *
   * @param json request json
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> publishHeartbeat(JsonObject json, DataBrokerService databroker);

  /**
   * publish data from adapter.
   *
   * @param json request json
   * @param databroker DataBrokerService object
   * @return Future
   */
  Future<JsonObject> publishDataFromAdapter(JsonArray json, DataBrokerService databroker);

  /**
   * publish all adapter from exchange.
   *
   * @param json request json
   * @param postgresService PostgresService object
   * @return Future
   */
  Future<JsonObject> getAllAdapterDetailsForUser(JsonObject json, PostgresService postgresService);
}
