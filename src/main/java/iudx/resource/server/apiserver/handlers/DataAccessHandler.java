package iudx.resource.server.apiserver.handlers;

import static iudx.resource.server.authenticator.Constants.ACCESS_MAP;
import static iudx.resource.server.common.Constants.*;
import static iudx.resource.server.common.HttpStatusCode.*;
import static iudx.resource.server.common.ResponseUrn.LIMIT_EXCEED_URN;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.common.ContextHelper;
import iudx.resource.server.apiserver.common.DataAccessLimitValidator;
import iudx.resource.server.apiserver.common.ValidateDataAccessResult;
import iudx.resource.server.authenticator.model.AuthInfo;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.metering.consumeddata.MeteringService;
import iudx.resource.server.metering.consumeddata.MeteringServiceImpl;
import iudx.resource.server.metering.model.ConsumedDataInfo;
import iudx.resource.server.metering.model.MeteringCountRequest;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataAccessHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(DataAccessHandler.class);
  boolean isEnableLimit;
  PostgresService postgresService;
  MeteringService meteringService;

  public DataAccessHandler(Vertx vertx, boolean isEnabledLimit) {
    this.isEnableLimit = isEnabledLimit;
    this.postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    meteringService = new MeteringServiceImpl(vertx, postgresService);
  }

  @Override
  public void handle(RoutingContext context) {
    AuthInfo authInfo = ContextHelper.getAuthInfo(context);
    LOGGER.info("isLimit Enable : {}", isEnableLimit);
    if (isValidationNotRequired(authInfo)) {
      context.next();
      return;
    }
    validateDataAccess(authInfo)
        .onSuccess(
            validateDataAccessResult -> {
              if(validateDataAccessResult.isWithInLimit()){
                ContextHelper.putConsumedData(context, validateDataAccessResult.getConsumedDataInfo());
                context.next();
              }else{
                Response response = limitExceed();
              }
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("Failed to route {} ", failureHandler.getMessage());
              ContextHelper.putResponse(context, new JsonObject(failureHandler.getMessage()));
              buildResponse(context);
            });
  }

  private boolean isValidationNotRequired(AuthInfo authInfo) {
    return !isEnableLimit && authInfo.getAccessPolicy().equalsIgnoreCase("OPEN");
  }

  private Future<ValidateDataAccessResult> validateDataAccess(AuthInfo authInfo) {

    Promise<ValidateDataAccessResult> promise = Promise.promise();
    MeteringCountRequest meteringCountRequest = getMeteringCountRequest(authInfo);

    if (isEnableLimit && authInfo.getRole().getRole().equalsIgnoreCase("consumer")) {
      Future<ConsumedDataInfo> consumedDataFuture =
          meteringService.getConsumedData(meteringCountRequest);
      consumedDataFuture
          .onSuccess(
              consumedData -> {
                LOGGER.info(
                    "consumedData: {}",
                    consumedData.getConsumedData() + " , " + consumedData.getApiCount());
                ValidateDataAccessResult validateDataAccessResult = new ValidateDataAccessResult();
                  if (DataAccessLimitValidator.isUsageWithinLimits(authInfo, consumedData)) {
                    LOGGER.info("User access is allowed.");
                    validateDataAccessResult.setConsumedDataInfo(consumedData);
                    validateDataAccessResult.setWithInLimit(true);
                    promise.complete(validateDataAccessResult);
                  } else {
                    LOGGER.error("Limit Exceeded");
                    validateDataAccessResult.setConsumedDataInfo(consumedData);
                    validateDataAccessResult.setWithInLimit(false);
                    promise.complete(validateDataAccessResult);
                  }
              })
          .onFailure(
              failure -> {
                LOGGER.error("failed to get metering response: ", failure);
                Response response = getInternalServerError();
                promise.fail(response.toString());
              });
    }
    return promise.future();
  }

  private MeteringCountRequest getMeteringCountRequest(AuthInfo authInfo) {
    OffsetDateTime startDateTime = OffsetDateTime.now(ZoneId.of("Z", ZoneId.SHORT_IDS));
    OffsetDateTime endDateTime = startDateTime.withHour(00).withMinute(00).withSecond(00);

    MeteringCountRequest meteringCountRequest = new MeteringCountRequest();
    meteringCountRequest.setStartTime(endDateTime.toString());
    meteringCountRequest.setEndTime(startDateTime.toString());
    meteringCountRequest.setUserid(authInfo.getUserid());
    meteringCountRequest.setAccessType(ACCESS_MAP.get(authInfo.getApi()));
    if (authInfo.getEndPoint().equalsIgnoreCase("/ngsi-ld/v1/async/status")) {
      meteringCountRequest.setResourceId(authInfo.getResourceId());
    } else {
      /*meteringCountRequest.put("resourceId", authInfo.getValue(ID));*/
      // TODO: Need to check and verify
    }
    LOGGER.trace("Metering count" + meteringCountRequest);
    return meteringCountRequest;
  }


  public void buildResponse(RoutingContext routingContext) {
    routingContext
        .response()
        .setStatusCode(routingContext.get("statusCode"))
        .end((String) routingContext.get("response"));
  }

  private static Response getInternalServerError() {
    return new Response.Builder()
            .withStatus(500)
            .withUrn(ResponseUrn.DB_ERROR_URN.getUrn())
            .withTitle(INTERNAL_SERVER_ERROR.getDescription())
            .withDetail("Internal server error")
            .build();
  }

  private static Response limitExceed() {
    Response response =
            new Response.Builder()
                    .withUrn(LIMIT_EXCEED_URN.getUrn())
                    .withStatus(429)
                    .withTitle("Too Many Requests")
                    .withDetail(LIMIT_EXCEED_URN.getMessage())
                    .build();
    return response;
  }
}
