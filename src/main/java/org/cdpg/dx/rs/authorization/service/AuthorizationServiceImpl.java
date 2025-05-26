package org.cdpg.dx.rs.authorization.service;

import io.vertx.core.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.common.exception.DxAuthException;
import org.cdpg.dx.common.models.JwtData;

public class AuthorizationServiceImpl implements ResourcePolicyAuthorizationServiceImpl {
  private static final Logger LOGGER = LogManager.getLogger(AuthorizationServiceImpl.class);
  private final CatalogueService catService;

  public AuthorizationServiceImpl(CatalogueService catService) {
    this.catService = catService;
  }

  @Override
  public Future<Void> authorize(JwtData jwtData, String resourceId) {
    LOGGER.debug("Starting authorization for resource: {}, JWT iid: {}", resourceId, jwtData.iid());

      return getAccessPolicy(resourceId)
              .compose(policy -> {
                  if ("OPEN".equalsIgnoreCase(policy)) {
                      LOGGER.debug("Access policy is OPEN. Skipping ID validation.");
                      return Future.succeededFuture();
                  }

                  return idValidation(jwtData, resourceId)
                          .onSuccess(v -> LOGGER.debug("Authorization successful for resource: {}", resourceId));
              })
              .recover(error -> {
                  // Log the failure and propagate the error
                  LOGGER.error("Authorization failed for resource {}: {}", resourceId, error.getMessage());
                  return Future.failedFuture(error);
              });
  }

  private Future<String> getAccessPolicy(String resourceId) {
    if (resourceId == null) {
      LOGGER.error("Resource ID is null. Cannot fetch access policy.");
      return Future.failedFuture(new DxAuthException("Resource ID cannot be null"));
    }

    return catService
        .fetchCatalogueInfo(resourceId)
        .map(
            catInfo -> {
              String policy = catInfo.getString("accessPolicy");
              LOGGER.debug("Fetched access policy [{}] for resource: {}", policy, resourceId);
              return policy;
            })
        .recover(
            error -> {
              LOGGER.error(
                  "Failed to fetch access policy for resource {}: {}",
                  resourceId,
                  error.getMessage());
              return Future.failedFuture(
                  new DxAuthException("Unable to fetch access policy"));
            });
  }

  private Future<Void> idValidation(JwtData jwtData, String resourceId) {
    String jwtId = jwtData.iid().split(":")[1];

    if (resourceId.equalsIgnoreCase(jwtId)) {
      LOGGER.debug("Resource ID matches JWT iid.");
      return Future.succeededFuture();
    } else {
      LOGGER.error("JWT iid [{}] does not match resource ID [{}]", jwtId, resourceId);
      return Future.failedFuture(new DxAuthException("JWT iid does not match resource ID"));
    }
  }
}
