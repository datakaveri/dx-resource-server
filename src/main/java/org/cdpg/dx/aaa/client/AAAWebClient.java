package org.cdpg.dx.aaa.client;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.DxAuthException;
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.cdpg.dx.common.exception.DxInternalServerErrorException;

/** Client for communicating with the authentication server. */
public class AAAWebClient implements AAAClient {
  public static final String AUTH_CERTIFICATE_PATH = "/cert";
  private static final Logger LOGGER = LogManager.getLogger(AAAWebClient.class);
  private static final String AUTH_SERVER_PATH = "/search";
  private final WebClient client;
  private final String authServerHost;
  private final String clientId;
  private final String clientSecret;
  private final int authServerPort;
  private final String authServerSearchPath;
  private final String authServerCertPath;

  public AAAWebClient(JsonObject config, WebClient webClient) {
    this.client = webClient;
    this.authServerHost = config.getString("authServerHost");
    this.authServerSearchPath = config.getString("dxAuthBasePath") + AUTH_SERVER_PATH;
    this.clientId = config.getString("clientId");
    this.clientSecret = config.getString("clientSecret");
    this.authServerPort = config.getInteger("authServerPort");
    this.authServerCertPath = config.getString("dxAuthBasePath") + AUTH_CERTIFICATE_PATH;
  }

  /** Fetches and validates user data from the authentication server. */
  public Future<JsonObject> fetchUserData(String userId, String role, String resourceServer) {
    LOGGER.info(
        "Fetching user data for userId: {}, role: {}, resourceServer: {}",
        userId,
        role,
        resourceServer);

    return client
        .get(authServerPort, authServerHost, authServerSearchPath)
        .putHeader("clientId", clientId)
        .putHeader("clientSecret", clientSecret)
        .addQueryParam("role", role)
        .addQueryParam("userId", userId)
        .addQueryParam("resourceServer", resourceServer)
        .send()
        .compose(this::handleResponse)
        .recover(
            error -> {
              LOGGER.error(
                  "Failed to fetch user data for userId {}: {}", userId, error.getMessage());
              return Future.failedFuture(error);
            });
  }

  @Override
  public Future<String> fetchCertKey() {
    return client
        .get(authServerPort, authServerHost, authServerCertPath)
        .send()
        .compose(
            response -> {
              if (response.statusCode() == 200) {
                JsonObject json = response.bodyAsJsonObject();
                if (json != null && json.containsKey("cert")) {
                  return Future.succeededFuture(json.getString("cert"));
                } else {
                  return Future.failedFuture(new DxAuthException("Response does not contain 'cert' field"));
                }
              } else {
                return Future.failedFuture(new DxInternalServerErrorException("Failed to fetch certificate"));
              }
            });
  }

  /** Handles and validates the response from the authentication server. */
  private Future<JsonObject> handleResponse(HttpResponse<Buffer> response) {
    int statusCode = response.statusCode();

    if (statusCode < 200 || statusCode >= 300) {
      LOGGER.warn(
          "Auth Server request failed - Status: {} - {}", statusCode, response.statusMessage());
      return Future.failedFuture(new DxAuthException(response.statusMessage()));
    }

    JsonObject responseBody = response.bodyAsJsonObject();
    LOGGER.debug("Auth Server Response: {}", responseBody.encodePrettily());

    if (!"urn:dx:as:Success".equals(responseBody.getString("type"))) {
      LOGGER.warn("User not found in Auth.");
      return Future.failedFuture(new DxAuthException("User not present in auth"));
    }

    JsonObject result = responseBody.getJsonObject("results");
    if (result == null) {
      LOGGER.error("Auth response does not contain 'results' field");
      return Future.failedFuture(new DxBadRequestException("Auth response does not contain 'results' field"));
    }

    return Future.succeededFuture(result);
  }
}
