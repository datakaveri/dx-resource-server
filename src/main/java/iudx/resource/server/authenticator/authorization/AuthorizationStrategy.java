package iudx.resource.server.authenticator.authorization;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;

public interface AuthorizationStrategy {

  boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData);

  /*boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData, JsonObject allowedLimits);*/
}
