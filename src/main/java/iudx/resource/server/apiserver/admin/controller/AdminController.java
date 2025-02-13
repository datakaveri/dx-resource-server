package iudx.resource.server.apiserver.admin.controller;

import static iudx.resource.server.apiserver.admin.util.Constants.*;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdminController {
  private static final Logger LOGGER = LogManager.getLogger(AdminController.class);
  private final Router router;
  private Vertx vertx;
  private Api api;

  public AdminController(Vertx vertx, Router router, Api api) {
    this.vertx = vertx;
    this.router = router;
    this.api = api;
  }

  public void init() {
    router.post(ADMIN + REVOKE_TOKEN).handler(this::handleRevokeTokenRequest);

    router.post(ADMIN + RESOURCE_ATTRIBS).handler(this::createUniqueAttribute);

    router.put(ADMIN + RESOURCE_ATTRIBS).handler(this::updateUniqueAttribute);

    router.delete(ADMIN + RESOURCE_ATTRIBS).handler(this::deleteUniqueAttribute);
  }

  private void createUniqueAttribute(RoutingContext routingContext) {}

  private void updateUniqueAttribute(RoutingContext routingContext) {}

  private void deleteUniqueAttribute(RoutingContext routingContext) {}

  private void handleRevokeTokenRequest(RoutingContext routingContext) {}
}
