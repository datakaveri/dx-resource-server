package org.cdpg.dx.revoked.client;

import static org.cdpg.dx.common.ErrorCode.ERROR_INTERNAL_SERVER;
import static org.cdpg.dx.common.ErrorMessage.INTERNAL_SERVER_ERROR;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.postgres.models.SelectQuery;
import org.cdpg.dx.database.postgres.service.PostgresService;

public class RevokedClientImpl implements RevokedClient {
  private static final Logger LOGGER = LogManager.getLogger(RevokedClientImpl.class);
  private final PostgresService postgresService;

  public RevokedClientImpl(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  @Override
  public Future<List<JsonObject>> fetchRevokedData() {
    Promise<List<JsonObject>> promise = Promise.promise();
    populateRevoked().onSuccess(promise::complete).onFailure(promise::fail);
    return promise.future();
  }

  // TODO :: need to revisit code once postgres model will be available
  private Future<List<JsonObject>> populateRevoked() {
    LOGGER.trace("populateRevoked() called");
    Promise<List<JsonObject>> promise = Promise.promise();
    SelectQuery selectQuery =
        new SelectQuery("revoked_tokens", List.of("*"), null, null, null, null, null);
    postgresService
        .select(selectQuery)
        .onSuccess(
            pgSuccess -> {
              LOGGER.trace("populateRevoked() success");
              List<JsonObject> resultJson = pgSuccess.rows();
              promise.complete(resultJson);
            })
        .onFailure(
            failure -> {
              LOGGER.error("failed to populate revoked token from postgres", failure);
              promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
            });
    return promise.future();
  }
}
