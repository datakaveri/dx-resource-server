package iudx.resource.server.apiserver.ingestion.controller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.RouteBuilder;
import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IngestionServerRestApi extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(IngestionServerRestApi.class);
    private final Router router = RouteBuilder.getRouter();

    @Override
    public void start() throws Exception {
        LOGGER.debug("Starting ingestion ");
        router.post(config().getString("dxApiBasePath")+"/ingestion").handler(this::handleIngestion);
    }

    @Override
    public void stop() throws Exception {
        LOGGER.debug("Shutting down");
    }

    private void handleIngestion(RoutingContext routingContext) {
        LOGGER.debug("- handleIngestion() started -" );

    }
}
