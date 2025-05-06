package org.cdpg.dx.rs.authorization.service;

import io.vertx.core.Future;
import org.cdpg.dx.common.models.JwtData;

public interface ResourcePolicyAuthorizationServiceImpl {
  Future<Void> authorize(JwtData JwtData, String resourceId);
}
