package org.cdpg.dx.rs.latest.factory;

import org.cdpg.dx.auditing.handler.AuditingHandler;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.database.redis.service.RedisService;
import org.cdpg.dx.rs.authorization.handler.ResourcePolicyAuthorizationHandler;
import org.cdpg.dx.rs.latest.controller.LatestController;
import org.cdpg.dx.rs.latest.service.LatestService;
import org.cdpg.dx.rs.latest.service.LatestServiceImpl;
import org.cdpg.dx.uniqueattribute.service.UniqueAttributeService;

public class LatestControllerFactory {

  public static LatestController create(
      RedisService redisService,
      UniqueAttributeService uniqueAttributeService,
      ClientRevocationValidationHandler clientRevocationValidationHandler,
      ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler,
      AuditingHandler auditingHandler,
      String tenantPrefix) {

    LatestService latestService =
        new LatestServiceImpl(redisService, tenantPrefix, uniqueAttributeService);

    return new LatestController(
        latestService, resourcePolicyAuthorizationHandler,
        clientRevocationValidationHandler, auditingHandler);
  }
}
