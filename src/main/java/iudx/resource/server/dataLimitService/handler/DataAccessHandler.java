package iudx.resource.server.dataLimitService.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.authenticator.model.AuthInfo;
import iudx.resource.server.common.ContextHelper;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.dataLimitService.model.UserAuthLimitInfo;
import iudx.resource.server.dataLimitService.service.DataAccessLimitService;;
import iudx.resource.server.database.redis.RedisService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.common.HttpStatusCode.INTERNAL_SERVER_ERROR;
import static iudx.resource.server.common.ResponseUrn.LIMIT_EXCEED_URN;

public class DataAccessHandler implements Handler<RoutingContext> {
    private static final Logger LOGGER = LogManager.getLogger(DataAccessHandler.class);
    public boolean isEnableLimit;
    DataAccessLimitService dataAccessLimitService;
    UserAuthLimitInfo userAuthLimitInfo;

    public DataAccessHandler(Vertx vertx, boolean isEnabledLimit, DataAccessLimitService dataAccessLimitService, RedisService redisService) {
        this.isEnableLimit=isEnabledLimit;
        this.dataAccessLimitService = dataAccessLimitService != null ? dataAccessLimitService : new DataAccessLimitService(vertx, isEnabledLimit,redisService);
    }

    private static Response getInternalServerError() {
        return new Response.Builder().withStatus(500).withUrn(ResponseUrn.DB_ERROR_URN.getUrn()).withTitle(INTERNAL_SERVER_ERROR.getDescription()).withDetail("Internal server error").build();
    }

    private static Response limitExceed() {
        return new Response.Builder().withUrn(LIMIT_EXCEED_URN.getUrn()).withStatus(429).withTitle("Too Many Requests").withDetail(LIMIT_EXCEED_URN.getMessage()).build();
    }

    @Override
    public void handle(RoutingContext context) {

        AuthInfo authInfo = ContextHelper.getAuthInfo(context);
        userAuthLimitInfo = new UserAuthLimitInfo(authInfo);
        LOGGER.info("isLimit Enable : {}", isEnableLimit);
        if (validationNotRequiredCheck(authInfo)) {
            context.next();
            return;
        }

        dataAccessLimitService.validateDataAccess(userAuthLimitInfo).onSuccess(validateDataAccessResult -> {
            if (validateDataAccessResult.isWithInLimit()) {
                ContextHelper.putConsumedData(context, validateDataAccessResult.getConsumedDataInfo());
                context.next();
            } else {
                Response response = limitExceed();
                ContextHelper.putResponse(context, response.toJson());
                buildResponse(context);
            }
        }).onFailure(failureHandler -> {
            LOGGER.error("Failed to route {} ", failureHandler.getMessage());
            Response response = getInternalServerError();
//              ContextHelper.putResponse(context, new JsonObject(failureHandler.getMessage()));
            ContextHelper.putResponse(context, response.toJson());
            buildResponse(context);
        });
    }

    private boolean validationNotRequiredCheck(AuthInfo authInfo) {
        LOGGER.info("isLimit Enable 1 : {}",authInfo.getAccessPolicy());
        return !isEnableLimit && authInfo.getAccessPolicy().equalsIgnoreCase("OPEN");
    }

    public void buildResponse(RoutingContext routingContext) {
        routingContext.response().setStatusCode(routingContext.get("statusCode")).end((String) routingContext.get("response"));
    }
}
