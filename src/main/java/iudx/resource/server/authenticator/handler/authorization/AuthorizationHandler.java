package iudx.resource.server.authenticator.handler.authorization;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.authenticator.model.DxRole;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.RoutingContextHelper;
import java.util.Arrays;

public class AuthorizationHandler implements Handler<RoutingContext> {
  //    private static final Logger LOGGER = LogManager.getLogger(AuthorizationHandler.class);

  /**
   * @param routingContext
   */
  @Override
  public void handle(RoutingContext routingContext) {
    routingContext.next();
  }

  public Handler<RoutingContext> setUserRolesForEndpoint(DxRole... roleForApi) {
    return context -> handleWithRoles(context, roleForApi);
  }

  private void handleWithRoles(RoutingContext event, DxRole[] roleForApi) {
    JwtData jwtData = RoutingContextHelper.getJwtData(event);
    DxRole userRole = DxRole.fromRole(jwtData);
    boolean isUserAllowedToAccessApi = Arrays.asList(roleForApi).contains(userRole);
    if (!isUserAllowedToAccessApi) {
      event.fail(
          new DxRuntimeException(
              HttpStatusCode.UNAUTHORIZED.getValue(), ResponseUrn.INVALID_TOKEN_URN));
    }
    event.next();
  }
}
