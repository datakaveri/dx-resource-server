package iudx.resource.server.databroker.model;

import static iudx.resource.server.apiserver.subscription.util.Constants.RESULTS;
import static iudx.resource.server.databroker.util.Constants.*;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject
public class ListStreamingSubsModel {

  private String type;
  private String title;
  private JsonObject result;

  public ListStreamingSubsModel(String type, String success, JsonObject result) {
    this.type = type;
    this.title = success;
    this.result = result;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put(TYPE, type);
    json.put(TITLE, title);
    json.put(RESULTS, new JsonArray().add(result));
    return json;
  }
}
