package iudx.resource.server.authenticator.authorization;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.Api;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderAuthStrategy implements AuthorizationStrategy {

  static Map<String, List<AuthorizationRequest>> providerAuthorizationRules = new HashMap<>();
  private static volatile ProviderAuthStrategy instance;

  private ProviderAuthStrategy() {
    buildPermissions();
  }

  public static ProviderAuthStrategy getInstance(Api api) {

    if (instance == null) {
      synchronized (ProviderAuthStrategy.class) {
        if (instance == null) {
          instance = new ProviderAuthStrategy();
        }
      }
    }
    return instance;
  }

  private void buildPermissions() {
    // provider allowed to access all endpoints
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }

 /* @Override
  public boolean isAuthorized(
      AuthorizationRequest authRequest, JwtData jwtData, JsonObject quotaConsumed) {
    // TODO Auto-generated method stub
    return isAuthorized(authRequest, jwtData);
  }*/
}
