package org.cdpg.dx.rs.search.controller;

import static org.cdpg.dx.util.Constants.GET_SPATIAL_DATA;
import static org.cdpg.dx.util.Constants.JSON_COUNT;
import static org.cdpg.dx.util.Constants.POST_SPATIAL_SEARCH;
import static org.cdpg.dx.util.Constants.POST_TEMPORAL_SEARCH;
import static org.cdpg.dx.util.Constants.TEMPORAL_SEARCH;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
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
import org.cdpg.dx.common.ResponseUrn;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.rs.apiserver.ApiController;
import org.cdpg.dx.rs.authorization.handler.ResourcePolicyAuthorizationHandler;
import org.cdpg.dx.rs.search.model.RequestDTO;
import org.cdpg.dx.rs.search.service.SearchApiService;
import org.cdpg.dx.rs.search.util.RequestType;
import org.cdpg.dx.rs.search.util.ResponseModel;
import org.cdpg.dx.util.RoutingContextHelper;
import org.cdpg.dx.validations.idhandler.GetIdFromBodyHandler;
import org.cdpg.dx.validations.idhandler.GetIdFromParams;

/**
 * Controller to handle spatial and temporal search endpoints. Uses the shared SearchRequestDto
 * model from util package.
 */
public class SearchController implements ApiController {
  private static final Logger LOGGER = LogManager.getLogger(SearchController.class);
  private final SearchApiService searchService;
  private final ClientRevocationValidationHandler clientRevocationValidationHandler;
  private final ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler;
  private final AuditingHandler auditingHandler;

  GetIdFromBodyHandler getIdFromBodyHandler = new GetIdFromBodyHandler();
  GetIdFromParams getIdFromParams = new GetIdFromParams();

  /** Initializes the search controller with required services and config. */
  public SearchController(
      SearchApiService searchService,
      ClientRevocationValidationHandler clientRevocationValidationHandler,
      ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler,
      AuditingHandler auditingHandler) {
    this.searchService = searchService;
    this.clientRevocationValidationHandler = clientRevocationValidationHandler;
    this.resourcePolicyAuthorizationHandler = resourcePolicyAuthorizationHandler;
    this.auditingHandler = auditingHandler;
  }

  @Override
  public void register(RouterBuilder builder) {
    builder
        .operation(GET_SPATIAL_DATA)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromParams)
        .handler(clientRevocationValidationHandler)
        .handler(resourcePolicyAuthorizationHandler)
        .handler(ctx -> handleGetQuery(ctx, RequestType.ENTITY));

    builder
        .operation(TEMPORAL_SEARCH)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromParams)
        .handler(clientRevocationValidationHandler)
        .handler(resourcePolicyAuthorizationHandler)
        .handler(ctx -> handleGetQuery(ctx, RequestType.TEMPORAL));

    builder
        .operation(POST_SPATIAL_SEARCH)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromBodyHandler)
        .handler(clientRevocationValidationHandler)
        .handler(resourcePolicyAuthorizationHandler)
        .handler(ctx -> handlePostQuery(ctx, RequestType.POST_ENTITIES));

    builder
        .operation(POST_TEMPORAL_SEARCH)
        .handler(auditingHandler::handleApiAudit)
        .handler(getIdFromBodyHandler)
        .handler(clientRevocationValidationHandler)
        .handler(resourcePolicyAuthorizationHandler)
        .handler(ctx -> handlePostQuery(ctx, RequestType.POST_TEMPORAL));

    LOGGER.info("SearchController deployed and routes registered.");
  }

  private void handleGetQuery(RoutingContext ctx, RequestType type) {
    LOGGER.debug("Handling GET {} query", type);
    MultiMap params = ctx.request().params(true);
    params.add("requestType", type.name());
    boolean countOnly = JSON_COUNT.equalsIgnoreCase(params.get("options"));

    Future<RequestDTO> dtoFuture = searchService.createRequestDto(params);
    processQuery(dtoFuture, type, countOnly, ctx);
  }

  private void handlePostQuery(RoutingContext ctx, RequestType type) {
    LOGGER.debug("Handling POST {} query", type);
    JsonObject body = ctx.getBodyAsJson();
    body.put("requestType", type.name());
    boolean countOnly = JSON_COUNT.equalsIgnoreCase(body.getString("options"));

    Future<RequestDTO> dtoFuture = searchService.createRequestDto(body);
    processQuery(dtoFuture, type, countOnly, ctx);
  }

  /** Centralizes execution of search requests. */
  private void processQuery(
      Future<RequestDTO> dtoFuture, RequestType type, boolean countOnly, RoutingContext ctx) {
    dtoFuture
        .compose(dto -> dispatchQuery(dto, type))
        .onSuccess(result -> sendResponse(ctx, result, countOnly))
        .onFailure(
            err -> {
              LOGGER.error("Error processing {} request", type, err);
              ctx.fail(err);
            });
  }

  private Future<ResponseModel> dispatchQuery(RequestDTO dto, RequestType type) {
    return switch (type) {
      case ENTITY -> searchService.handleEntitiesQuery(dto);
      case TEMPORAL -> searchService.handleTemporalQuery(dto);
      case POST_ENTITIES, POST_TEMPORAL -> searchService.handlePostEntitiesQuery(dto);
      default -> Future.failedFuture("Unsupported request type: " + type);
    };
  }

  private void sendResponse(RoutingContext ctx, ResponseModel responseModel, boolean countOnly) {
    JsonObject raw = responseModel.getResponse();
    JsonObject response =
        new JsonObject()
            .put("type", ResponseUrn.SUCCESS_URN.getUrn())
            .put("title", ResponseUrn.SUCCESS_URN.getMessage());

    if (countOnly) {
      response.put(
          "results",
          new JsonArray().add(new JsonObject().put("totalHits", raw.getInteger("totalHits"))));
    } else {
      response
          .put("results", raw.getJsonArray("results"))
          .put("limit", raw.getInteger("limit"))
          .put("offset", raw.getInteger("offset"))
          .put("totalHits", raw.getInteger("totalHits"));
    }
    new AuditLogConstructor(ctx);
    ctx.response()
        .putHeader("Content-Type", "application/json")
        .setStatusCode(200)
        .end(response.encode());
  }

  public void roleAccessValidation(RoutingContext routingContext) {
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    if (!"consumer".equalsIgnoreCase(jwtData.get().role())) {
      routingContext.next();
    }
    boolean hasApiAccess =
        jwtData.get().cons().getJsonArray("access", new JsonArray()).contains("api");
    if (!hasApiAccess) {
      LOGGER.error("Role validation failed");
      routingContext.fail(new AuthorizationException("Role validation failed"));
    }
    routingContext.next();
  }
}
