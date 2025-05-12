package org.cdpg.dx.rs.ingestion.dao;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import org.cdpg.dx.database.postgres.base.dao.BaseDAO;
import org.cdpg.dx.database.postgres.models.QueryResult;
import org.cdpg.dx.rs.ingestion.model.IngestionDTO;

public interface IngestionDAO extends BaseDAO<IngestionDTO> {
    Future<QueryResult> getAllAdaptersDetailsByProviderId(String providerId);
    Future<Void> deleteAdapterByExchangeName(String exchangeName);
    Future<JsonArray> getAdapterDetailsByExchangeName(String exchangeName);
    Future<Void> insertAdapterDetails(IngestionDTO ingestionDTO);
}
