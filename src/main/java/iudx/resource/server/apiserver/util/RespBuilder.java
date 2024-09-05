package iudx.resource.server.apiserver.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static iudx.resource.server.database.archives.Constants.TOTAL_HITS;
import static iudx.resource.server.databroker.util.Constants.*;


public class RespBuilder {
  private JsonObject response = new JsonObject();

  public RespBuilder withType(String type) {
    response.put(TYPE, type);
    return this;
  }

  public RespBuilder withTitle(String title) {
    response.put(TITLE, title);
    return this;
  }

  public RespBuilder withDetail(String detail) {
    response.put(DETAIL, detail);
    return this;
  }

  public RespBuilder withTotalHits(Integer count) {
    response.put(TOTAL_HITS, count);
    return this;
  }

  /**
   * Adds a result to the response with the given id, method, and status.
   * @param id The id of the result to be added
   * @param method The method used to produce the result
   * @param status The status of the result
   * @return This RespBuilder object with the added result
   */
  public RespBuilder withResult(String id, String method, String status) {
    JsonObject resultAttrs = new JsonObject().put(ID, id)
        .put(METHOD, method)
        .put(STATUS, status);
    response.put(RESULTS, new JsonArray().add(resultAttrs));
    return this;
  }

  /**
   * Adds a result with the provided attributes to the response.
   * @param id the ID of the result
   * @param method the method used to perform the operation resulting in this result
   * @param status the status of the operation resulting in this result
   * @param detail the details of the operation resulting in this result
   * @return this RespBuilder instance
   */
  public RespBuilder withResult(String id, String method, String status, String detail) {
    JsonObject resultAttrs = new JsonObject().put(ID, id)
        .put(METHOD, method)
        .put(STATUS, status)
        .put(DETAIL, detail);
    response.put(RESULTS, new JsonArray().add(resultAttrs));
    return this;
  }

  /**
   * Adds a result with the provided ID and detail information to the response.
   * @param id the ID of the result
   * @param detail the detail information of the result
   * @return the updated {@code RespBuilder} object
   */
  public RespBuilder withResult(String id, String detail) {
    JsonObject resultAttrs = new JsonObject().put(ID, id)
            .put(DETAIL, detail);
    response.put(RESULTS, new JsonArray().add(resultAttrs));
    return this;
  }

  /**
   * Adds a result to the response with the provided ID.
   * @param id The ID of the result to be added.
   * @return Returns the updated instance of the RespBuilder.
   */
  public RespBuilder withResult(String id) {
    JsonObject resultAttrs = new JsonObject().put(ID, id);
    response.put(RESULTS, new JsonArray().add(resultAttrs));
    return this;
  }

  public RespBuilder withResult() {
    response.put(RESULTS, new JsonArray());
    return this;
  }

  public RespBuilder withResult(JsonArray results) {
    response.put(RESULTS, results);
    return this;
  }

  public RespBuilder withResult(JsonObject results) {
    response.put(RESULTS, results);
    return this;
  }

  public JsonObject getJsonResponse() {
    return response;
  }

  public String getResponse() {
    return response.toString();
  }

}
