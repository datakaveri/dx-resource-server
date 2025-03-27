package iudx.resource.server.database.elastic.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import iudx.resource.server.database.elastic.model.ElasticsearchResponse;
import iudx.resource.server.database.elastic.model.QueryModel;

import java.util.List;

@VertxGen
@ProxyGen
public interface ElasticsearchService {

  @GenIgnore
  static ElasticsearchService createProxy(Vertx vertx, String address) {
    return new ElasticsearchServiceVertxEBProxy(vertx, address);
  }

  Future<List<ElasticsearchResponse>> search(String index, QueryModel queryModel);

  Future<Integer> count(String index, QueryModel queryModel);
}
