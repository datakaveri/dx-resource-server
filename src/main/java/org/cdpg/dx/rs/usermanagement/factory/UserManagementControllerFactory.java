package org.cdpg.dx.rs.usermanagement.factory;

import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.rs.usermanagement.controller.UserManagementController;
import org.cdpg.dx.rs.usermanagement.service.UserManagementService;
import org.cdpg.dx.rs.usermanagement.service.UserManagementServiceImpl;

public class UserManagementControllerFactory {

  public static UserManagementController create(
      DataBrokerService dataBroker,
      ClientRevocationValidationHandler clientRevocationValidationHandler) {

    UserManagementService UserManagementService = new UserManagementServiceImpl(dataBroker);

    return new UserManagementController(UserManagementService, clientRevocationValidationHandler);
  }
}
