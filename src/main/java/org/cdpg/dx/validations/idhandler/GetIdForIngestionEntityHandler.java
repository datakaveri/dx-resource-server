package org.cdpg.dx.validations.idhandler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.DxAuthException;
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.util.RoutingContextHelper;

import java.util.HashSet;
import java.util.Set;

import static org.cdpg.dx.common.ResponseUrn.*;
import static org.cdpg.dx.validations.util.Constants.JSON_ENTITIES;

public class GetIdForIngestionEntityHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LogManager.getLogger(GetIdForIngestionEntityHandler.class);

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.debug("Info : path {}", RoutingContextHelper.getRequestPath(routingContext));
        RequestBody requestBody = routingContext.body();
        try {
            JsonArray requestJsonArray = requestBody.asJsonArray();
            Set<String> entityIds = new HashSet<>();

            for (int i = 0; i < requestJsonArray.size(); i++) {
                JsonObject entity = requestJsonArray.getJsonObject(i);
                entityIds.add(entity.getString("entities"));
            }

            if (entityIds.size() == 1) {
                LOGGER.debug("All entity IDs match: " + entityIds.iterator().next());
                JsonObject body = routingContext.body().asJsonArray().getJsonObject(0);
                String id = body.getJsonArray(JSON_ENTITIES).getString(0);
                if (id != null) {
                    LOGGER.info("id :{}", id);
                    RoutingContextHelper.setId(routingContext, id);
                    routingContext.next();
                } else {
                    LOGGER.error("Error : Id not Found");
                    routingContext.fail(new DxNotFoundException(RESOURCE_NOT_FOUND_URN.getMessage()));
                }
            } else {
                LOGGER.error("Entity IDs do not match: {}", entityIds);
                processAuthFailure(routingContext, "Entity IDs do not match");
            }
        } catch (Exception e) {
            processAuthFailure(routingContext, "Error processing the request body");
        }
    }

    private void processAuthFailure(RoutingContext ctx, String result) {
        if (result.contains("Entity IDs do not match") || result.contains("Error processing the request body")) {
            LOGGER.error("Entity IDs do not match");
            ctx.fail(new DxBadRequestException(BAD_REQUEST_URN.getMessage()));
        } else {
            LOGGER.error("Error : Authentication Failure");
            ctx.fail(new DxAuthException(INVALID_TOKEN_URN.getMessage()));
        }
    }
}
