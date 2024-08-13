package iudx.resource.server.common;

import java.util.stream.Stream;

public enum ResponseUrn {
  SUCCESS_URN("urn:dx:rs:success", "Success"),
  INVALID_PARAM_URN("urn:dx:rs:invalidParamameter", "Invalid parameter passed"),
  INVALID_GEO_REL_URN("urn:dx:rs:invalidGeoRel", "Invalid geo relation value"),
  INVALID_TEMPORAL_PARAM_URN("urn:dx:rs:invalidTemporalParam", "Invalid temporal parameter"),
  INVALID_TEMPORAL_REL_URN(
      "urn:dx:rs:invalidTemporalRelationParam", "Invalid temporal param value"),
  INVALID_TEMPORAL_DATE_FORMAT_URN("urn:dx:rs:invalidTemporalDateFormat", "Invalid date format"),
  INVALID_GEO_PARAM_URN("urn:dx:rs:invalidGeoParam", "Invalid geo param"),
  INVALID_GEO_VALUE_URN("urn:dx:rs:invalidGeoValue", "Invalid geo param value"),
  INVALID_ATTR_PARAM_URN("urn:dx:rs:invalidAttributeParam", "Invalid attribute param"),
  INVALID_ATTR_VALUE_URN("urn:dx:rs:invalidAttributeValue", "Invalid attribute value"),
  INVALID_OPERATION_URN("urn:dx:rs:invalidOperation", "Invalid operation"),
  UNAUTHORIZED_ENDPOINT_URN(
      "urn:dx:rs:unauthorizedEndpoint", "Access to endpoint is not available"),
  UNAUTHORIZED_RESOURCE_URN(
      "urn,dx:rs:unauthorizedResource", "Access to resource is not available"),
  EXPIRED_TOKEN_URN("urn:dx:rs:expiredAuthorizationToken", "Token has expired"),
  MISSING_TOKEN_URN("urn:dx:rs:missingAuthorizationToken", "Token needed and not present"),
  INVALID_TOKEN_URN("urn:dx:rs:invalidAuthorizationToken", "Token is invalid"),
  RESOURCE_NOT_FOUND_URN("urn:dx:rs:resourceNotFound", "Document of given id does not exist"),
  RESOURCE_ALREADY_EXIST_URN("urn:dx:rs:resourceAlreadyExist", "Resource already exist"),

  LIMIT_EXCEED_URN("urn:dx:rs:requestLimitExceeded", "Data usage limits exceeded"),

  PAYLOAD_TOO_LARGE_URN("urn:dx:rs:payloadTooLarge", "Response size exceeds limit"),

  // extra urn
  INVALID_ID_VALUE_URN("urn:dx:rs:invalidIdValue", "Invalid id"),
  INVALID_PAYLOAD_FORMAT_URN(
      "urn:dx:rs:invalidPayloadFormat", "Invalid json format in post request [schema mismatch]"),
  INVALID_PARAM_VALUE_URN("urn:dx:rs:invalidParameterValue", "Invalid parameter value passed"),
  BAD_REQUEST_URN("urn:dx:rs:badRequest", "bad request parameter"),
  INVALID_HEADER_VALUE_URN("urn:dx:rs:invalidHeaderValue", "Invalid header value"),
  DB_ERROR_URN("urn:dx:rs:DatabaseError", "Database error"),
  QUEUE_ERROR_URN("urn:dx:rs:QueueError", "Queue error"),

  BACKING_SERVICE_FORMAT_URN(
      "urn:dx:rs:backend", "format error from backing service [cat,auth etc.]"),
  SCHEMA_READ_ERROR_URN("urn:dx:rs:readError", "Fail to read file"),
  YET_NOT_IMPLEMENTED_URN("urn:dx:rs:general", "urn yet not implemented in backend verticle."),
  UNAUTHORIZED_ATTRS_URN(
      "urn:dx:rs:unauthorizedAttributes", "user unauthorized to access given attributes"),
  URL_EXPIRED_URN(
      "urn:dx:rs:UrlExpired", "The requested url/resource is expired and no longer available");

  private final String urn;
  private final String message;

  ResponseUrn(String urn, String message) {
    this.urn = urn;
    this.message = message;
  }

  public static ResponseUrn fromCode(final String urn) {
    return Stream.of(values())
        .filter(v -> v.urn.equalsIgnoreCase(urn))
        .findAny()
        .orElse(YET_NOT_IMPLEMENTED_URN); // if backend service dont respond with urn
  }

  public String getUrn() {
    return urn;
  }

  public String getMessage() {
    return message;
  }

  public String toString() {
    return "[" + urn + " : " + message + " ]";
  }
}
