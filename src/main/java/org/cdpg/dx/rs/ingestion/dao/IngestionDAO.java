package org.cdpg.dx.rs.ingestion.dao;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.database.postgres.base.dao.BaseDAO;
import org.cdpg.dx.rs.ingestion.model.IngestionDTO;

import java.util.List;

public interface IngestionDAO extends BaseDAO<IngestionDTO> {
    Future<List<JsonObject>> getAllAdaptersDetailsByProviderId(String providerId);
    Future<Void> deleteAdapterByExchangeName(String exchangeName);
    Future<JsonArray> getAdapterDetailsByExchangeName(String exchangeName);
    Future<Void> insertAdapterDetails(IngestionDTO ingestionDTO);
}
