package org.cdpg.dx.authenticator.service;


import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.authenticator.AuthenticationService;
import org.cdpg.dx.authenticator.model.JwtData;

public class JwtAuthenticationServiceImpl implements AuthenticationService {

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);
  final JWTAuth jwtAuth;

  public JwtAuthenticationServiceImpl(final JWTAuth jwtAuth) {
    this.jwtAuth = jwtAuth;
  }

  @Override
  public Future<JwtData> tokenIntrospect(JsonObject authenticationInfo) {

    LOGGER.info("authInfo " + authenticationInfo);
    String token = authenticationInfo.getString("token");
    return decodeJwt(token);
  }

  Future<JwtData> decodeJwt(String jwtToken) {
    Promise<JwtData> promise = Promise.promise();
    TokenCredentials creds = new TokenCredentials(jwtToken);

    jwtAuth
        .authenticate(creds)
        .onSuccess(
            user -> {
              JwtData jwtData = new JwtData(user.principal());
              jwtData.setExp(user.get("exp"));
              jwtData.setIat(user.get("iat"));
              String exp =
                  LocalDateTime.ofInstant(
                          Instant.ofEpochSecond(jwtData.getExp()), ZoneId.systemDefault())
                      .toString();
              jwtData.setExpiry(exp);
              promise.complete(jwtData);
            })
        .onFailure(
            err -> {
              LOGGER.error("failed to decode/validate jwt token : " + err.getMessage());
              promise.fail("failed");
            });

    return promise.future();
  }
}