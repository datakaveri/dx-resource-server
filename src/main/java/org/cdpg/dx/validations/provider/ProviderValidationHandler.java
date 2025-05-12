package org.cdpg.dx.validations.provider;

import static org.cdpg.dx.common.ErrorCode.ERROR_NOT_FOUND;
import static org.cdpg.dx.common.ErrorMessage.BAD_REQUEST_ERROR;
import static org.cdpg.dx.util.ResponseUrn.UNAUTHORIZED_RESOURCE_URN;
import static org.cdpg.dx.validations.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.util.HttpStatusCode;
import org.cdpg.dx.util.ResponseUrn;
import org.cdpg.dx.util.RoutingContextHelper;

public class ProviderValidationHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(ProviderValidationHandler.class);
  private final CatalogueService catalogueService;

  public ProviderValidationHandler(CatalogueService catalogueService) {
    this.catalogueService = catalogueService;
  }

  @Override
  public void handle(RoutingContext routingContext) {
    String id = RoutingContextHelper.getId(routingContext);
    LOGGER.debug("id {} to verify provider check", id);

    catalogueService
        .getProviderOwnerId(id)
        .compose(
            providerIdHandler -> {
              LOGGER.trace("providerOwnerId {}", providerIdHandler);
              return validateProviderUser(
                  providerIdHandler, RoutingContextHelper.getJwtData(routingContext).get());
            })
        .onSuccess(
            validateProviderUserHandler -> {
              if (validateProviderUserHandler) {
                routingContext.next();
              } else {
                LOGGER.error("Permission not allowed");
                HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
                routingContext
                    .response()
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(statusCode.getValue())
                    .end(generateResponse(UNAUTHORIZED_RESOURCE_URN, statusCode).toString());
              }
            })
        .onFailure(
            err -> {
              routingContext.fail(404, new ServiceException(ERROR_NOT_FOUND, BAD_REQUEST_ERROR));
            });
  }

  Future<Boolean> validateProviderUser(String providerUserId, JwtData jwtData) {
    LOGGER.trace("validateProviderUser() started");
    Promise<Boolean> promise = Promise.promise();
    try {
      if (jwtData.role().equalsIgnoreCase("delegate")) {
        if (jwtData.did().equalsIgnoreCase(providerUserId)) {
          LOGGER.info("success");
          promise.complete(true);
        } else {
          LOGGER.error("incorrect providerUserId");
          promise.fail(new ServiceException(ERROR_NOT_FOUND, BAD_REQUEST_ERROR));
        }
      } else if (jwtData.role().equalsIgnoreCase("provider")) {
        if (jwtData.sub().equalsIgnoreCase(providerUserId)) {
          LOGGER.info("success");
          promise.complete(true);
        } else {
          LOGGER.error("incorrect providerUserId");
          promise.fail(new ServiceException(ERROR_NOT_FOUND, BAD_REQUEST_ERROR));
        }
      } else {
        LOGGER.error("invalid role");
        promise.fail(new ServiceException(ERROR_NOT_FOUND, BAD_REQUEST_ERROR));
      }
    } catch (Exception e) {
      LOGGER.error("exception occurred while validating provider user : {}", e.getMessage());
      promise.fail(new ServiceException(ERROR_NOT_FOUND, BAD_REQUEST_ERROR));
    }
    return promise.future();
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }
}
