package org.cdpg.dx.rs.usermanagement.controller;

import static org.cdpg.dx.util.Constants.*;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.common.response.ResponseBuilder;
import org.cdpg.dx.rs.apiserver.ApiController;
import org.cdpg.dx.rs.usermanagement.service.UserManagementService;
import org.cdpg.dx.util.RoutingContextHelper;

public class UserManagementController implements ApiController {
  private static final Logger LOGGER = LogManager.getLogger(UserManagementController.class);
  private final UserManagementService userManagementService;
  private final ClientRevocationValidationHandler clientRevocationValidationHandler;

  public UserManagementController(
      UserManagementService userManagementService,
      ClientRevocationValidationHandler clientRevocationValidationHandler) {
    this.userManagementService = userManagementService;
    this.clientRevocationValidationHandler = clientRevocationValidationHandler;
  }

  @Override
  public void register(RouterBuilder builder) {
    builder
        .operation(RESET_PASSWORD)
        .handler(clientRevocationValidationHandler)
        .handler(this::handleResetPassword);
  }

  private void handleResetPassword(RoutingContext routingContext) {
    LOGGER.debug("Trying to reset password.");
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    String userid = jwtData.get().sub();

    userManagementService
        .resetPassword(userid)
        .onSuccess(
            successHandler -> {
              ResponseBuilder.sendSuccess(routingContext, successHandler.toJson());
            })
        .onFailure(
              routingContext::fail);
  }
}
