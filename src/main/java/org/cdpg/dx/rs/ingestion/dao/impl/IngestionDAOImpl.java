package org.cdpg.dx.rs.ingestion.dao.impl;

import static org.cdpg.dx.rs.ingestion.util.Constants.INGESTION_TABLE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.base.dao.AbstractBaseDAO;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.rs.ingestion.dao.IngestionDAO;
import org.cdpg.dx.rs.ingestion.model.IngestionDTO;

public class IngestionDAOImpl extends AbstractBaseDAO<IngestionDTO> implements IngestionDAO {
  private static final Logger LOGGER = LogManager.getLogger(IngestionDAOImpl.class);

  public IngestionDAOImpl(PostgresService postgresService) {
    super(postgresService, INGESTION_TABLE, "_id", IngestionDTO::fromJson);
  }

  @Override
  public Future<List<JsonObject>> getAllAdaptersDetailsByProviderId(String providerId) {
    Promise<List<JsonObject>> promise = Promise.promise();
    Condition conditionComponent =
        new Condition("providerid", Condition.Operator.EQUALS, List.of(providerId));
    SelectQuery selectQuery =
        new SelectQuery(tableName, List.of("*"), conditionComponent, null, null, null, null);
    postgresService
        .select(selectQuery)
        .onSuccess(
            result -> {
              List<JsonObject> resultJson = new ArrayList<>();
              for (int i = 0; i < result.getRows().size(); i++) {
                resultJson.add(result.getRows().getJsonObject(i));
              }
              promise.complete(resultJson);
            })
        .onFailure(promise::fail);

    return promise.future();
  }

  @Override
  public Future<Void> deleteAdapterByExchangeName(String exchangeName) {
    Promise<Void> promise = Promise.promise();
    Condition exchangeCondition =
        new Condition("exchange_name", Condition.Operator.EQUALS, List.of(exchangeName));
    DeleteQuery deleteQuery = new DeleteQuery(tableName, exchangeCondition, null, null);
    postgresService
        .delete(deleteQuery)
        .onSuccess(
            result -> {
              promise.complete();
            })
        .onFailure(promise::fail);
    return promise.future();
  }

  @Override
  public Future<JsonArray> getAdapterDetailsByExchangeName(String exchangeName) {
    Promise<JsonArray> promise = Promise.promise();
    Condition conditionComponent =
        new Condition("exchange_name", Condition.Operator.EQUALS, List.of(exchangeName));
    SelectQuery selectQuery =
        new SelectQuery(tableName, List.of("*"), conditionComponent, null, null, null, null);

    postgresService
        .select(selectQuery)
        .onSuccess(
            result -> {
              promise.complete(result.getRows());
            })
        .onFailure(promise::fail);
    return promise.future();
  }

  @Override
  public Future<Void> insertAdapterDetails(IngestionDTO ingestionDTO) {
    Promise<Void> promise = Promise.promise();
    Map<String, Object> map = ingestionDTO.toNonEmptyFieldsMap();
    InsertQuery insertQuery =
        new InsertQuery(tableName, List.copyOf(map.keySet()), List.copyOf(map.values()));
    postgresService
        .insert(insertQuery)
        .onSuccess(
            result -> {
              promise.complete();
            })
        .onFailure(promise::fail);
    return promise.future();
  }
}
