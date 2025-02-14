package iudx.resource.server.apiserver.usermanagement.controller;

import static iudx.resource.server.apiserver.util.Constants.RESET_PWD;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserManagementController {
  private static final Logger LOGGER = LogManager.getLogger(UserManagementController.class);

  private final Router router;
  private String dxApiBasePath;

  public UserManagementController(Router router, String dxApiBasePath) {
    this.dxApiBasePath = dxApiBasePath;
    this.router = router;
  }

  public void init() {
    router.post(dxApiBasePath + RESET_PWD).handler(this::resetPassword);
  }

  private void resetPassword(RoutingContext routingContext) {}
}
