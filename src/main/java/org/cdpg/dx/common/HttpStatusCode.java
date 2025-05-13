package org.cdpg.dx.common;

public enum HttpStatusCode {

  // 1xx: Informational
  CONTINUE(100, "Continue", "urn:dx:acl:continue"),
  SWITCHING_PROTOCOLS(101, "Switching Protocols", "urn:dx:acl:switchingProtocols"),
  PROCESSING(102, "Processing", "urn:dx:acl:processing"),
  EARLY_HINTS(103, "Early Hints", "urn:dx:acl:earlyHints"),

  // 2XX: codes
  NO_CONTENT(204, "No Content", "urn:dx:acl:noContent"),
  SUCCESS(200, "Success", "urn:dx:acl:success"),

  // 4xx: Client Error
  BAD_REQUEST(400, "Bad Request", "urn:dx:acl:badRequest"),
  UNAUTHORIZED(401, "Not Authorized", "urn:dx:acl:notAuthorized"),
  PAYMENT_REQUIRED(402, "Payment Required", "urn:dx:acl:paymentRequired"),
  FORBIDDEN(403, "Forbidden", "urn:dx:acl:forbidden"),
  VERIFY_FORBIDDEN(403, "Policy does not exist", "urn:apd:Deny"),
  NOT_FOUND(404, "Not Found", "urn:dx:acl:notFound"),
  METHOD_NOT_ALLOWED(405, "Method Not Allowed", "urn:dx:acl:methodNotAllowed"),
  NOT_ACCEPTABLE(406, "Not Acceptable", "urn:dx:acl:notAcceptable"),
  PROXY_AUTHENTICATION_REQUIRED(
      407, "Proxy Authentication Required", "urn:dx:acl:proxyAuthenticationRequired"),
  REQUEST_TIMEOUT(408, "Request Timeout", "urn:dx:acl:requestTimeout"),
  CONFLICT(409, "Conflict", "urn:dx:acl:conflict"),
  GONE(410, "Gone", "urn:dx:acl:gone"),
  LENGTH_REQUIRED(411, "Length Required", "urn:dx:acl:lengthRequired"),
  PRECONDITION_FAILED(412, "Precondition Failed", "urn:dx:acl:preconditionFailed"),
  REQUEST_TOO_LONG(413, "Payload Too Large", "urn:dx:acl:payloadTooLarge"),
  REQUEST_URI_TOO_LONG(414, "URI Too Long", "urn:dx:acl:uriTooLong"),
  UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type", "urn:dx:acl:unsupportedMediaType"),
  REQUESTED_RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable", "urn:dx:acl:rangeNotSatisfiable"),
  EXPECTATION_FAILED(417, "Expectation Failed", "urn:dx:acl:expectation Failed"),
  MISDIRECTED_REQUEST(421, "Misdirected Request", "urn:dx:acl:misdirected Request"),
  UNPROCESSABLE_ENTITY(422, "Unprocessable Entity", "urn:dx:acl:unprocessableEntity"),
  LOCKED(423, "Locked", "urn:dx:acl:locked"),
  FAILED_DEPENDENCY(424, "Failed Dependency", "urn:dx:acl:failedDependency"),
  TOO_EARLY(425, "Too Early", "urn:dx:acl:tooEarly"),
  UPGRADE_REQUIRED(426, "Upgrade Required", "urn:dx:acl:upgradeRequired"),
  PRECONDITION_REQUIRED(428, "Precondition Required", "urn:dx:acl:preconditionRequired"),
  TOO_MANY_REQUESTS(429, "Too Many Requests", "urn:dx:acl:tooManyRequests"),
  REQUEST_HEADER_FIELDS_TOO_LARGE(
      431, "Request Header Fields Too Large", "urn:dx:acl:requestHeaderFieldsTooLarge"),
  UNAVAILABLE_FOR_LEGAL_REASONS(
      451, "Unavailable For Legal Reasons", "urn:dx:acl:unavailableForLegalReasons"),

  // 5xx: Server Error
  INTERNAL_SERVER_ERROR(500, "Internal Server Error", "urn:dx:acl:internalServerError"),
  NOT_IMPLEMENTED(501, "Not Implemented", "urn:dx:acl:notImplemented"),
  BAD_GATEWAY(502, "Bad Gateway", "urn:dx:acl:badGateway"),
  SERVICE_UNAVAILABLE(503, "Service Unavailable", "urn:dx:acl:serviceUnavailable"),
  GATEWAY_TIMEOUT(504, "Gateway Timeout", "urn:dx:acl:gatewayTimeout"),
  HTTP_VERSION_NOT_SUPPORTED(
      505, "HTTP Version Not Supported", "urn:dx:acl:httpVersionNotSupported"),
  VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates", "urn:dx:acl:variantAlsoNegotiates"),
  INSUFFICIENT_STORAGE(507, "Insufficient Storage", "urn:dx:acl:insufficientStorage"),
  LOOP_DETECTED(508, "Loop Detected", "urn:dx:acl:loopDetected"),
  NOT_EXTENDED(510, "Not Extended", "urn:dx:acl:notExtended"),
  NETWORK_AUTHENTICATION_REQUIRED(
      511, "Network Authentication Required", "urn:dx:acl:networkAuthenticationRequired");

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
