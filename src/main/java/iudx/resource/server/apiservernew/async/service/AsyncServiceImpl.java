package iudx.resource.server.apiservernew.async.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class AsyncServiceImpl implements AsyncService {
  @Override
  public Future<Void> asyncSearch(
      String requestId,
      String sub,
      String searchId,
      JsonObject query,
      String format,
      String role,
      String drl,
      String did) {
    return null;
  }

  @Override
  public Future<JsonObject> asyncStatus(String sub, String searchId) {
    return null;
  }
}
