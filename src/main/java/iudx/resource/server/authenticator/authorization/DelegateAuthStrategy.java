package iudx.resource.server.authenticator.authorization;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.Api;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DelegateAuthStrategy implements AuthorizationStrategy {

  static Map<String, List<AuthorizationRequest>> delegateAuthorizationRules = new HashMap<>();
  private static volatile DelegateAuthStrategy instance;

  private DelegateAuthStrategy() {
    buildPermissions();
  }

  public static DelegateAuthStrategy getInstance(Api api) {

    if (instance == null) {
      synchronized (DelegateAuthStrategy.class) {
        if (instance == null) {
          instance = new DelegateAuthStrategy();
        }
      }
    }
    return instance;
  }

  private void buildPermissions() {
    // delegate allowed to access all endpoints
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }

 /* @Override
  public boolean isAuthorized(
      AuthorizationRequest authRequest, JwtData jwtData, JsonObject quotaConsumed) {
    return isAuthorized(authRequest, jwtData);
  }*/
}
