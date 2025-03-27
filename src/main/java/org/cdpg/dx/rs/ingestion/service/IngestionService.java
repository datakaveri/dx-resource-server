package org.cdpg.dx.rs.ingestion.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import org.cdpg.dx.database.postgres.models.QueryResult;
import org.cdpg.dx.databroker.model.ExchangeSubscribersResponse;
import org.cdpg.dx.databroker.model.RegisterExchangeModel;

public interface IngestionService {
  Future<RegisterExchangeModel> registerAdapter(String entitiesId, String userId);

  Future<Void> deleteAdapter(String exchangeName, String userId);

  Future<ExchangeSubscribersResponse> getAdapterDetails(String exchangeName);

  Future<Void> publishDataFromAdapter(JsonArray json);

  Future<QueryResult> getAllAdapterDetailsForUser(String iid);
}
