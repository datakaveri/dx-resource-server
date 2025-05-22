package org.cdpg.dx.auth.authentication.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import java.util.Collections;
import org.cdpg.dx.aaa.service.AAAService;
import org.cdpg.dx.auth.authentication.client.SecretKeyClient;
import org.cdpg.dx.common.exception.DxBadRequestException;

public class JwtAuthProvider {

  private static JWTAuth jwtAuthInstance;

  private JwtAuthProvider() {
    // private constructor to prevent instantiation
  }

  public static Future<JWTAuth> init(
      Vertx vertx, JsonObject config, SecretKeyClient secretKeyClient) {
    if (jwtAuthInstance != null) {
      return Future.succeededFuture(jwtAuthInstance); // already initialized
    }

    return secretKeyClient
        .fetchCertKey()
        .compose(
            cert -> {
              if (cert == null || cert.isEmpty()) {
                return Future.failedFuture(new DxBadRequestException("Public key (certificate) is empty or null"));
              }
              JWTAuthOptions options =
                  new JWTAuthOptions()
                      .addPubSecKey(new PubSecKeyOptions().setAlgorithm("ES256").setBuffer(cert))
                      .setJWTOptions(
                          new JWTOptions()
                              .setLeeway(30)
                              .setIgnoreExpiration(config.getBoolean("jwtIgnoreExpiry", false))
                              .setIssuer(config.getString("iss"))
                              .setAudience(Collections.singletonList(config.getString("aud"))));
              jwtAuthInstance = JWTAuth.create(vertx, options);
              return Future.succeededFuture(jwtAuthInstance);
            });
  }

  public static JWTAuth getInstance() {
    if (jwtAuthInstance == null) {
      throw new IllegalStateException("JWTAuthProvider not initialized. Call init() first.");
    }
    return jwtAuthInstance;
  }
}
