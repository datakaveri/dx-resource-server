package org.cdpg.dx.database.postgres.service;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.cdpg.dx.database.postgres.models.*;

@VertxGen
@ProxyGen
public interface PostgresService {

    Future<QueryResult> insert(InsertQuery query);
    Future<QueryResult> update(UpdateQuery query);
    Future<QueryResult> delete(DeleteQuery query);
    Future<QueryResult> select(SelectQuery query);

    static PostgresService createProxy(Vertx vertx, String address) {
        return new PostgresServiceVertxEBProxy(vertx, address);
    }
}
