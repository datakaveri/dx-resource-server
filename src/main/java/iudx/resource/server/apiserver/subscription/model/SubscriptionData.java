package iudx.resource.server.apiserver.subscription.model;

import io.vertx.core.json.JsonObject;

public class SubscriptionData {
  private JsonObject dataBrokerResult;
  private JsonObject cacheResult;
  private JsonObject streamingResult;

  public SubscriptionData(
      JsonObject dataBrokerResult, JsonObject cacheResult, JsonObject streamingResult) {
    this.dataBrokerResult = dataBrokerResult;
    this.cacheResult = cacheResult;
    this.streamingResult = streamingResult;
  }

  public JsonObject getDataBrokerResult() {
    return dataBrokerResult;
  }

  public JsonObject getCacheResult() {
    return cacheResult;
  }

  public JsonObject getStreamingResult() {
    return streamingResult;
  }

  @Override
  public String toString() {
    return "DataBrokerResult = "
        + dataBrokerResult
        + ", cacheResult = "
        + cacheResult
        + ", StreamingResult = "
        + streamingResult;
  }
}
