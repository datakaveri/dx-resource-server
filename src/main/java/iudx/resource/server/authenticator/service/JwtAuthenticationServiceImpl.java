package iudx.resource.server.authenticator;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import iudx.resource.server.authenticator.model.JwtData;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JwtAuthenticationServiceImpl implements AuthenticationService {
  private static final Logger LOGGER = LogManager.getLogger(JwtAuthenticationServiceImpl.class);
  final JWTAuth jwtAuth;

  public JwtAuthenticationServiceImpl(final JWTAuth jwtAuth) {
    this.jwtAuth = jwtAuth;
  }

  @Override
  public Future<JwtData> decodeToken(String token) {
    Promise<JwtData> promise = Promise.promise();
    TokenCredentials creds = new TokenCredentials(token);
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
              LOGGER.error("failed to decode/validate jwt token : {}", err.getMessage());
              promise.fail(err.getMessage());
            });

    return promise.future();
  }
}
