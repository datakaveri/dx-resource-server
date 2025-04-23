package org.cdpg.dx.container;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.databroker.client.RabbitClient;
import org.cdpg.dx.databroker.client.RabbitWebClient;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;

public class RMQContainer {
  @Container
  public static final RabbitMQContainer rabbitMQContainer =
      new RabbitMQContainer("rabbitmq:4.0-management")
          .withExposedPorts(5672, 15672)
          .withEnv("RABBITMQ_DEFAULT_USER", "guest")
          .withEnv("RABBITMQ_DEFAULT_PASS", "guest")
          .withEnv("RABBITMQ_DEFAULT_VHOST", "iudx")
          .withNetwork(Network.newNetwork());

  private static final Logger LOGGER = LogManager.getLogger(RMQContainer.class);
  /*private final RabbitMQContainer container;*/
  private final RabbitMQClient rabbitMQClient;
  private final RabbitWebClient rabbitWebClient;
  private final RabbitClient rabbitClient;

  public RMQContainer(Vertx vertx) throws IOException, InterruptedException {
    /*this.container = new RabbitMQContainer("rabbitmq:4.0-management")
    .withExposedPorts(5672, 15672)
    .withEnv("RABBITMQ_DEFAULT_USER", "guest")
    .withEnv("RABBITMQ_DEFAULT_PASS", "guest")
    .withEnv("RABBITMQ_DEFAULT_VHOST", "iudx");*/

    /*container.start();*/

    rabbitMQContainer.start();

    String host = /*container.getHost()*/ rabbitMQContainer.getHost();
    int amqpPort = rabbitMQContainer.getAmqpPort();
    int mgmtPort = rabbitMQContainer.getMappedPort(15672);
    rabbitMQContainer.execInContainer("rabbitmqctl", "set_user_tags", "guest", "administrator");
    rabbitMQContainer.execInContainer(
        "rabbitmqctl", "set_permissions", "-p", "/", "guest", ".*", ".*", ".*");
    // Setup RabbitMQClient
    RabbitMQOptions rmqOptions =
        new RabbitMQOptions()
            .setUser("guest")
            .setPassword("guest")
            .setVirtualHost("iudx")
            .setHost(host)
            .setPort(amqpPort);

    rabbitMQClient = RabbitMQClient.create(vertx, rmqOptions);
    rabbitMQClient.start().toCompletionStage().toCompletableFuture().join();
    LOGGER.info("RabbitWebClient connecting to {}:{}", host, mgmtPort);
    // Setup RabbitWebClient
    WebClientOptions webClientOptions =
        new WebClientOptions().setDefaultHost(host).setDefaultPort(mgmtPort).setSsl(false);

    JsonObject credentials = new JsonObject().put("username", "guest").put("password", "guest");

    rabbitWebClient = new RabbitWebClient(vertx, webClientOptions, credentials);

    // Final combined client
    rabbitClient = new RabbitClient(rabbitWebClient, rabbitMQClient, rabbitMQClient);
  }

  public RabbitMQContainer getContainer() {
    return rabbitMQContainer;
  }

  public RabbitClient getRabbitClient() {
    return rabbitClient;
  }

  public void stop() {
    // rabbitMQClient.stop();
    rabbitMQContainer.stop();
    LOGGER.debug("RMQ container stopped completed");
  }
}
