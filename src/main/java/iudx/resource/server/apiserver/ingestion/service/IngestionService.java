package iudx.resource.server.apiserver.ingestion.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import org.cdpg.dx.database.postgres.models.QueryResult;
import org.cdpg.dx.databroker.model.ExchangeSubscribersResponse;
import org.cdpg.dx.databroker.model.RegisterExchangeModel;

public interface IngestionService {
  Future<RegisterExchangeModel> registerAdapter(String entities, String instanceId, String userId);

  Future<Void> deleteAdapter(String adapterId, String userId);

  Future<ExchangeSubscribersResponse> getAdapterDetails(String adapterId);

  Future<Void> publishDataFromAdapter(JsonArray json);

  Future<QueryResult> getAllAdapterDetailsForUser(String iid);
}
