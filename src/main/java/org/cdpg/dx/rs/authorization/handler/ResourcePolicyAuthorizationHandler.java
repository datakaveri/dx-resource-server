package org.cdpg.dx.rs.authorization.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auth.authorization.exception.AuthorizationException;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.rs.authorization.service.AuthorizationServiceImpl;
import org.cdpg.dx.util.RoutingContextHelper;

public class ResourcePolicyAuthorizationHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER =
      LogManager.getLogger(ResourcePolicyAuthorizationHandler.class);
  private final CatalogueService catService;
  private final AuthorizationServiceImpl authorizationService;

  public ResourcePolicyAuthorizationHandler(CatalogueService catService) {
    this.catService = catService;
    this.authorizationService = new AuthorizationServiceImpl(catService);
  }

  @Override
  public void handle(RoutingContext context) {

    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(context);
    String resourceId = RoutingContextHelper.getId(context);

    if (resourceId == null || jwtData.isEmpty()) {
      LOGGER.error("Resource ID or token is missing in the request");
      context.fail(new AuthorizationException("Resource ID or token is missing in the request"));
      return;
    }
    authorizationService
        .authorize(jwtData.get(), resourceId)
        .onSuccess(v -> context.next())
        .onFailure(
            error -> {
              LOGGER.error("Authorization failed: {}", error.getMessage());
              context.fail(error);
            });
  }
}
