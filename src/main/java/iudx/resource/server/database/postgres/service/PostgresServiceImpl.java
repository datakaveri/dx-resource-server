package iudx.resource.server.database.postgres.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PostgresServiceImpl implements PostgresService {

  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);

  private final PgPool client;

  public PostgresServiceImpl(final PgPool pgclient) {
    this.client = pgclient;
  }

  @Override
  public Future<JsonObject> executeQuery(final String query) {

    Promise<JsonObject> promise = Promise.promise();

    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());

    client
        .withConnection(
            connection ->
                connection.query(query).collecting(rowCollector).execute().map(row -> row.value()))
        .onSuccess(
            successHandler -> {
              JsonArray result = new JsonArray(successHandler);
              JsonObject responseJson =
                  new JsonObject()
                      .put("type", ResponseUrn.SUCCESS_URN.getUrn())
                      .put("title", ResponseUrn.SUCCESS_URN.getMessage())
                      .put("result", result);
              promise.complete(responseJson);
            })
        .onFailure(
            failureHandler -> {
              LOGGER.debug(failureHandler);
              Response response =
                  new Response.Builder()
                      .withUrn(ResponseUrn.DB_ERROR_URN.getUrn())
                      .withStatus(HttpStatus.SC_BAD_REQUEST)
                      .withDetail(failureHandler.getLocalizedMessage())
                      .build();
              promise.fail(response.toString());
            });
    return promise.future();
  }

  // TODO : prepared query works only for String parameters, due to service proxy restriction with
  // allowed type as arguments. needs to work with TupleBuilder class which will parse other types
  // like date appropriately to match with postgres types
  @Override
  public Future<JsonObject> executePreparedQuery(final String query, final JsonObject queryParams) {

    Promise<JsonObject> promise = Promise.promise();

    List<Object> params = new ArrayList<Object>(queryParams.getMap().values());

    Tuple tuple = Tuple.from(params);

    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());

    client
        .withConnection(
            connection ->
                connection
                    .preparedQuery(query)
                    .collecting(rowCollector)
                    .execute(tuple)
                    .map(rows -> rows.value()))
        .onSuccess(
            successHandler -> {
              JsonArray response = new JsonArray(successHandler);
              promise.complete(new JsonObject().put("result", response));
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(failureHandler);
              Response response =
                  new Response.Builder()
                      .withUrn(ResponseUrn.DB_ERROR_URN.getUrn())
                      .withStatus(HttpStatus.SC_BAD_REQUEST)
                      .withDetail(failureHandler.getLocalizedMessage())
                      .build();
              promise.fail(response.toString());
            });
    return promise.future();
  }
}
