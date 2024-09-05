package iudx.resource.server.authenticator.authorization;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.Api;

public class AdminAuthStrategy implements AuthorizationStrategy {
  private static volatile AdminAuthStrategy instance;

  private AdminAuthStrategy() {
    buildPermissions();
  }

  public static AdminAuthStrategy getInstance(Api api) {
    if (instance == null) {
      synchronized (AdminAuthStrategy.class) {
        if (instance == null) {
          instance = new AdminAuthStrategy();
        }
      }
    }
    return instance;
  }

  private void buildPermissions() {}

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }

  /*@Override
  public boolean isAuthorized(
      AuthorizationRequest authRequest, JwtData jwtData, JsonObject quotaConsumed) {
    return isAuthorized(authRequest, jwtData);
  }*/
}
