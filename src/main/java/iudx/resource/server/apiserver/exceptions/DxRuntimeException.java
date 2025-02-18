package iudx.resource.server.apiserver.exceptions;

import iudx.resource.server.common.ResponseUrn;

public final class DxRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int statusCode;
  private final ResponseUrn urn;
  private final String message;

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