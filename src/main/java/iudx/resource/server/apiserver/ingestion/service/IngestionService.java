package iudx.resource.server.apiserver.ingestion.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import iudx.resource.server.apiserver.ingestion.model.GetResultModel;
import iudx.resource.server.apiserver.ingestion.model.IngestionEntitiesResponseModel;
import iudx.resource.server.database.postgres.model.PostgresResultModel;
import org.cdpg.dx.databroker.model.RegisterExchangeModel;

public interface IngestionService {
  Future<RegisterExchangeModel> registerAdapter(String entities, String instanceId, String userId);

  Future<Void> deleteAdapter(String adapterId, String userId);

  Future<GetResultModel> getAdapterDetails(String adapterId);

  Future<IngestionEntitiesResponseModel> publishDataFromAdapter(JsonArray json);

  Future<PostgresResultModel> getAllAdapterDetailsForUser(String iid);
}
