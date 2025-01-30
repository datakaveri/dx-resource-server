package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.response.ResponseUtil.generateResponse;
import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.resource.server.apiserver.util.Constants.USER_ID;
import static iudx.resource.server.common.HttpStatusCode.UNAUTHORIZED;
import static iudx.resource.server.common.ResponseUrn.INVALID_TOKEN_URN;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.RoutingContextHelper;
import iudx.resource.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ManagementRestApi {

  private static final Logger LOGGER = LogManager.getLogger(ManagementRestApi.class);

  private final DataBrokerService rmqBrokerService;

  ManagementRestApi(DataBrokerService brokerService) {
    this.rmqBrokerService = brokerService;
  }

  public void resetPassword(RoutingContext routingContext) {
    LOGGER.trace("Info: resetPassword method started");

    HttpServerResponse response = routingContext.response();
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    String userId = jwtData.getSub();
    JsonObject request = new JsonObject();
    request.put(USER_ID, userId);

    rmqBrokerService.resetPassword(
        request,
        handler -> {
          if (handler.succeeded()) {
            handleSuccessResponse(response, ResponseType.Ok.getCode(), handler.result().toString());
          } else {
            handleResponse(response, UNAUTHORIZED, INVALID_TOKEN_URN);
          }
        });
  }

  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode code, ResponseUrn urn) {
    handleResponse(response, code, urn, code.getDescription());
  }

  private void handleResponse(
      HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn, String message) {

    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, message).toString());
  }
}
