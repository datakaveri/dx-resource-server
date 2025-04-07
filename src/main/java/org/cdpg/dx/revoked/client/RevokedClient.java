package org.cdpg.dx.revoked.client;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.util.List;

public interface RevokedClient {
  Future<List<JsonObject>> fetchRevokedData();
}
