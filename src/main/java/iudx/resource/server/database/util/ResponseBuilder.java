package iudx.resource.server.database.util;

import static iudx.resource.server.database.elastic.Constants.DETAIL;
import static iudx.resource.server.database.elastic.Constants.ERROR_TYPE;
import static iudx.resource.server.database.elastic.Constants.FAILED;
import static iudx.resource.server.database.elastic.Constants.RESULTS;
import static iudx.resource.server.database.elastic.Constants.SUCCESS;
import static iudx.resource.server.database.elastic.Constants.TITLE;
import static iudx.resource.server.database.util.Constants.FROM_KEY;
import static iudx.resource.server.database.util.Constants.SIZE_KEY;
import static iudx.resource.server.database.util.Constants.TYPE_KEY;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;

public class ResponseBuilder {

  private String status;
  private JsonObject response;

  /** Initialise the object with Success or Failure. */
  public ResponseBuilder(String status) {
    this.status = status;
    response = new JsonObject();
  }

  public ResponseBuilder setTypeAndTitle(int statusCode) {
    response.put(ERROR_TYPE, statusCode);
    if (SUCCESS.equalsIgnoreCase(status)) {
      response.put(TYPE_KEY, ResponseUrn.SUCCESS_URN.getUrn());
      response.put(TITLE, ResponseUrn.SUCCESS_URN.getMessage());
    } else if (FAILED.equalsIgnoreCase(status)) {
      response.put(TITLE, FAILED);
    }
    return this;
  }

  public ResponseBuilder setTypeAndTitle(int statusCode, String title) {
    response.put(ERROR_TYPE, statusCode);
    response.put(TITLE, title);
    return this;
  }

  /** Successful Database Request with responses > 0. */
  public ResponseBuilder setMessage(JsonArray results) {
    response.put(RESULTS, results);
    return this;
  }

  /** Overloaded methods for Error messages. */
  public ResponseBuilder setMessage(String error) {
    response.put(DETAIL, error);
    return this;
  }

  public ResponseBuilder setMessage(JsonObject error) {
    /*int statusCode = error.getInteger(STATUS);
    String type = error.getJsonObject(ERROR.toLowerCase()).getString(TYPE_KEY);
    if (statusCode == 404 && INDEX_NOT_FOUND.equalsIgnoreCase(type)) {
      response.put(DETAIL, INVALID_RESOURCE_ID);
    } else {
      response.put(
          DETAIL,
          error
              .getJsonObject(ERROR.toLowerCase())
              .getJsonArray(ROOT_CAUSE)
              .getJsonObject(0)
              .getString(REASON));
    }*/
    return this;
  }

  public ResponseBuilder setCount(long count) {
    /*response.put(RESULTS, new JsonArray().add(new JsonObject().put(TOTAL_HITS, count)));*/
    return this;
  }

  public ResponseBuilder setFromParam(int from) {
    response.put(FROM_KEY, from);
    return this;
  }

  public ResponseBuilder setSizeParam(int size) {
    response.put(SIZE_KEY, size);
    return this;
  }

  public JsonObject getResponse() {
    return response;
  }
}
