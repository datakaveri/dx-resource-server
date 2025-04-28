package org.cdpg.dx.auth.authentication.client;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SecretKeyClientImpl implements SecretKeyClient {
  private static final Logger LOGGER = LogManager.getLogger(SecretKeyClientImpl.class);

  private final WebClient webClient;
  private final int authServerPort;
  private final String authServerHost;
  private final String authServerCertPath;

  public SecretKeyClientImpl(JsonObject config, Vertx vertx) {
    ;
    this.authServerPort = config.getInteger("authServerPort");
    this.authServerHost = config.getString("authServerHost");
    this.authServerCertPath = config.getString("dxAuthBasePath") + "/cert";
    this.webClient = getWebClient(vertx);
  }

  private WebClient getWebClient(Vertx vertx) {
    WebClientOptions webClientOptions = new WebClientOptions();
    webClientOptions.setTrustAll(true).setVerifyHost(false).setSsl(true);
    return WebClient.create(vertx, webClientOptions);
  }

  @Override
  public Future<String> fetchCertKey() {
    LOGGER.info("Fetching certificate key from {}:{}", authServerHost, authServerCertPath);

    return webClient
        .get(authServerPort, authServerHost, authServerCertPath)
        .send()
        .compose(
            response -> {
              if (response.statusCode() == 200) {
                JsonObject json = response.bodyAsJsonObject();
                if (json != null && json.containsKey("cert")) {
                  LOGGER.info("Successfully fetched certificate key.");
                  return Future.succeededFuture(json.getString("cert"));
                } else {
                  LOGGER.error("Response does not contain 'cert' field.");
                  return Future.failedFuture("Response does not contain 'cert' field.");
                }
              } else {
                String errorMessage =
                    "Failed to fetch JWT public key, HTTP status: " + response.statusCode();
                LOGGER.error(errorMessage);
                return Future.failedFuture(errorMessage);
              }
            })
        .recover(
            error -> {
              LOGGER.error("Error fetching certificate key: {}", error.getMessage());
              return Future.failedFuture(error);
            });
  }
}
