package iudx.resource.server.apiserver.subscription.controller;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SubscriptionServerRestApi {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionServerRestApi.class);
  private final Router router;
  private Vertx vertx;
  private Api api;

  public SubscriptionServerRestApi(Vertx vertx, Router router, Api api) {
    this.vertx = vertx;
    this.router = router;
    this.api = api;
  }

  public void init() {

    router.post(api.getSubscriptionUrl()).handler(this::postSubscriptions);

    router.patch(api.getSubscriptionUrl() + "/:userid/:alias").handler(this::appendSubscription);

    router.put(api.getSubscriptionUrl() + "/:userid/:alias").handler(this::updateSubscription);

    router.get(api.getSubscriptionUrl() + "/:userid/:alias").handler(this::getSubscription);

    router.get(api.getSubscriptionUrl()).handler(this::getAllSubscriptionForUser);

    router.delete(api.getSubscriptionUrl() + "/:userid/:alias").handler(this::deleteSubscription);

    LOGGER.debug("----------------------------------- sfdgfdff ------------------------");
  }

  private void appendSubscription(RoutingContext routingContext) {
    LOGGER.debug(
        "----------------------------------- started  append subscription------------------------");
  }

  private void updateSubscription(RoutingContext routingContext) {
    LOGGER.debug(
        "----------------------------------- started  update subscription------------------------");
  }

  private void getSubscription(RoutingContext routingContext) {
    LOGGER.debug(
        "----------------------------------- started  get subscription------------------------");
  }

  private void getAllSubscriptionForUser(RoutingContext routingContext) {
    LOGGER.debug(
        "----------------------------------- started  get all subscription------------------------");
  }

  private void deleteSubscription(RoutingContext routingContext) {

    LOGGER.debug(
        "----------------------------------- started  delete subscription------------------------");
  }

  private void postSubscriptions(RoutingContext routingContext) {
    LOGGER.debug(
        "----------------------------------- started  put subscription------------------------");
  }
}
