package org.cdpg.dx.databroker.client;

import static org.cdpg.dx.databroker.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RabbitWebClient {
    private static final Logger LOGGER = LogManager.getLogger(RabbitWebClient.class);
    static WebClient webClient;
    private String username;
    private String password;

    public RabbitWebClient(Vertx vertx, WebClientOptions webClientOptions, JsonObject propJson) {
        this.username = propJson.getString("username");
        this.password = propJson.getString("password");
        if (webClient == null) {
            webClient = getRabbitMqWebClient(vertx, webClientOptions);
        }
    }

  private WebClient getRabbitMqWebClient(Vertx vertx, WebClientOptions webClientOptions) {
    return WebClient.create(vertx, webClientOptions);
  }

    public Future<HttpResponse<Buffer>> requestAsync(
            String requestType, String url, JsonObject requestJson) {
        LOGGER.trace("Info : RabbitMQClientImpl#requestAsync() started");
        Promise<HttpResponse<Buffer>> promise = Promise.promise();
        HttpRequest<Buffer> webRequest = createRequest(requestType, url);
        webRequest.sendJsonObject(
                requestJson,
                ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        promise.complete(response);
                    } else {
                        promise.fail(ar.cause());
                    }
                });
        return promise.future();
    }

    public Future<HttpResponse<Buffer>> requestAsync(String requestType, String url) {
        LOGGER.trace("Info : RabbitMQClientImpl#requestAsync() started");
        Promise<HttpResponse<Buffer>> promise = Promise.promise();
        HttpRequest<Buffer> webRequest = createRequest(requestType, url);
        webRequest.send(
                ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        promise.complete(response);
                    } else {
                        promise.fail(ar.cause());
                    }
                });
        return promise.future();
    }

    private HttpRequest<Buffer> createRequest(String requestType, String url) {
        HttpRequest<Buffer> webRequest = null;
        switch (requestType) {
            case REQUEST_GET:
                webRequest = webClient.get(url).basicAuthentication(username, password);
                break;
            case REQUEST_POST:
                webRequest = webClient.post(url).basicAuthentication(username, password);
                break;
            case REQUEST_PUT:
                webRequest = webClient.put(url).basicAuthentication(username, password);
                break;
            case REQUEST_DELETE:
                webRequest = webClient.delete(url).basicAuthentication(username, password);
                break;
            default:
                break;
        }
        return webRequest;
    }

}
