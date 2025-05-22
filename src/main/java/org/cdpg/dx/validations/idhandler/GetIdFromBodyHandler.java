package org.cdpg.dx.validations.idhandler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.util.RoutingContextHelper;

import static org.cdpg.dx.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;

public class GetIdFromBodyHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LogManager.getLogger(GetIdFromBodyHandler.class);

    public static String getIdFromBody(RoutingContext routingContext) {
        JsonObject body = routingContext.body().asJsonObject();
        JsonArray entities = body.getJsonArray("entities");
        if (entities == null || entities.isEmpty()) {
            return null;
        }

        Object firstEntity = entities.getValue(0);
        if (firstEntity instanceof JsonObject) {
            return ((JsonObject) firstEntity).getString("id");
        } else if (firstEntity instanceof String) {
            return (String) firstEntity;
        }

        return null;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.debug("Info : path {}", RoutingContextHelper.getRequestPath(routingContext));
        String id = getIdFromBody(routingContext);
        if (id != null) {
            LOGGER.info("id :" + id);
            RoutingContextHelper.setId(routingContext, id);
            routingContext.next();
        } else {
            LOGGER.error("Error : Id not Found");
            routingContext.fail(new DxNotFoundException(RESOURCE_NOT_FOUND_URN.getMessage()));
        }
    }
}
