package org.cdpg.dx.rs.ingestion.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.databroker.model.ExchangeSubscribersResponse;
import org.cdpg.dx.databroker.model.RegisterExchangeModel;
import org.cdpg.dx.rs.ingestion.model.IngestionDTO;

import java.util.List;

public interface IngestionService {
  Future<RegisterExchangeModel> registerAdapter(String entitiesId, String userId);

  Future<Void> deleteAdapter(String exchangeName, String userId);

  Future<ExchangeSubscribersResponse> getAdapterDetails(String exchangeName);

  Future<Void> publishDataFromAdapter(JsonArray json);

  Future<List<JsonObject>> getAllAdapterDetailsForUser(String iid);
}
