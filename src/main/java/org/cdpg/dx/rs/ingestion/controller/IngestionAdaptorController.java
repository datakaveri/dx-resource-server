package org.cdpg.dx.rs.ingestion.controller;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.revoked.service.RevokedService;
import org.cdpg.dx.rs.apiserver.ApiController;
import org.cdpg.dx.rs.authorization.handler.ResourcePolicyAuthorizationHandler;
import org.cdpg.dx.rs.ingestion.dao.IngestionDAO;
import org.cdpg.dx.rs.ingestion.dao.impl.IngestionDAOImpl;
import org.cdpg.dx.rs.ingestion.service.IngestionService;
import org.cdpg.dx.rs.ingestion.service.IngestionServiceImpl;
import org.cdpg.dx.util.RoutingContextHelper;
import org.cdpg.dx.validations.idhandler.GetIdFromBodyHandler;
import org.cdpg.dx.validations.idhandler.GetIdFromPathHandler;
import org.cdpg.dx.validations.provider.ProviderValidationHandler;

import static org.cdpg.dx.util.Constants.*;

public class IngestionAdaptorController implements ApiController {
    private static final Logger LOGGER = LogManager.getLogger(IngestionAdaptorController.class);
    private ClientRevocationValidationHandler clientRevocationValidationHandler;
    private ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler;
    private IngestionDAO ingestionDAO;
    private IngestionService ingestionService;
    private GetIdFromPathHandler getIdFromPathHandler;
    private GetIdFromBodyHandler getIdFromBodyHandler;
    private ProviderValidationHandler providerValidationHandler;
    public IngestionAdaptorController(DataBrokerService dataBrokerService, RevokedService revokedService, CatalogueService catalogueService, PostgresService postgresService) {
        this.clientRevocationValidationHandler = new ClientRevocationValidationHandler(revokedService);
        this.resourcePolicyAuthorizationHandler = new ResourcePolicyAuthorizationHandler(catalogueService);
        this.ingestionDAO = new IngestionDAOImpl(postgresService);
        this.ingestionService = new IngestionServiceImpl(catalogueService, dataBrokerService, ingestionDAO);
        this.getIdFromPathHandler = new GetIdFromPathHandler();
        this.getIdFromBodyHandler = new GetIdFromBodyHandler();
        this.providerValidationHandler = new ProviderValidationHandler(catalogueService);
    }

    @Override
    public void register(RouterBuilder builder) {
        builder
                .operation(GET_ADAPTER_LIST)
                .handler(clientRevocationValidationHandler)
                .handler(resourcePolicyAuthorizationHandler)
                .handler(providerValidationHandler)
                .handler(this::handleGetAdapterDetails)
                .failureHandler(this::handleFailure);

        builder
                .operation(POST_ATTRIBUTES)
                .handler(getIdFromBodyHandler)
                .handler(clientRevocationValidationHandler)
                .handler(resourcePolicyAuthorizationHandler)
                .handler(providerValidationHandler)
                .handler(this::handleRegisterAdapter)
                .failureHandler(this::handleFailure);

        builder
                .operation(GET_ADAPTOR_BY_ID)
                .handler(getIdFromPathHandler)
                .handler(clientRevocationValidationHandler)
                .handler(resourcePolicyAuthorizationHandler)
                .handler(providerValidationHandler)
                .handler(this::handleGetAdapterDetails)
                .failureHandler(this::handleFailure);

        builder
                .operation(DELETE_ADAPTER_BY_ID)
                .handler(getIdFromPathHandler)
                .handler(clientRevocationValidationHandler)
                .handler(resourcePolicyAuthorizationHandler)
                .handler(providerValidationHandler)
                .handler(this::handleDeleteAdapter)
                .failureHandler(this::handleFailure);
        builder
                .operation(POST_INGESTION_ADAPTER_ENTITIES)
                .handler(getIdFromBodyHandler)
                .handler(providerValidationHandler)
                .handler(clientRevocationValidationHandler)
                .handler(resourcePolicyAuthorizationHandler)
                .handler(this::handlePublishDataFromAdapter)
                .failureHandler(this::handleFailure);
    }

    private void handleGetAdapterDetails(RoutingContext routingContext) {
        LOGGER.debug("Trying to get adapter details");

        String exchangeName = RoutingContextHelper.getId(routingContext);

        ingestionService.getAdapterDetails(exchangeName)
                .onSuccess(result -> {
                    // handle successHandler

                })
                .onFailure(err -> {
                    // handle failureHandler

                });
    }

    private void handlePublishDataFromAdapter(RoutingContext routingContext) {
        LOGGER.debug("Trying to publish data from adapter");


        ingestionService.publishDataFromAdapter(routingContext.getBodyAsJsonArray())
                .onSuccess(result -> {
                    // handle successHandler
                })
                .onFailure(err -> {
                    // handle failureHandler

                });
    }

    private void handleDeleteAdapter(RoutingContext routingContext) {
        LOGGER.debug("Trying to delete adapter");
        String exchangeName = routingContext.request().getParam("exchangeName");
        String id = routingContext.pathParam("id");

        ingestionService.deleteAdapter(exchangeName,id)
                .onSuccess(result -> {
                    // handle successHandler

                })
                .onFailure(err -> {
                    // handle failureHandler

                });
    }

    private void handleRegisterAdapter(RoutingContext routingContext) {
        LOGGER.debug("Trying to register adapter");

        String entitiesId = routingContext.request().getParam("entitiesId");
        String exchangeName = routingContext.request().getParam("exchangeName");
        ingestionService.registerAdapter(entitiesId, exchangeName)
                .onSuccess(result -> {
                    // handle successHandler

                })
                .onFailure(err -> {
                    // handle failureHandler

                });
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
