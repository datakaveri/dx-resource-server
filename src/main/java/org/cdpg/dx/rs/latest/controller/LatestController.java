package org.cdpg.dx.rs.latest.controller;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auth.authorization.exception.AuthorizationException;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.common.ResponseUrn;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.database.redis.service.RedisService;
import org.cdpg.dx.revoked.service.RevokedService;
import org.cdpg.dx.rs.apiserver.ApiController;
import org.cdpg.dx.rs.authorization.handler.ResourcePolicyAuthorizationHandler;
import org.cdpg.dx.rs.latest.model.LatestData;
import org.cdpg.dx.rs.latest.service.LatestService;
import org.cdpg.dx.rs.latest.service.LatestServiceImpl;
import org.cdpg.dx.uniqueattribute.service.UniqueAttributeService;
import org.cdpg.dx.util.RoutingContextHelper;
import org.cdpg.dx.validations.idhandler.GetIdFromPathHandler;

import java.util.Optional;

import static org.cdpg.dx.util.Constants.*;

/**
 * Controller to handle latest entity data retrieval endpoints.
 */
public class LatestController implements ApiController {
    private static final Logger LOGGER = LogManager.getLogger(LatestController.class);

    private final LatestService latestService;
    private ClientRevocationValidationHandler clientRevocationValidationHandler;
    private ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler;
    private GetIdFromPathHandler getIdFromPathHandler=new GetIdFromPathHandler();

    /**
     * Initializes the latest controller with required services and config.
     */
    public LatestController(RedisService redisService, String tenantPrefix,
                            UniqueAttributeService uniqueAttributeService, RevokedService revokedService, CatalogueService catalogueService) {
        this.latestService = new LatestServiceImpl(redisService, tenantPrefix, uniqueAttributeService);
        this.clientRevocationValidationHandler=new ClientRevocationValidationHandler(revokedService);
        this.resourcePolicyAuthorizationHandler=new ResourcePolicyAuthorizationHandler(catalogueService);
    }

    @Override
    public void register(RouterBuilder builder) {
        builder.operation(GET_LATEST_ENTITY_DATA)
                .handler(getIdFromPathHandler)
                .handler(clientRevocationValidationHandler)
                .handler(resourcePolicyAuthorizationHandler)
                .handler(this::roleAccessValidation)
                .handler(this::handleLatestSearchQuery)
                .failureHandler(this::handleFailure);

        LOGGER.debug("Latest Controller deployed and route registered.");
    }

    private void handleLatestSearchQuery(RoutingContext ctx) {
        LOGGER.debug("Handling latest data query");
        String id = ctx.pathParam(ID);

        latestService.getLatestData(id)
                .onSuccess(result -> sendResponse(ctx, result))
                .onFailure(err -> {
                    LOGGER.error("Error processing latest data request for ID: {}", id, err);
                    ctx.fail(err);
                });
    }

    private void sendResponse(RoutingContext ctx, LatestData latestData) {
        if (latestData.getLatestData().isEmpty()) {
            ctx.response()
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(204)
                    .end();
        } else {
            JsonObject response = new JsonObject()
                    .put(JSON_TYPE, ResponseUrn.SUCCESS_URN.getUrn())
                    .put(JSON_TITLE, ResponseUrn.SUCCESS_URN.getMessage())
                    .put(JSON_RESULTS, latestData.getLatestData());

            ctx.response()
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(200)
                    .end(response.encode());
        }
    }

    private void handleFailure(RoutingContext ctx) {
        int statusCode = ctx.statusCode() >= 400 ? ctx.statusCode() : 500;
        Throwable failure = ctx.failure();
        String errorMessage = failure != null ? failure.getMessage() : "Unknown error occurred";

        LOGGER.warn("Request failed with status {}: {}", statusCode, errorMessage);

        JsonObject errorResponse = new JsonObject()
                .put("error", errorMessage)
                .put("status", statusCode);

        ctx.response()
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(statusCode)
                .end(errorResponse.encode());
    }
    public void roleAccessValidation(RoutingContext routingContext) {
        Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
        if (!"consumer".equalsIgnoreCase(jwtData.get().role())) {
            routingContext.next();
        }
        boolean hasSubAccess =
                jwtData.get().cons().getJsonArray("access", new JsonArray()).contains("api");
        if (!hasSubAccess) {
            LOGGER.error("Role validation failed");
            routingContext.fail(new AuthorizationException("Role validation failed"));
        }
        routingContext.next();
    }
}