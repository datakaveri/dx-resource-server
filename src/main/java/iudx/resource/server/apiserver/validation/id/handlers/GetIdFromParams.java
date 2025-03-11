package iudx.resource.server.apiserver.validation.id.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.RoutingContextHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;

public class GetIdFromParams implements Handler<RoutingContext> {
    private static final Logger LOGGER = LogManager.getLogger(GetIdFromParams.class);
    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.debug("Info : path {}", RoutingContextHelper.getRequestPath(routingContext));
        String id = getIdFromParam(routingContext);
        if(id !=null){
            LOGGER.info("id :" + id);
            RoutingContextHelper.setId(routingContext, id);
            routingContext.next();
        }else{
            LOGGER.error("Error : Id not Found");
            HttpStatusCode statusCode = HttpStatusCode.getByValue(404);
            routingContext.response()
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(statusCode.getValue())
                    .end(generateResponse(RESOURCE_NOT_FOUND_URN, statusCode).toString());
        }
    }

    private String getIdFromParam(RoutingContext routingContext) {
        return routingContext.request().getParam(ID);
    }
    private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
        return new JsonObject()
                .put(JSON_TYPE, urn.getUrn())
                .put(JSON_TITLE, statusCode.getDescription())
                .put(JSON_DETAIL, statusCode.getDescription());
    }
}
