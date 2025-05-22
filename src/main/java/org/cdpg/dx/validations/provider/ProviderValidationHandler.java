package org.cdpg.dx.validations.provider;

import static org.cdpg.dx.common.ErrorMessage.BAD_REQUEST_ERROR;
import static org.cdpg.dx.common.ResponseUrn.UNAUTHORIZED_RESOURCE_URN;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.common.exception.DxAuthException;
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.cdpg.dx.common.models.JwtData;
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
        Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
        roleAccessValidation(jwtData.get())
                .compose(
                        roleAccessValidationHandler -> catalogueService.getProviderOwnerId(id))
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
                                routingContext.fail(new DxAuthException(UNAUTHORIZED_RESOURCE_URN.getMessage()));
                            }
                        })
                .onFailure(
                        routingContext::fail);
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
                    promise.fail(new DxBadRequestException(BAD_REQUEST_ERROR));
                }
            } else if (jwtData.role().equalsIgnoreCase("provider")) {
                if (jwtData.sub().equalsIgnoreCase(providerUserId)) {
                    LOGGER.info("success");
                    promise.complete(true);
                } else {
                    LOGGER.error("incorrect providerUserId");
                    promise.fail(new DxBadRequestException(BAD_REQUEST_ERROR));
                }
            } else {
                LOGGER.error("invalid role");
                promise.fail(new DxBadRequestException(BAD_REQUEST_ERROR));
            }
        } catch (Exception e) {
            LOGGER.error("exception occurred while validating provider user : {}", e.getMessage());
            promise.fail(new DxBadRequestException(BAD_REQUEST_ERROR));
        }
        return promise.future();
    }

    public Future<Boolean> roleAccessValidation(JwtData jwtData) {

        if ("provider".equalsIgnoreCase(jwtData.role())) {
            return Future.succeededFuture(true);
        } else if ("delegate".equalsIgnoreCase(jwtData.role())
                && "provider".equalsIgnoreCase(jwtData.drl())) {
            return Future.succeededFuture(true);
        } else {
            LOGGER.error("Client doesn't have access of this api");
            return Future.failedFuture(new DxAuthException("Client doesn't have access of this api"));
        }
    }
}
