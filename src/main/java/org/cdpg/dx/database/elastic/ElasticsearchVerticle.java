package iudx.resource.server.database.elastic;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.resource.server.database.elastic.service.ElasticsearchService;
import iudx.resource.server.database.elastic.service.ElasticsearchServiceImpl;

import static iudx.resource.server.common.Constants.*;


/**
 * The Elasticsearch Verticle.
 *
 * <h1>Elasticsearch Verticle</h1>
 *
 * <p>The Elasticsearch Verticle implementation in the IUDX Catalogue Server exposes the {@link
 * ElasticsearchService} over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class ElasticsearchVerticle extends AbstractVerticle {

  private ElasticsearchService database;
  private String databaseIp;
  private String databaseUser;
  private String databasePassword;
  private int databasePort;
  private ElasticClient client;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   *
   * @throws Exception which is a start-up exception.
   */
  @Override
  public void start() throws Exception {
    binder = new ServiceBinder(vertx);
    databaseIp = config().getString(DATABASE_IP);
    databasePort = config().getInteger(DATABASE_PORT);
    databaseUser = config().getString(DATABASE_UNAME);
    databasePassword = config().getString(DATABASE_PASSWD);

    client = new ElasticClient(databaseIp, databasePort, databaseUser, databasePassword);

    database = new ElasticsearchServiceImpl(client);

    consumer =
        binder.setAddress(ELASTIC_SERVICE_ADDRESS).register(ElasticsearchService.class, database);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
