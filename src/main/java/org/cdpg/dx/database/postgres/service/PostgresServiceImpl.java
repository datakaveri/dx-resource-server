package org.cdpg.dx.database.postgres.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.List;
import java.util.stream.Collectors;
import org.cdpg.dx.database.postgres.models.*;

public class PostgresServiceImpl implements PostgresService {
    private final PgPool client;

    public PostgresServiceImpl(PgPool client) {
        this.client = client;
    }

    private QueryResult convertToQueryResult(RowSet<Row> rowSet) {
        List<JsonObject> rows = rowSet.stream().map(row -> {
            JsonObject json = new JsonObject();
            for (int i = 0; i < row.size(); i++) {
                json.put(row.getColumnName(i), row.getValue(i));
            }
            return json;
        }).collect(Collectors.toList());
        return new QueryResult(rows, rows.size(), false);
    }

    private Future<QueryResult> executeQuery(String sql, List<Object> params) {
        return client.preparedQuery(sql).execute(Tuple.from(params))
            .map(this::convertToQueryResult);
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
    public Future<QueryResult> search(SelectQuery query, int limit, int offset) {
        return executeQuery(query.toSQL() + " LIMIT " + limit + " OFFSET " + offset, query.getQueryParams());
    }

    @Override
    public Future<QueryResult> delete(DeleteQuery query) {
        return executeQuery(query.toSQL(), query.getQueryParams());
    }

    @Override
    public Future<QueryResult> select(SelectQuery query) {
        return null;
    }
}
