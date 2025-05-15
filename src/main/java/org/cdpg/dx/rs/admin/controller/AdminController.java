package org.cdpg.dx.rs.admin.controller;

import static org.cdpg.dx.util.Constants.*;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.revoked.service.RevokedService;
import org.cdpg.dx.rs.admin.dao.RevokedTokenServiceDAO;
import org.cdpg.dx.rs.admin.dao.UniqueAttributeServiceDAO;
import org.cdpg.dx.rs.admin.service.AdminService;
import org.cdpg.dx.rs.admin.service.AdminServiceImpl;
import org.cdpg.dx.rs.apiserver.ApiController;
import org.cdpg.dx.util.RoutingContextHelper;

public class AdminController implements ApiController {
  private static final Logger LOGGER = LogManager.getLogger(AdminController.class);
  private final ClientRevocationValidationHandler clientRevocationValidationHandler;
  public AdminService adminService;

  public AdminController(
      RevokedTokenServiceDAO revokedTokenServiceDAO,
      UniqueAttributeServiceDAO uniqueAttributeServiceDAO,
      DataBrokerService dataBrokerService,
      RevokedService revokedService) {
    this.adminService =
        new AdminServiceImpl(revokedTokenServiceDAO, uniqueAttributeServiceDAO, dataBrokerService);
    this.clientRevocationValidationHandler = new ClientRevocationValidationHandler(revokedService);
  }

  @Override
  public void register(RouterBuilder builder) {
    builder
        .operation(REVOKE_TOKEN)
        .handler(clientRevocationValidationHandler)
        .handler(this::handlerRevokedTokenRequest)
        .failureHandler(this::handleFailure);
    builder
        .operation(POST_RESOURCE_ATTRIBUTE)
        .handler(clientRevocationValidationHandler)
        .handler(this::handleCreateUniqueAttribute)
        .failureHandler(this::handleFailure);
    builder
        .operation(DELETE_RESOURCE_ATTRIBUTE)
        .handler(clientRevocationValidationHandler)
        .handler(this::handleDeleteUniqueAttribute)
        .failureHandler(this::handleFailure);
    builder
        .operation(UPDATE_RESOURCE_ATTRIBUTE)
        .handler(clientRevocationValidationHandler)
        .handler(this::handleUpdateUniqueAttribute)
        .failureHandler(this::handleFailure);
  }

  private void handlerRevokedTokenRequest(RoutingContext routingContext) {
    LOGGER.debug("Trying to revoke token.");
    JsonObject requestBody = routingContext.getBodyAsJson();
    String userid = requestBody.getString("sub");
    LOGGER.debug("Trying to revoke token. " + userid);

    adminService
        .revokedTokenRequest(userid)
        .onSuccess(
            successHandler -> {
              // handle success handler

            })
        .onFailure(
            failureHandler -> {
              // handle failure handler
            });
  }

  private void handleCreateUniqueAttribute(RoutingContext routingContext) {
    LOGGER.debug("Trying to create unique attribute");

    JsonObject requestBody = routingContext.getBodyAsJson();
    String userid = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");
    LOGGER.debug("Trying to revoke token. {},{}", userid, attribute);

    adminService
        .createUniqueAttribute(userid, attribute)
        .onSuccess(
            successHandler -> {
              // handle success handler

            })
        .onFailure(
            failureHandler -> {
              // handle failure handler
            });
  }

  private void handleUpdateUniqueAttribute(RoutingContext routingContext) {
    LOGGER.debug("Trying to update unique attribute");

    JsonObject requestBody = routingContext.getBodyAsJson();

    String userid = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");
    LOGGER.debug("Trying to revoke token. {},{}", userid, attribute);

    adminService
        .updateUniqueAttribute(userid, attribute)
        .onSuccess(
            successHandler -> {
              // handle success handler

            })
        .onFailure(
            failureHandler -> {
              // handle failure handler
            });
  }

  private void handleDeleteUniqueAttribute(RoutingContext routingContext) {
    LOGGER.debug("Trying to delete unique attribute");
    String userId = RoutingContextHelper.getId(routingContext);
    LOGGER.debug("Trying to revoke token. {}", userId);

    adminService
        .deleteUniqueAttribute(userId)
        .onSuccess(
            successHandler -> {
              // handle success handler

            })
        .onFailure(
            failureHandler -> {
              // handle failure handler
            });
  }

  private void handleFailure(RoutingContext ctx) {
    int status = ctx.statusCode() >= 400 ? ctx.statusCode() : 500;
    String message = ctx.failure() != null ? ctx.failure().getMessage() : "Unknown error occurred";

    ctx.response()
        .putHeader("Content-Type", "application/json")
        .setStatusCode(status)
        .end(new JsonObject().put("error", message).put("status", status).encode());
  }
}
