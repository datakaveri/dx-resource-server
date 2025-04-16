package org.cdpg.dx.uniqueattribute.client;

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

public class UniqueAttributeClientImpl implements UniqueAttributeClient {
  private static final Logger LOGGER = LogManager.getLogger(UniqueAttributeClientImpl.class);
  private final PostgresService postgresService;

  public UniqueAttributeClientImpl(PostgresService postgresService) {
    this.postgresService = postgresService;
  }

  @Override
  public Future<List<JsonObject>> fetchUniqueAttribute() {
    Promise<List<JsonObject>> promise = Promise.promise();
    populateUniqueAttribute().onSuccess(promise::complete).onFailure(promise::fail);
    return promise.future();
  }

  private Future<List<JsonObject>> populateUniqueAttribute() {
    LOGGER.trace("populateUniqueAttribute() called");
    Promise<List<JsonObject>> promise = Promise.promise();
    SelectQuery selectQuery =
        new SelectQuery("unique_attributes", List.of("*"), null, null, null, null, null);
    postgresService
        .select(selectQuery)
        .onSuccess(
            pgSuccess -> {
              LOGGER.trace("populateUniqueAttribute() success");
              List<JsonObject> resultJson = pgSuccess.getRows().getList();
              promise.complete(resultJson);
            })
        .onFailure(
            failure -> {
              LOGGER.error("failed to populate unique attribute from postgres", failure);
              promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
            });
    return promise.future();
  }
}
