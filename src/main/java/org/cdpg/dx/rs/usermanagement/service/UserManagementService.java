package org.cdpg.dx.rs.usermanagement.service;

import io.vertx.core.Future;
import org.cdpg.dx.rs.usermanagement.model.ResetPassword;

public interface UserManagementService {
  Future<ResetPassword> resetPassword(String userId);
}
