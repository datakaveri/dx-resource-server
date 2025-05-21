package org.cdpg.dx.rs.admin.dao.impl;

import static org.cdpg.dx.rs.admin.util.Constants.REVOKED_TOKEN_TABLE;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import java.time.LocalDateTime;
import java.util.List;
import org.cdpg.dx.database.postgres.base.dao.AbstractBaseDAO;
import org.cdpg.dx.database.postgres.models.Condition;
import org.cdpg.dx.database.postgres.models.InsertQuery;
import org.cdpg.dx.database.postgres.models.SelectQuery;
import org.cdpg.dx.database.postgres.models.UpdateQuery;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.rs.admin.dao.RevokedTokenServiceDAO;
import org.cdpg.dx.rs.admin.model.RevokedTokenDTO;

public class RevokedTokenServiceDAOImpl extends AbstractBaseDAO<RevokedTokenDTO>
    implements RevokedTokenServiceDAO {
  public RevokedTokenServiceDAOImpl(PostgresService postgresService) {
    super(postgresService, REVOKED_TOKEN_TABLE, "_id", RevokedTokenDTO::fromJson);
  }

  @Override
  public Future<JsonArray> getRevokedTokensByUserId(String userId) {
    Promise<JsonArray> promise = Promise.promise();
    Condition condition = new Condition("_id", Condition.Operator.EQUALS, List.of(userId));
    SelectQuery selectQuery =
        new SelectQuery(tableName, List.of("*"), condition, null, null, null, null);

    postgresService
        .select(selectQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                JsonArray result = pgHandler.result().getRows();
                promise.complete(result);
              } else {
                promise.fail(pgHandler.cause());
              }
            });
    return promise.future();
  }

  @Override
  public Future<JsonArray> insertRevokedToken(RevokedTokenDTO revokedTokenDTO) {
    Promise<JsonArray> promise = Promise.promise();
    var columnNameAndValues = revokedTokenDTO.toNonEmptyFieldsMap();
    postgresService
        .insert(
            new InsertQuery(
                tableName,
                List.copyOf(columnNameAndValues.keySet()),
                List.copyOf(columnNameAndValues.values())))
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                JsonArray result = pgHandler.result().getRows();
                promise.complete(result);
              } else {
                promise.fail(pgHandler.cause());
              }
            });

    return promise.future();
  }

  @Override
  public Future<JsonArray> updateRevokedToken(String userId) {
    Promise<JsonArray> promise = Promise.promise();

    Condition condition = new Condition("_id", Condition.Operator.EQUALS, List.of(userId));
    UpdateQuery updateQuery =
        new UpdateQuery(
            "revoked_tokens",
            List.of("expiry"),
            List.of(LocalDateTime.now().toString()),
            condition,
            null,
            null);

    postgresService
        .update(updateQuery)
        .onComplete(
            pgHandler -> {
              if (pgHandler.succeeded()) {
                JsonArray result = pgHandler.result().getRows();
                promise.complete(result);
              } else {
                promise.fail(pgHandler.cause());
              }
            });

    return promise.future();
  }
}
