package iudx.resource.server.apiserver.ingestion.controller;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IngestionController {
  private static final Logger LOGGER = LogManager.getLogger(IngestionController.class);
  private final Router router;
  private Vertx vertx;
  private Api api;

  public IngestionController(Vertx vertx, Router router, Api api) {
    this.router = router;
    this.vertx = vertx;
    this.api = api;
  }

  public void init() {
    router.post(api.getIngestionPath()).handler(this::registerAdapter);
    router.delete(api.getIngestionPath() + "/*").handler(this::deleteAdapter);
    router.get(api.getIngestionPath() + "/:UUID").handler(this::getAdapterDetails);
    router.post(api.getIngestionPath() + "/heartbeat").handler(this::publishHeartbeat);
    router.post(api.getIngestionPathEntities()).handler(this::publishDataFromAdapter);
    router.get(api.getIngestionPath()).handler(this::getAllAdaptersForUsers);
  }

  private void registerAdapter(RoutingContext routingContext) {}

  private void deleteAdapter(RoutingContext routingContext) {}

  private void publishHeartbeat(RoutingContext routingContext) {}

  private void getAdapterDetails(RoutingContext routingContext) {}

  private void publishDataFromAdapter(RoutingContext routingContext) {}

  private void getAllAdaptersForUsers(RoutingContext routingContext) {}
}
