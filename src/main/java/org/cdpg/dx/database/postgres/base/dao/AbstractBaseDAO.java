package org.cdpg.dx.database.postgres.base.dao;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.base.enitty.BaseEntity;
import org.cdpg.dx.database.postgres.models.Condition;
import org.cdpg.dx.database.postgres.models.DeleteQuery;
import org.cdpg.dx.database.postgres.models.InsertQuery;
import org.cdpg.dx.database.postgres.models.SelectQuery;
import org.cdpg.dx.database.postgres.service.PostgresService;

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
                return Future.failedFuture("Select query returned no rows id :" + id.toString());
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
              return Future.failedFuture(err);
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
              return Future.failedFuture(err);
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
                    "No rows updated when deleting from : " + tableName + " for id : " + id);
              }
              return Future.succeededFuture(true);
            })
        .recover(
            err -> {
              LOGGER.error(
                  "Error deleting from {} with ID {}: msg{}", tableName, id, err.getMessage(), err);
              return Future.failedFuture(err);
            });
  }
}
