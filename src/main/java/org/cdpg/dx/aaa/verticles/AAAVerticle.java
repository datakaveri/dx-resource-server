package org.cdpg.dx.aaa.verticles;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.AAA_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.aaa.client.AAAClient;
import org.cdpg.dx.aaa.client.AAAWebClient;
import org.cdpg.dx.aaa.service.AAAService;
import org.cdpg.dx.aaa.service.AAAServiceImpl;

public class AAAVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(AAAVerticle.class);
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  @Override
  public void start(Promise<Void> startPromise) {
    try {
      validateConfig();

      JsonObject config = config();
      WebClientOptions webClientOptions = new WebClientOptions();
      webClientOptions.setTrustAll(true).setVerifyHost(false).setSsl(true);
      WebClient webClient = WebClient.create(vertx, webClientOptions);

      AAAClient aaaClient = new AAAWebClient(config, webClient);
      AAAService aaaService = new AAAServiceImpl(aaaClient);

      binder = new ServiceBinder(vertx);
      consumer = binder.setAddress(AAA_SERVICE_ADDRESS).register(AAAService.class, aaaService);
      LOGGER.info("AAAVerticle deployed successfully.");
      startPromise.complete();
    } catch (Exception e) {
      LOGGER.error("Failed to deploy AAAVerticle: {}", e.getMessage(), e);
      startPromise.fail(e);
    }
  }

  @Override
  public void stop() throws Exception {
    LOGGER.info("Shutting down AAAVerticle and unregistering service proxy.");
    if (binder != null && consumer != null) {
      binder.unregister(consumer);
    }
  }

  private void validateConfig() {
    checkConfigExists("dxAuthBasePath");
    checkConfigExists("authServerHost");
    checkConfigExists("authServerPort");
    checkConfigExists("clientId");
    checkConfigExists("clientSecret");
  }

  private void checkConfigExists(String key) {
    if (config().getValue(key) == null) {
      LOGGER.error("Configuration error: Missing required configuration: [{}]", key);
      throw new IllegalArgumentException("Missing required configuration: " + key);
    }
  }
}
