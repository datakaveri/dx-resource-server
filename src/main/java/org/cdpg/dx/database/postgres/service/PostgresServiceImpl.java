package org.cdpg.dx.database.postgres.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.cdpg.dx.common.exception.DxPgException;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.database.postgres.util.DxPgExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresServiceImpl implements PostgresService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresServiceImpl.class);
    private final PgPool client;

    public PostgresServiceImpl(PgPool client) {
        this.client = client;
    }

    private QueryResult convertToQueryResult(RowSet<Row> rowSet) {
        LOGGER.trace("convertToQueryResult() started");
        JsonArray jsonArray = new JsonArray();
        Object value;
        for (Row row : rowSet) {
            JsonObject json = new JsonObject();
            for (int i = 0; i < row.size(); i++) {
                // json.put(row.getColumnName(i), row.getValue(i));
                String column = row.getColumnName(i);
                value = row.getValue(i);
                if (value == null
                        || value instanceof String
                        || value instanceof Number
                        || value instanceof Boolean) {
                    json.put(column, value);
                } else {
                    json.put(column, value.toString());
                }
            }
            jsonArray.add(json);
        }

        boolean rowsAffected = rowSet.rowCount() > 0; // Check if any rows were affected
        if (rowsAffected) {
            LOGGER.debug("Rows affected :" + rowSet.rowCount());
        } else {
            LOGGER.debug("Rows unaffected");
        }

        QueryResult queryResult = new QueryResult();
        queryResult.setRows(jsonArray);
        queryResult.setTotalCount(rowSet.rowCount());
        queryResult.setHasMore(false);
        queryResult.setRowsAffected(rowsAffected);
        // return new QueryResult(jsonArray, jsonArray.size(), false, rowsAffected);
        return queryResult;
    }

    private Future<QueryResult> executeQuery(String sql, List<Object> params) {
        LOGGER.trace("executeQuery() started");

        try {
            List<Object> coercedParams = new ArrayList<>();

            for (Object param : params) {
                LOGGER.debug(
                        "Param type: {}, value: {}",
                        param != null ? param.getClass().getSimpleName() : "null",
                        param);

                if (param instanceof String paramStr) {

                    // Check if it's an ISO timestamp string
                    if (paramStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
                            || paramStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1,9}$")) {
                        try {
                            // Parse and
                            // qconvert to LocalDateTime
                            LocalDateTime time = LocalDateTime.parse(paramStr);
                            coercedParams.add(time);
                            continue;
                        } catch (Exception e) {
                            LOGGER.error("Failed to parse timestamp, keeping as string: {}", paramStr);
                        }
                    }
                }

                // Default: keep original
                coercedParams.add(param);
            }

            Tuple tuple = Tuple.from(coercedParams);

            return client
                    .preparedQuery(sql)
                    .execute(tuple)
                    .map(rowSet -> {
                        LOGGER.info("Query executed successfully.");
                        return convertToQueryResult(rowSet);
                    })
                    .onFailure(err -> {
                        LOGGER.error("SQL execution error: {}", err.getMessage(), err);
                    })
                    .recover(err -> {
                        LOGGER.warn("Recovering from SQL error: {}", err.toString());
                        // Return a fallback QueryResult or a failed future again
                        return Future.failedFuture(DxPgExceptionMapper.from(err));
                    });

        } catch (Exception e) {
            LOGGER.error("Exception while building Tuple or executing query: {}", e.getMessage());
            return Future.failedFuture(DxPgException.from(e));
        }
    }

    @Override
    public Future<QueryResult> insert(InsertQuery query) {
        return executeQuery(query.toSQL(), query.getQueryParams());
    }

    @Override
    public Future<QueryResult> update(UpdateQuery query) {
        return executeQuery(query.toSQL(), query.getQueryParams());
    }

    @Override
    public Future<QueryResult> delete(DeleteQuery query) {
        return executeQuery(query.toSQL(), query.getQueryParams());
    }

    @Override
    public Future<QueryResult> select(SelectQuery query) {
        return executeQuery(query.toSQL(), query.getQueryParams());
    }
}
