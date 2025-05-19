package org.cdpg.dx.rs.ingestion.controller;

import static org.cdpg.dx.util.Constants.*;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auditing.handler.AuditingHandler;
import org.cdpg.dx.auditing.helper.AuditLogConstructor;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.common.response.ResponseBuilder;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.revoked.service.RevokedService;
import org.cdpg.dx.rs.apiserver.ApiController;
import org.cdpg.dx.rs.authorization.handler.ResourcePolicyAuthorizationHandler;
import org.cdpg.dx.rs.ingestion.dao.IngestionDAO;
import org.cdpg.dx.rs.ingestion.service.IngestionService;
import org.cdpg.dx.rs.ingestion.service.IngestionServiceImpl;
import org.cdpg.dx.util.RoutingContextHelper;
import org.cdpg.dx.validations.idhandler.GetIdForIngestionEntityHandler;
import org.cdpg.dx.validations.idhandler.GetIdFromBodyHandler;
import org.cdpg.dx.validations.idhandler.GetIdFromPathHandler;
import org.cdpg.dx.validations.provider.ProviderValidationHandler;

public class IngestionAdaptorController implements ApiController {
  private static final Logger LOGGER = LogManager.getLogger(IngestionAdaptorController.class);
  private final ClientRevocationValidationHandler clientRevocationValidationHandler;
  private final ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler;
  private final IngestionService ingestionService;
  private final GetIdFromPathHandler getIdFromPathHandler;
  private final GetIdFromBodyHandler getIdFromBodyHandler;
  private final ProviderValidationHandler providerValidationHandler;
  private final AuditingHandler auditingHandler;
  private final GetIdForIngestionEntityHandler getIdForIngestionEntityHandler;

  public IngestionAdaptorController(
      Vertx vertx,
      DataBrokerService dataBrokerService,
      RevokedService revokedService,
      CatalogueService catalogueService,
      IngestionDAO ingestionDAO) {
    this.clientRevocationValidationHandler = new ClientRevocationValidationHandler(revokedService);
    this.resourcePolicyAuthorizationHandler =
        new ResourcePolicyAuthorizationHandler(catalogueService);
    this.ingestionService =
        new IngestionServiceImpl(catalogueService, dataBrokerService, ingestionDAO);
    this.getIdFromPathHandler = new GetIdFromPathHandler();
    this.getIdFromBodyHandler = new GetIdFromBodyHandler();
    this.providerValidationHandler = new ProviderValidationHandler(catalogueService);
    this.auditingHandler = new AuditingHandler(vertx);
    this.getIdForIngestionEntityHandler = new GetIdForIngestionEntityHandler();
  }

  @Override
  public void register(RouterBuilder builder) {
    builder
        .operation(GET_ADAPTER_LIST)
        .handler(clientRevocationValidationHandler)
        .handler(this::handleGetAllAdapterDetails)
        .failureHandler(this::handleFailure);

    builder
        .operation(POST_ADAPTER)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromBodyHandler)
        .handler(clientRevocationValidationHandler)
        .handler(providerValidationHandler)
        .handler(this::handleRegisterAdapter)
        .failureHandler(this::handleFailure);

    builder
        .operation(GET_ADAPTOR_BY_ID)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromPathHandler)
        .handler(clientRevocationValidationHandler)
        .handler(providerValidationHandler)
        .handler(this::handleGetAdapterDetailsById)
        .failureHandler(this::handleFailure);

    builder
        .operation(DELETE_ADAPTER_BY_ID)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromPathHandler)
        .handler(clientRevocationValidationHandler)
        .handler(providerValidationHandler)
        .handler(this::handleDeleteAdapter)
        .failureHandler(this::handleFailure);
    builder
        .operation(POST_INGESTION_ADAPTER_ENTITIES)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdForIngestionEntityHandler)
        .handler(clientRevocationValidationHandler)
        .handler(providerValidationHandler)
        .handler(this::handlePublishDataFromAdapter)
        .failureHandler(this::handleFailure);
  }

  private void handleGetAllAdapterDetails(RoutingContext routingContext) {
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    ingestionService
        .getAllAdapterDetailsForUser(jwtData.get().iid().split(":")[1])
        .onSuccess(
            result -> {
              ResponseBuilder.sendSuccess(routingContext, result);
            })
        .onFailure(
            err -> {
              // handle failureHandler

            });
  }

  private void handleGetAdapterDetailsById(RoutingContext routingContext) {
    String exchangeName = RoutingContextHelper.getId(routingContext);

    ingestionService
        .getAdapterDetails(exchangeName)
        .onSuccess(
            result -> {
              ResponseBuilder.sendSuccess(routingContext, result.toJson());
              RoutingContextHelper.setResponseSize(routingContext, 0);
              new AuditLogConstructor(routingContext);
            })
        .onFailure(
            err -> {
              // handle failureHandler

            });
  }

  private void handlePublishDataFromAdapter(RoutingContext routingContext) {
    JsonArray requestJson = routingContext.body().asJsonArray();
    ingestionService
        .publishDataFromAdapter(requestJson)
        .onSuccess(
            result -> {
              RoutingContextHelper.setResponseSize(routingContext, 0);
              new AuditLogConstructor(routingContext);
              ResponseBuilder.sendSuccess(routingContext, "Item Published");
            })
        .onFailure(
            err -> {
              // handle failureHandler

            });
  }

  private void handleDeleteAdapter(RoutingContext routingContext) {
    String exchangeName = routingContext.pathParam(ID);
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    String userId = jwtData.get().sub();

    ingestionService
        .deleteAdapter(exchangeName, userId)
        .onSuccess(
            result -> {
              RoutingContextHelper.setResponseSize(routingContext, 0);
              new AuditLogConstructor(routingContext);
              ResponseBuilder.sendSuccess(routingContext, "Adapter deleted");
            })
        .onFailure(
            err -> {
              // handle failureHandler

            });
  }

  private void handleRegisterAdapter(RoutingContext routingContext) {
    JsonObject requestBody = routingContext.body().asJsonObject();
    String entitiesId = requestBody.getJsonArray("entities").getString(0);
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    String userId = jwtData.get().sub();
    ingestionService
        .registerAdapter(entitiesId, userId)
        .onSuccess(
            result -> {
              RoutingContextHelper.setResponseSize(routingContext, 0);
              new AuditLogConstructor(routingContext);
              ResponseBuilder.sendSuccess(routingContext, result.toJson());
            })
        .onFailure(
            err -> {
              // handle failureHandler

            });
  }

  private void handleFailure(RoutingContext ctx) {
    Throwable failure = ctx.failure();
    int statusCode = ctx.statusCode();

    if (statusCode < 400) {
      // Default to 500 if not set
      statusCode = 500;
    }

    String message = failure != null ? failure.getMessage() : "Unknown error occurred";

    ctx.response()
        .putHeader("Content-Type", "application/json")
        .setStatusCode(statusCode)
        .end(new JsonObject().put("error", message).put("status", statusCode).encode());
  }
}
