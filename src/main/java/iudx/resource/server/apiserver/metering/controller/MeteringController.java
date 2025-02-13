package iudx.resource.server.apiserver.metering.controller;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringController {
  private static final Logger LOGGER = LogManager.getLogger(MeteringController.class);

  private final Router router;
  private Api api;
  private Vertx vertx;

  public MeteringController(Vertx vertx, Router router, Api api) {
    this.api = api;
    this.router = router;
    this.vertx = vertx;
  }

  public void init() {
    router.get(api.getMonthlyOverview()).handler(this::getOverview);
    router.get(api.getSummaryPath()).handler(this::getSummary);
    router.get(api.getIudxConsumerAuditUrl()).handler(this::getConsumerAuditDetail);
    router.get(api.getIudxProviderAuditUrl()).handler(this::getProviderAuditDetail);
  }

  private void getProviderAuditDetail(RoutingContext routingContext) {}

  private void getConsumerAuditDetail(RoutingContext routingContext) {}

  private void getSummary(RoutingContext routingContext) {}

  private void getOverview(RoutingContext routingContext) {}
}
