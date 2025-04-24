package org.cdpg.dx.databroker;

import static org.cdpg.dx.common.util.ProxyAddressConstants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.databroker.client.RabbitClient;
import org.cdpg.dx.databroker.client.RabbitWebClient;
import org.cdpg.dx.databroker.listeners.RevokeClientQlistener;
import org.cdpg.dx.databroker.listeners.UniqueAttribQlistener;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.databroker.service.DataBrokerServiceImpl;
import org.cdpg.dx.databroker.util.Vhosts;
import org.cdpg.dx.revoked.service.RevokedService;
import org.cdpg.dx.uniqueattribute.service.UniqueAttributeService;

public class DataBrokerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(DataBrokerVerticle.class);
  private DataBrokerService dataBrokerService;
  private RabbitMQOptions rabbitMQOptions;
  private String dataBrokerIp;
  private int dataBrokerPort;
  private int dataBrokerManagementPort;
  private String dataBrokerUserName;
  private String dataBrokerPassword;
  private int connectionTimeout;
  private int requestedHeartbeat;
  private int handshakeTimeout;
  private int requestedChannelMax;
  private int networkRecoveryInterval;
  private WebClientOptions webConfig;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private RabbitClient rabbitClient;
  private RabbitWebClient rabbitWebClient;
  /*private AsyncService asyncService;*/
  private RabbitMQClient iudxRabbitMqClient;
  private RabbitMQClient iudxInternalRabbitMqClient;
  private int amqpPort;
  private String amqpUrl;
  private RevokedService revokedService;
  private UniqueAttributeService uniqueAttributeService;

  @Override
  public void start() throws Exception {

    /* Read the configuration and set the rabbitMQ server properties. */
    dataBrokerIp = config().getString("dataBrokerIP");
    dataBrokerPort = config().getInteger("dataBrokerPort");
    dataBrokerManagementPort = config().getInteger("dataBrokerManagementPort");
    dataBrokerUserName = config().getString("dataBrokerUserName");
    dataBrokerPassword = config().getString("dataBrokerPassword");
    connectionTimeout = config().getInteger("connectionTimeout");
    requestedHeartbeat = config().getInteger("requestedHeartbeat");
    handshakeTimeout = config().getInteger("handshakeTimeout");
    requestedChannelMax = config().getInteger("requestedChannelMax");
    networkRecoveryInterval = config().getInteger("networkRecoveryInterval");
    amqpUrl = config().getString("brokerAmqpIp");
    amqpPort = config().getInteger("brokerAmqpPort");

    /* Configure the RabbitMQ Data Broker client with input from config files. */
    rabbitMQOptions = new RabbitMQOptions();
    rabbitMQOptions.setUser(dataBrokerUserName);
    rabbitMQOptions.setPassword(dataBrokerPassword);
    rabbitMQOptions.setHost(dataBrokerIp);
    rabbitMQOptions.setPort(dataBrokerPort);
    rabbitMQOptions.setConnectionTimeout(connectionTimeout);
    rabbitMQOptions.setRequestedHeartbeat(requestedHeartbeat);
    rabbitMQOptions.setHandshakeTimeout(handshakeTimeout);
    rabbitMQOptions.setRequestedChannelMax(requestedChannelMax);
    rabbitMQOptions.setNetworkRecoveryInterval(networkRecoveryInterval);
    rabbitMQOptions.setAutomaticRecoveryEnabled(true);

    String externalVhost = config().getString(Vhosts.IUDX_EXTERNAL.value);

    RabbitMQOptions iudxConfig = new RabbitMQOptions(rabbitMQOptions);
    String prodVhost = config().getString(Vhosts.IUDX_PROD.value);

    RabbitMQOptions iudxInternalConfig = new RabbitMQOptions(rabbitMQOptions);
    String iudxInternalVhost = config().getString(Vhosts.IUDX_INTERNAL.value);

    iudxConfig.setVirtualHost(prodVhost);
    iudxInternalConfig.setVirtualHost(iudxInternalVhost);

    webConfig = new WebClientOptions();
    webConfig.setKeepAlive(true);
    webConfig.setConnectTimeout(86400000);
    webConfig.setDefaultHost(dataBrokerIp);
    webConfig.setDefaultPort(dataBrokerManagementPort);
    webConfig.setKeepAliveTimeout(86400000);

    /* Create a RabbitMQ Clinet with the configuration and vertx cluster instance. */
    RabbitMQClient.create(vertx, rabbitMQOptions);

    /* Create a Vertx Web Client with the configuration and vertx cluster instance. */
    WebClient.create(vertx, webConfig);

    /* Create a Json Object for properties */
    JsonObject propObj = new JsonObject();

    propObj.put("username", dataBrokerUserName);
    propObj.put("password", dataBrokerPassword);

    /* Call the databroker constructor with the RabbitMQ client. */

    rabbitWebClient = new RabbitWebClient(vertx, webConfig, propObj);
    iudxRabbitMqClient = RabbitMQClient.create(vertx, iudxConfig);
    iudxInternalRabbitMqClient = RabbitMQClient.create(vertx, iudxInternalConfig);
    rabbitClient =
        new RabbitClient(rabbitWebClient, iudxInternalRabbitMqClient, iudxRabbitMqClient);
    binder = new ServiceBinder(vertx);

    /* Create RabbitMQ listeners for revoke client queue, unique attribute queue and async query queue. */
    revokedService = RevokedService.createProxy(vertx, REVOKED_SERVICE_ADDRESS);
    uniqueAttributeService = UniqueAttributeService.createProxy(vertx, UNIQUE_ATTRIBUTE_SERVICE_ADDRESS);
    RevokeClientQlistener revokeQlistener =
            new RevokeClientQlistener(iudxInternalRabbitMqClient, revokedService);
    UniqueAttribQlistener uniqueAttrQlistener =
            new UniqueAttribQlistener(iudxInternalRabbitMqClient, uniqueAttributeService);

    revokeQlistener.start();
    uniqueAttrQlistener.start();

    dataBrokerService =
        new DataBrokerServiceImpl(
            rabbitClient, amqpUrl, amqpPort, iudxInternalVhost, prodVhost, externalVhost);

    /* Publish the Data Broker service with the Event Bus against an address. */

    consumer =
        binder
            .setAddress(DATA_BROKER_SERVICE_ADDRESS)
            .register(DataBrokerService.class, dataBrokerService);
  }

  @Override
  public void stop() throws Exception {
    binder.unregister(consumer);
  }
}
