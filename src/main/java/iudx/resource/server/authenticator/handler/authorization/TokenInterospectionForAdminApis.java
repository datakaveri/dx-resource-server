package iudx.resource.server.authenticator.handler.authorization;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.RoutingContextHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TokenInterospectionForAdminApis implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(TokenInterospectionForAdminApis.class);

  /**
   * @param context
   */
  /* TODO: check if the issuer can be fetched from config*/
  @Override
  public void handle(RoutingContext context) {
    JwtData jwtData = RoutingContextHelper.getJwtData(context);
    if (jwtData.getSub() == null) {
      LOGGER.error("No sub value in JWT");
      context.fail(
          new DxRuntimeException(
              HttpStatusCode.UNAUTHORIZED.getValue(), ResponseUrn.UNAUTHORIZED_ENDPOINT_URN));
    } else if (jwtData.getAud().isEmpty()) {
      LOGGER.error("No audience value in JWT");
      context.fail(
          new DxRuntimeException(
              HttpStatusCode.UNAUTHORIZED.getValue(), ResponseUrn.UNAUTHORIZED_ENDPOINT_URN));
    } else if (!jwtData.getSub().equalsIgnoreCase(jwtData.getIss())) {
      LOGGER.error("Incorrect subject value in JWT");
      context.fail(
          new DxRuntimeException(
              HttpStatusCode.UNAUTHORIZED.getValue(), ResponseUrn.UNAUTHORIZED_ENDPOINT_URN));
    } else {
      LOGGER.info("Auth token verified.");
      context.next();
    }
    context.next();
  }
}
