package iudx.resource.server.authenticator.authorization;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;

public final class JwtAuthorization {

  private final AuthorizationStrategy authStrategy;

  public JwtAuthorization(final AuthorizationStrategy authStrategy) {
    this.authStrategy = authStrategy;
  }

  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return authStrategy.isAuthorized(authRequest, jwtData);
  }

  public boolean isAuthorized(
      AuthorizationRequest authRequest, JwtData jwtData, JsonObject userQuotaLimit) {
    /*if (authStrategy instanceof ConsumerAuthStrategy) {
      return authStrategy.isAuthorized(authRequest, jwtData, userQuotaLimit);
    }*/
    return this.isAuthorized(authRequest, jwtData);
  }
}
