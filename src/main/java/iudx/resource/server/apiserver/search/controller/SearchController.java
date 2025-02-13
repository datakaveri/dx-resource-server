package iudx.resource.server.apiserver.search.controller;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SearchController {
  private static final Logger LOGGER = LogManager.getLogger(SearchController.class);
  private final Router router;
  private Vertx vertx;
  private Api api;

  public SearchController(Vertx vertx, Router router, Api api) {
    this.vertx = vertx;
    this.router = router;
    this.api = api;
  }

  public void init() {
    router.get(api.getEntitiesUrl()).handler(this::getEntitiesQuery);
    router.get(api.getEntitiesUrl() + "/*").handler(this::getLatestEntitiesQuery);
    router.post(api.getPostTemporalQueryPath()).handler(this::postEntitiesQuery);
    router.post(api.getPostEntitiesQueryPath()).handler(this::postEntitiesQuery);
    router.get(api.getTemporalUrl()).handler(this::getTemporalQuery);
  }

  private void getTemporalQuery(RoutingContext routingContext) {}

  private void postEntitiesQuery(RoutingContext routingContext) {}

  private void getLatestEntitiesQuery(RoutingContext routingContext) {}

  private void getEntitiesQuery(RoutingContext routingContext) {}
}
