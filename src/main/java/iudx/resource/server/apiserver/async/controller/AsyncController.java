package iudx.resource.server.apiserver.async.controller;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AsyncController {
  private static final Logger LOGGER = LogManager.getLogger(AsyncController.class);
  private final Router router;
  private Vertx vertx;
  private Api api;

  public AsyncController(Vertx vertx, Router router, Api api) {
    this.vertx = vertx;
    this.router = router;
    this.api = api;
  }

  public void init() {
    router.get(api.getIudxAsyncSearchApi()).handler(this::asyncSearchRequest);
    router.get(api.getIudxAsyncStatusApi()).handler(this::asyncStatusRequest);
  }

  private void asyncStatusRequest(RoutingContext routingContext) {}

  private void asyncSearchRequest(RoutingContext routingContext) {}
}
