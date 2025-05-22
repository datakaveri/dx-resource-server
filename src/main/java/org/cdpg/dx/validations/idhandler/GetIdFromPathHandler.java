package org.cdpg.dx.validations.idhandler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.DxNotFoundException;
import org.cdpg.dx.util.RoutingContextHelper;

import java.util.Map;

import static org.cdpg.dx.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;
import static org.cdpg.dx.validations.util.Constants.*;

public class GetIdFromPathHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LogManager.getLogger(GetIdFromPathHandler.class);

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.debug("Info : path {}", RoutingContextHelper.getRequestPath(routingContext));
        String id = getIdFromPath(routingContext);
        if (id != null) {
            LOGGER.info("id :{}", id);
            RoutingContextHelper.setId(routingContext, id);
            routingContext.next();
        } else {
            LOGGER.error("Error : Id not Found");
            routingContext.fail(new DxNotFoundException(RESOURCE_NOT_FOUND_URN.getMessage()));
        }
    }

    private String getIdFromPath(RoutingContext routingContext) {
        StringBuilder id = null;
        Map<String, String> pathParams = routingContext.pathParams();
        LOGGER.debug("path params :" + pathParams);
        if (pathParams != null && !pathParams.isEmpty()) {
            if (pathParams.containsKey(ID)) {
                id = new StringBuilder(pathParams.get(ID));
                LOGGER.info("API is : {} and path param is : {}", RoutingContextHelper.getRequestPath(routingContext), pathParams);
                LOGGER.debug("id :" + id);
            } else if (pathParams.containsKey(USER_ID) && pathParams.containsKey(JSON_ALIAS)) {
                id = new StringBuilder();
                LOGGER.info("User id and alias name are present : {}, {}", routingContext.request().path(), pathParams);
                id.append(pathParams.get(USER_ID)).append("/").append(pathParams.get(JSON_ALIAS));
            }
        }
        return id != null ? id.toString() : null;
    }
}
