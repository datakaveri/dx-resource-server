package org.cdpg.dx.validations.idhandler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.util.RoutingContextHelper;

import static org.cdpg.dx.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;
import static org.cdpg.dx.validations.util.Constants.ID;

public class GetIdFromParams implements Handler<RoutingContext> {
    private static final Logger LOGGER = LogManager.getLogger(GetIdFromParams.class);

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.debug("Info : path {}", RoutingContextHelper.getRequestPath(routingContext));
        String id = getIdFromParam(routingContext);
        if (id != null) {
            LOGGER.info("id :" + id);
            RoutingContextHelper.setId(routingContext, id);
            routingContext.next();
        } else {
            LOGGER.error("Error : Id not Found");
            routingContext.fail(new DxNotFoundException(RESOURCE_NOT_FOUND_URN.getMessage()));
        }
    }

    private String getIdFromParam(RoutingContext routingContext) {
        return routingContext.request().getParam(ID);
    }
}
