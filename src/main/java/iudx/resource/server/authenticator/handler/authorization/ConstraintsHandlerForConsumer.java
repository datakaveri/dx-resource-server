package iudx.resource.server.authenticator.handler.authorization;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.authenticator.model.DxAccess;
import iudx.resource.server.authenticator.model.DxRole;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.RoutingContextHelper;
import java.util.HashSet;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConstraintsHandlerForConsumer implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(ConstraintsHandlerForConsumer.class);

  /**
   * @param routingContext Checks if the required constraints matches the constraints given in the
   *     consumer's token to validate consumer's access to specific apis
   */
  @Override
  public void handle(RoutingContext routingContext) {
    routingContext.next();
  }

  public Handler<RoutingContext> consumerConstraintsForEndpoint(DxAccess... constraint) {
    return context -> handleWithConstraints(context, constraint);
  }

  private void handleWithConstraints(RoutingContext event, DxAccess[] requiredConstraints) {
    LOGGER.info("handle with constraints started");
    HashSet<DxAccess> setOfRequiredConstraints = new HashSet<>(List.of(requiredConstraints));
    if (DxRole.fromRole(RoutingContextHelper.getJwtData(event)).equals(DxRole.CONSUMER)) {
      JwtData jwtData = RoutingContextHelper.getJwtData(event);
      List<String> constraintsFromToken = jwtData.getCons().getJsonArray("access").getList();
      for (String value : constraintsFromToken) {
        DxAccess tokenConstraint = DxAccess.fromAccess(value);
        setOfRequiredConstraints.remove(tokenConstraint);
      }

      LOGGER.info("Required constraints for the given api : {}", (Object) requiredConstraints);
      LOGGER.info("Constraints from the token : {}", constraintsFromToken);
      if (!setOfRequiredConstraints.isEmpty()) {
        event.fail(
            new DxRuntimeException(
                HttpStatusCode.UNAUTHORIZED.getValue(),
                ResponseUrn.INVALID_TOKEN_URN,
                "No access provided to endpoint"));
      }
    }
    event.next();
  }
}
