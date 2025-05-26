package org.cdpg.dx.rs.latest.controller;

import static org.cdpg.dx.util.Constants.*;

import io.vertx.core.json.JsonArray;
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
import org.cdpg.dx.rs.apiserver.ApiController;
import org.cdpg.dx.rs.authorization.handler.ResourcePolicyAuthorizationHandler;
import org.cdpg.dx.rs.latest.model.LatestData;
import org.cdpg.dx.rs.latest.service.LatestService;
import org.cdpg.dx.util.RoutingContextHelper;
import org.cdpg.dx.validations.idhandler.GetIdFromPathHandler;

/** Controller to handle latest entity data retrieval endpoints. */
public class LatestController implements ApiController {
  private static final Logger LOGGER = LogManager.getLogger(LatestController.class);

  private final LatestService latestService;
  private final ClientRevocationValidationHandler clientRevocationValidationHandler;
  private final ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler;
  private final AuditingHandler auditingHandler;
  private final GetIdFromPathHandler getIdFromPathHandler = new GetIdFromPathHandler();

  /** Initializes the latest controller with required services and config. */
  public LatestController(
      LatestService latestService,
      ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler,
      ClientRevocationValidationHandler ClientRevocationValidationHandler,
      AuditingHandler auditingHandler) {
    this.latestService = latestService;
    this.clientRevocationValidationHandler = ClientRevocationValidationHandler;
    this.resourcePolicyAuthorizationHandler = resourcePolicyAuthorizationHandler;
    this.auditingHandler = auditingHandler;
  }

  @Override
  public void register(RouterBuilder builder) {
    builder
        .operation(GET_LATEST_ENTITY_DATA)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromPathHandler)
        .handler(clientRevocationValidationHandler)
        .handler(resourcePolicyAuthorizationHandler)
        .handler(this::roleAccessValidation)
        .handler(this::handleLatestSearchQuery);

    LOGGER.debug("Latest Controller deployed and route registered.");
  }

  private void handleLatestSearchQuery(RoutingContext ctx) {
    LOGGER.debug("Handling latest data query");
    String id = ctx.pathParam(ID);

    latestService
        .getLatestData(id)
        .onSuccess(result -> sendResponse(ctx, result))
        .onFailure(
            err -> {
              LOGGER.error("Error processing latest data request for ID: {}", id, err);
              ctx.fail(err);
            });
  }

  private void sendResponse(RoutingContext ctx, LatestData latestData) {
    if (latestData.getLatestData().isEmpty()) {
      ResponseBuilder.sendNoContent(ctx);
    } else {
      new AuditLogConstructor(ctx);
      ResponseBuilder.sendSuccess(ctx, latestData.getLatestData());
    }
  }

  public void roleAccessValidation(RoutingContext routingContext) {
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    if (!"consumer".equalsIgnoreCase(jwtData.get().role())) {
      routingContext.next();
    }
    boolean hasSubAccess =
        jwtData.get().cons().getJsonArray("access", new JsonArray()).contains("api");
    if (!hasSubAccess) {
      LOGGER.error("Role validation failed");
      routingContext.fail(new DxAuthException("Role validation failed"));
    }
    routingContext.next();
  }
}
