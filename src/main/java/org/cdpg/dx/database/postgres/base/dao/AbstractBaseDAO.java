package org.cdpg.dx.database.postgres.base.dao;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.DxPgException;
import org.cdpg.dx.common.exception.NoRowFoundException;
import org.cdpg.dx.database.postgres.base.enitty.BaseEntity;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.database.postgres.util.DxPgExceptionMapper;

public abstract class AbstractBaseDAO<T extends BaseEntity<T>> implements BaseDAO<T> {

  private static final Logger LOGGER = LogManager.getLogger(AbstractBaseDAO.class);
  protected final PostgresService postgresService;
  protected final String tableName;
  protected final String idFileld;
  protected final Function<JsonObject, T> fromJson;

  public AbstractBaseDAO(
      PostgresService postgresService,
      String tableName,
      String idFileld,
      Function<JsonObject, T> fromJson) {
    this.postgresService = postgresService;
    this.tableName = tableName;
    this.fromJson = fromJson;
    this.idFileld = idFileld;
  }

  @Override
  public Future<T> create(T entity) {
    var dataMap = entity.toNonEmptyFieldsMap();
    InsertQuery query =
        new InsertQuery(tableName, List.copyOf(dataMap.keySet()), List.copyOf(dataMap.values()));

    return postgresService
        .insert(query)
        .compose(
            result -> {
              if (result.getRows().isEmpty()) {
                return Future.failedFuture("Insert query returned no rows.");
              }
              return Future.succeededFuture(fromJson.apply(result.getRows().getJsonObject(0)));
            })
        .recover(
            err -> {
              LOGGER.error("Error inserting to {}: msg: {}", tableName, err.getMessage(), err);
              return Future.failedFuture(err);
            });
  }

  @Override
  public Future<T> get(UUID id) {
    Condition condition =
        new Condition(idFileld, Condition.Operator.EQUALS, List.of(id.toString()));
    SelectQuery query = new SelectQuery(tableName, List.of("*"), condition, null, null, null, null);

    return postgresService
        .select(query)
        .compose(
            result -> {
              if (result.getRows().isEmpty()) {
                return Future.failedFuture("Select query returned no rows id :" + id);
              }
              return Future.succeededFuture(fromJson.apply(result.getRows().getJsonObject(0)));
            })
        .recover(
            err -> {
              LOGGER.error(
                  "Error fetching  from {} ,with ID {}: mesg{}",
                  tableName,
                  id,
                  err.getMessage(),
                  err);
              return Future.failedFuture(DxPgExceptionMapper.from(err));
            });
  }

  @Override
  public Future<List<T>> getAll() {
    SelectQuery query = new SelectQuery(tableName, List.of("*"), null, null, null, null, null);

    return postgresService
        .select(query)
        .compose(
            result -> {
              List<T> entities =
                  result.getRows().stream()
                      .map(row -> fromJson.apply((JsonObject) row))
                      .collect(Collectors.toList());
              return Future.succeededFuture(entities);
            })
        .recover(
            err -> {
              LOGGER.error(
                  "Error fetching all from: {}, msg: {}", tableName, err.getMessage(), err);
              return Future.failedFuture(DxPgExceptionMapper.from(err));
            });
  }

  @Override
  public Future<List<T>> getAllWithFilters(Map<String, Object> filters) {
    Condition condition =
        filters.entrySet().stream()
            .map(e -> new Condition(e.getKey(), Condition.Operator.EQUALS, List.of(e.getValue())))
            .reduce((c1, c2) -> new Condition(List.of(c1, c2), Condition.LogicalOperator.AND))
            .orElse(null);

    SelectQuery query = new SelectQuery(tableName, List.of("*"), condition, null, null, null, null);

    return postgresService
        .select(query)
        .compose(
            result -> {
              List<T> entities =
                  result.getRows().stream()
                      .map(row -> fromJson.apply((JsonObject) row))
                      .collect(Collectors.toList());
              return Future.succeededFuture(entities);
            })
        .recover(
            err -> {
              LOGGER.error(
                  "Error fetching all from: {}, msg: {}", tableName, err.getMessage(), err);
              return Future.failedFuture(DxPgExceptionMapper.from(err));
            });
  }

  @Override
  public Future<Boolean> update(
      Map<String, Object> conditionMap, Map<String, Object> updateDataMap) {
    Condition condition =
        conditionMap.entrySet().stream()
            .map(e -> new Condition(e.getKey(), Condition.Operator.EQUALS, List.of(e.getValue())))
            .reduce((c1, c2) -> new Condition(List.of(c1, c2), Condition.LogicalOperator.AND))
            .orElse(null);

    List<String> columns = new ArrayList<>(updateDataMap.keySet());
    List<Object> values = new ArrayList<>(updateDataMap.values());
    UpdateQuery query = new UpdateQuery(tableName, columns, values, condition, null, null);

    return postgresService
        .update(query)
        .compose(
            result -> {
              if (!result.isRowsAffected()) {
                return Future.failedFuture(new NoRowFoundException("No rows updated for"));
              }
              return Future.succeededFuture(true);
            })
        .recover(
            err -> {
              LOGGER.error("Error updating  in {} : msg{}", tableName, err.getMessage(), err);
              return Future.failedFuture(DxPgException.from(err));
            });
  }

  @Override
  public Future<Boolean> delete(UUID id) {
    Condition condition =
        new Condition(idFileld, Condition.Operator.EQUALS, List.of(id.toString()));
    DeleteQuery query = new DeleteQuery(tableName, condition, null, null);

    return postgresService
        .delete(query)
        .compose(
            result -> {
              if (!result.isRowsAffected()) {
                return Future.failedFuture(
                    new NoRowFoundException(
                        "No rows deleted from : " + tableName + " for id : " + id));
              }
              return Future.succeededFuture(true);
            })
        .recover(
            err -> {
              LOGGER.error(
                  "Error deleting from {} with ID {}: msg{}", tableName, id, err.getMessage(), err);
              return Future.failedFuture(DxPgExceptionMapper.from(err));
            });
  }
}
