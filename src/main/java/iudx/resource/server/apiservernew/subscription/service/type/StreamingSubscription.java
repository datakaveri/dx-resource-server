package iudx.resource.server.apiservernew.subscription.service.type;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.databrokernew.service.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StreamingSubscription {

  private static final Logger LOGGER = LogManager.getLogger(StreamingSubscription.class);

  private DataBrokerService databroker;

  public StreamingSubscription(DataBrokerService databroker) {
    this.databroker = databroker;
  }

  public Future<JsonObject> create(JsonObject subscription) {
    return null;
  }

  public Future<JsonObject> update(JsonObject subscription) {
    return null;
  }

  public Future<JsonObject> append(JsonObject subscription) {
    return null;
  }

  public Future<JsonObject> delete(JsonObject subscription) {
    return null;
  }

  public Future<JsonObject> get(JsonObject subscription) {
    return null;
  }
}
