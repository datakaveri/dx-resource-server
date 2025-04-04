package org.cdpg.dx.auditing.model;

import io.vertx.core.json.JsonObject;

public interface AuditLog {
     JsonObject toJson();
}