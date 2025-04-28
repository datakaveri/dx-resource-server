package org.cdpg.dx.rs.admin.service;

import io.vertx.core.Future;

public interface AdminService {
    Future<Void> revokedTokenRequest(String userId);

    Future<Void> createUniqueAttribute(String id, String attribute);

    Future<Void> updateUniqueAttribute(String id, String attribute);

    Future<Void> deleteUniqueAttribute(String id);
}
