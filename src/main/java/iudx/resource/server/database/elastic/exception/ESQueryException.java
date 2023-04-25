package iudx.resource.server.database.elastic.exception;

import static iudx.resource.server.database.archives.Constants.*;
import org.apache.http.HttpStatus;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;

public class ESQueryException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int statusCode;
  private final iudx.resource.server.common.ResponseUrn urn;
  private final String message;

  public ESQueryException(final String message) {
    this(ResponseUrn.BAD_REQUEST_URN, message, HttpStatus.SC_BAD_REQUEST);
  }

  public ESQueryException(final ResponseUrn urn, final String message) {
    this(urn, message, HttpStatus.SC_BAD_REQUEST);
  }

  public ESQueryException(final ResponseUrn urn, final String message, final int statusCode) {
    super(message);
    this.urn = urn;
    this.message = message;
    this.statusCode = statusCode;
  }

  public String toString() {
    JsonObject json = new JsonObject();
    json.put("status", statusCode);
    json.put(TYPE_KEY, urn.getUrn());
    json.put(TITLE, urn.getMessage());
    json.put(DETAIL, message);
    return json.toString();
  }
}
