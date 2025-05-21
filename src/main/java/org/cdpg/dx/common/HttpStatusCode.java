package org.cdpg.dx.common;

import org.cdpg.dx.common.config.URNConstants;

public enum HttpStatusCode {

  // 1xx: Informational
  CONTINUE(100, "Continue", URNConstants.SERVER + "continue"),
  SWITCHING_PROTOCOLS(101, "Switching Protocols", URNConstants.SERVER + "switchingProtocols"),
  PROCESSING(102, "Processing", URNConstants.SERVER + "processing"),
  EARLY_HINTS(103, "Early Hints", URNConstants.SERVER + "earlyHints"),

  // 2XX: codes
  NO_CONTENT(204, "No Content", URNConstants.SERVER + "noContent"),
  SUCCESS(200, "Success", URNConstants.SERVER + "success"),
  CREATED(201, "Created", URNConstants.SERVER + "success"),

  // 4xx: Client Error
  BAD_REQUEST(400, "Bad Request", URNConstants.SERVER + "badRequest"),
  UNAUTHORIZED(401, "Not Authorized", URNConstants.SERVER + "notAuthorized"),
  PAYMENT_REQUIRED(402, "Payment Required", URNConstants.SERVER + "paymentRequired"),
  FORBIDDEN(403, "Forbidden", URNConstants.SERVER + "forbidden"),
  VERIFY_FORBIDDEN(403, "Policy does not exist", "urn:apd:Deny"),
  NOT_FOUND(404, "Not Found", URNConstants.SERVER + "notFound"),
  METHOD_NOT_ALLOWED(405, "Method Not Allowed", URNConstants.SERVER + "methodNotAllowed"),
  NOT_ACCEPTABLE(406, "Not Acceptable", URNConstants.SERVER + "notAcceptable"),
  PROXY_AUTHENTICATION_REQUIRED(
      407, "Proxy Authentication Required", URNConstants.SERVER + "proxyAuthenticationRequired"),
  REQUEST_TIMEOUT(408, "Request Timeout", URNConstants.SERVER + "requestTimeout"),
  CONFLICT(409, "Conflict", URNConstants.SERVER + "conflict"),
  GONE(410, "Gone", URNConstants.SERVER + "gone"),
  LENGTH_REQUIRED(411, "Length Required", URNConstants.SERVER + "lengthRequired"),
  PRECONDITION_FAILED(412, "Precondition Failed", URNConstants.SERVER + "preconditionFailed"),
  REQUEST_TOO_LONG(413, "Payload Too Large", URNConstants.SERVER + "payloadTooLarge"),
  REQUEST_URI_TOO_LONG(414, "URI Too Long", URNConstants.SERVER + "uriTooLong"),
  UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type", URNConstants.SERVER + "unsupportedMediaType"),
  REQUESTED_RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable", URNConstants.SERVER + "rangeNotSatisfiable"),
  EXPECTATION_FAILED(417, "Expectation Failed", URNConstants.SERVER + "expectation Failed"),
  MISDIRECTED_REQUEST(421, "Misdirected Request", URNConstants.SERVER + "misdirected Request"),
  UNPROCESSABLE_ENTITY(422, "Unprocessable Entity", URNConstants.SERVER + "unprocessableEntity"),
  LOCKED(423, "Locked", URNConstants.SERVER + "locked"),
  FAILED_DEPENDENCY(424, "Failed Dependency", URNConstants.SERVER + "failedDependency"),
  TOO_EARLY(425, "Too Early", URNConstants.SERVER + "tooEarly"),
  UPGRADE_REQUIRED(426, "Upgrade Required", URNConstants.SERVER + "upgradeRequired"),
  PRECONDITION_REQUIRED(428, "Precondition Required", URNConstants.SERVER + "preconditionRequired"),
  TOO_MANY_REQUESTS(429, "Too Many Requests", URNConstants.SERVER + "tooManyRequests"),
  REQUEST_HEADER_FIELDS_TOO_LARGE(
      431, "Request Header Fields Too Large", URNConstants.SERVER + "requestHeaderFieldsTooLarge"),
  UNAVAILABLE_FOR_LEGAL_REASONS(
      451, "Unavailable For Legal Reasons", URNConstants.SERVER + "unavailableForLegalReasons"),

  // 5xx: Server Error
  INTERNAL_SERVER_ERROR(500, "Internal Server Error", URNConstants.SERVER + "internalServerError"),
  NOT_IMPLEMENTED(501, "Not Implemented", URNConstants.SERVER + "notImplemented"),
  BAD_GATEWAY(502, "Bad Gateway", URNConstants.SERVER + "badGateway"),
  SERVICE_UNAVAILABLE(503, "Service Unavailable", URNConstants.SERVER + "serviceUnavailable"),
  GATEWAY_TIMEOUT(504, "Gateway Timeout", URNConstants.SERVER + "gatewayTimeout"),
  HTTP_VERSION_NOT_SUPPORTED(
      505, "HTTP Version Not Supported", URNConstants.SERVER + "httpVersionNotSupported"),
  VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates", URNConstants.SERVER + "variantAlsoNegotiates"),
  INSUFFICIENT_STORAGE(507, "Insufficient Storage", URNConstants.SERVER + "insufficientStorage"),
  LOOP_DETECTED(508, "Loop Detected", URNConstants.SERVER + "loopDetected"),
  NOT_EXTENDED(510, "Not Extended", URNConstants.SERVER + "notExtended"),
  NETWORK_AUTHENTICATION_REQUIRED(
      511, "Network Authentication Required", URNConstants.SERVER + "networkAuthenticationRequired");

  private final int value;
  private final String description;
  private final String urn;

  HttpStatusCode(int value, String description, String urn) {
    this.value = value;
    this.description = description;
    this.urn = urn;
  }

  public static HttpStatusCode getByValue(int value) {
    for (HttpStatusCode status : values()) {
      if (status.value == value) {
        return status;
      }
    }
    throw new IllegalArgumentException("Invalid status code: " + value);
  }

  public int getValue() {
    return value;
  }

  public String getDescription() {
    return description;
  }

  public String getUrn() {
    return urn;
  }

  @Override
  public String toString() {
    return value + " " + description;
  }
}
