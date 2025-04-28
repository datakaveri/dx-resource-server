package org.cdpg.dx.rs.usermanagement.service;

import io.vertx.core.Future;

public interface UserManagementService {
  Future<Void> resetPassword(String userId);
}
