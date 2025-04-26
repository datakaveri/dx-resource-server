package org.cdpg.dx.rs.subscription;

import static org.cdpg.dx.util.Constants.*;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.rs.apiserver.ApiController;
import org.cdpg.dx.rs.subscription.service.SubscriptionService;
import org.cdpg.dx.rs.subscription.service.SubscriptionServiceImpl;

public class SubsCriptionController implements ApiController {
  private static final Logger LOGGER = LogManager.getLogger(SubsCriptionController.class);

  private final PostgresService pgService;
  private final DataBrokerService brokerService;
  private final CatalogueService catalogueService;
  private final Vertx vertx;
  private SubscriptionService subscriptionService;

  public SubsCriptionController(
      Vertx vertx,
      PostgresService pgService,
      DataBrokerService brokerService,
      CatalogueService catalogueService) {
    this.vertx = vertx;
    this.pgService = pgService;
    this.brokerService = brokerService;
    this.catalogueService = catalogueService;
    this.subscriptionService =
        new SubscriptionServiceImpl(pgService, brokerService, catalogueService);
  }

  @Override
  public void register(RouterBuilder builder) {

    builder
        .operation(GET_SUBSCRIBER_BY_ID)
        .handler(this::handleGetSubscriberById)
        .failureHandler(this::handleFailure);

    builder
        .operation(GET_LIST_OF_SUBSCRIBERS)
        .handler(this::handleGetListOfSubscribers)
        .failureHandler(this::handleFailure);

    builder
        .operation(POST_SUBSCRIPTION)
        .handler(this::handlePostSubscription)
        .failureHandler(this::handleFailure);

    builder
        .operation(UPDATE_SUBSCRIPTION)
        .handler(this::handleUpdateSubscription)
        .failureHandler(this::handleFailure);

    builder
        .operation(APPEND_SUBSCRIPTION)
        .handler(this::handleAppendSubscription)
        .failureHandler(this::handleFailure);

    builder
        .operation(DELETE_SUBSCRIBER_BY_ID)
        .handler(this::handleDeleteSubscriberById)
        .failureHandler(this::handleFailure);
  }

  private void handleGetSubscriberById(RoutingContext ctx) {

    String id = ctx.pathParam("id");
    String name = ctx.pathParam("name");
    String subscriptionId = id + "/" + name;

    LOGGER.info("id {} , name {}", id, name);
    LOGGER.info("subscriptionId {}", subscriptionId);

    subscriptionService
        .getSubscription(subscriptionId)
        .onSuccess(
            subscriber -> {
              LOGGER.debug("subscriber details: {}", subscriber.toString());
              ctx.response()
                  .putHeader("Content-Type", "application/json")
                  .setStatusCode(200)
                  .end(new JsonObject().put("subscriber", subscriber).encode());
            })
        .onFailure(
            err -> {
              ctx.fail(err);
            })
        .onFailure(
            failure -> {
              handleFailure(ctx);
            });
  }

  private void handleGetListOfSubscribers(RoutingContext ctx) {

    subscriptionService
        .getAllSubscriptionQueueForUser("fd47486b-3497-4248-ac1e-082e4d37a66c")
        .onSuccess(
            subscriber -> {
              LOGGER.debug("subscriber details: {}", subscriber.toString());
              ctx.response()
                  .putHeader("Content-Type", "application/json")
                  .setStatusCode(200)
                  .end(new JsonObject().put("subscriber", subscriber).encode());
            })
        .onFailure(
            err -> {
              ctx.fail(err);
            })
        .onFailure(
            failure -> {
              handleFailure(ctx);
            });
  }

  private void handlePostSubscription(RoutingContext ctx) {
    sendDummyResponse(ctx, "Dummy response for postSubscription");
  }

  private void handleUpdateSubscription(RoutingContext ctx) {
    sendDummyResponse(ctx, "Dummy response for updateSubscription");
  }

  private void handleAppendSubscription(RoutingContext ctx) {
    sendDummyResponse(ctx, "Dummy response for appendSubscription");
  }

  private void handleDeleteSubscriberById(RoutingContext ctx) {
    sendDummyResponse(ctx, "Dummy response for deleteSubscriberById");
  }

  private void sendDummyResponse(RoutingContext ctx, String message) {
    ctx.response()
        .putHeader("Content-Type", "application/json")
        .setStatusCode(200)
        .end(new JsonObject().put("message", message).encode());
  }

  private void handleFailure(RoutingContext ctx) {
    Throwable failure = ctx.failure();
    int statusCode = ctx.statusCode();

    if (statusCode < 400) {
      // Default to 500 if not set
      statusCode = 500;
    }

    String message = failure != null ? failure.getMessage() : "Unknown error occurred";

    ctx.response()
        .putHeader("Content-Type", "application/json")
        .setStatusCode(statusCode)
        .end(new JsonObject().put("error", message).put("status", statusCode).encode());
  }
}
