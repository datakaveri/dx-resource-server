package org.cdpg.dx.rs.admin.controller;

import static org.cdpg.dx.util.Constants.*;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auditing.handler.AuditingHandler;
import org.cdpg.dx.auditing.helper.AuditLogConstructor;
import org.cdpg.dx.auth.authorization.exception.AuthorizationException;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.common.exception.DxAuthException;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.common.response.ResponseBuilder;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.revoked.service.RevokedService;
import org.cdpg.dx.rs.admin.dao.RevokedTokenServiceDAO;
import org.cdpg.dx.rs.admin.dao.UniqueAttributeServiceDAO;
import org.cdpg.dx.rs.admin.service.AdminService;
import org.cdpg.dx.rs.admin.service.AdminServiceImpl;
import org.cdpg.dx.rs.apiserver.ApiController;
import org.cdpg.dx.util.RoutingContextHelper;
import org.cdpg.dx.validations.idhandler.GetIdFromBodyHandler;
import org.cdpg.dx.validations.idhandler.GetIdFromParams;

public class AdminController implements ApiController {
  private static final Logger LOGGER = LogManager.getLogger(AdminController.class);
  private final ClientRevocationValidationHandler clientRevocationValidationHandler;
  public AdminService adminService;
  public AuditingHandler auditingHandler;

  public AdminController(
      Vertx vertx,
      RevokedTokenServiceDAO revokedTokenServiceDAO,
      UniqueAttributeServiceDAO uniqueAttributeServiceDAO,
      DataBrokerService dataBrokerService,
      RevokedService revokedService) {
    this.adminService =
        new AdminServiceImpl(revokedTokenServiceDAO, uniqueAttributeServiceDAO, dataBrokerService);
    this.clientRevocationValidationHandler = new ClientRevocationValidationHandler(revokedService);
    this.auditingHandler = new AuditingHandler(vertx);
  }

  @Override
  public void register(RouterBuilder builder) {
    GetIdFromBodyHandler getIdFromBodyHandler = new GetIdFromBodyHandler();
    GetIdFromParams getIdFromParams = new GetIdFromParams();
    builder
        .operation(REVOKE_TOKEN)
        .handler(auditingHandler::handleApiAudit)
        .handler(clientRevocationValidationHandler)
        .handler(this::roleAccessValidation)
        .handler(this::handlerRevokedTokenRequest);
    builder
        .operation(POST_RESOURCE_ATTRIBUTE)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromBodyHandler)
        .handler(clientRevocationValidationHandler)
        .handler(this::roleAccessValidation)
        .handler(this::handleCreateUniqueAttribute);
    builder
        .operation(DELETE_RESOURCE_ATTRIBUTE)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromParams)
        .handler(clientRevocationValidationHandler)
        .handler(this::roleAccessValidation)
        .handler(this::handleDeleteUniqueAttribute);
    builder
        .operation(UPDATE_RESOURCE_ATTRIBUTE)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromBodyHandler)
        .handler(clientRevocationValidationHandler)
        .handler(this::roleAccessValidation)
        .handler(this::handleUpdateUniqueAttribute);
  }

  private void handlerRevokedTokenRequest(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    String userid = requestBody.getString("sub");

    adminService
        .revokedTokenRequest(userid)
        .onSuccess(
            successHandler -> {
              RoutingContextHelper.setResponseSize(routingContext, 0);
              new AuditLogConstructor(routingContext);
              ResponseBuilder.sendSuccess(routingContext, "Success");
            })
        .onFailure(routingContext::fail);
  }

  private void handleCreateUniqueAttribute(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    String resourceId = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");

    adminService
        .createUniqueAttribute(resourceId, attribute)
        .onSuccess(
            successHandler -> {
              RoutingContextHelper.setResponseSize(routingContext, 0);
              new AuditLogConstructor(routingContext);
              ResponseBuilder.sendSuccess(routingContext, "Success");
            })
        .onFailure(
            failureHandler -> {
              routingContext.fail(failureHandler);
            });
  }

  private void handleUpdateUniqueAttribute(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    String id = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");

    adminService
        .updateUniqueAttribute(id, attribute)
        .onSuccess(
            successHandler -> {
              RoutingContextHelper.setResponseSize(routingContext, 0);
              new AuditLogConstructor(routingContext);
              ResponseBuilder.sendSuccess(routingContext, "Success");
            })
        .onFailure(routingContext::fail);
  }

  private void handleDeleteUniqueAttribute(RoutingContext routingContext) {
    String id = routingContext.request().getParam("id");

    adminService
        .deleteUniqueAttribute(id)
        .onSuccess(
            successHandler -> {
              RoutingContextHelper.setResponseSize(routingContext, 0);
              new AuditLogConstructor(routingContext);
              ResponseBuilder.sendSuccess(routingContext, "Success");
            })
        .onFailure(routingContext::fail);
  }

  public void roleAccessValidation(RoutingContext routingContext) {
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    if ("admin".equalsIgnoreCase(jwtData.get().role())) {
      routingContext.next();
    } else {
      routingContext.fail(new DxAuthException("Role validation failed"));
    }
  }
}
