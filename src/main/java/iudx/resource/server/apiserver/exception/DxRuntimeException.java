package iudx.resource.server.apiserver.exception;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DxRuntimeException extends RuntimeException {
  private static final Logger LOGGER = LogManager.getLogger(DxRuntimeException.class);
  private static final long serialVersionUID = 1L;
  private int statusCode;
  private ResponseUrn urn;
  private String message;

  public DxRuntimeException(final int statusCode, final ResponseUrn urn) {
    super();
    this.statusCode = statusCode;
    this.urn = urn;
    this.message = urn.getMessage();
  }

  public DxRuntimeException(final int statusCode, final ResponseUrn urn, final String message) {
    super(message);
    this.statusCode = statusCode;
    this.urn = urn;
    this.message = message;
  }

  public DxRuntimeException(final int statusCode, final ResponseUrn urn, final Throwable cause) {
    super(cause);
    this.statusCode = statusCode;
    this.urn = urn;
    this.message = urn.getMessage();
  }

  public DxRuntimeException(String message) {
    super(message);
    LOGGER.debug("Info : " + message);
    try {
      JsonObject resultJson = new JsonObject(message);
      String urnTitle = resultJson.getString("type");
      this.statusCode = resultJson.getInteger("status");
      this.urn = ResponseUrn.fromCode(urnTitle);
      this.message = resultJson.getString("detail", urn.getMessage());
    } catch (Exception e) {
      LOGGER.debug("Exception : " + e);
    }
  }

  @Override
  public String toString() {
    return "DxRuntimeException{"
        + "statusCode="
        + statusCode
        + ", urn="
        + urn
        + ", message='"
        + message
        + '\''
        + '}';
  }

  public int getStatusCode() {
    return statusCode;
  }

  public ResponseUrn getUrn() {
    return urn;
  }

  public String getMessage() {
    return message;
  }
}
