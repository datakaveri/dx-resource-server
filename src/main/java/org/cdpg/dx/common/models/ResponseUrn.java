package org.cdpg.dx.common.models;

import java.util.stream.Stream;

public enum ResponseUrn {
  SUCCESS_URN("urn:dx:acl:success", "Success"),
  VERIFY_SUCCESS_URN("urn:apd:Allow", "Success"),
  INVALID_PARAM_URN("urn:dx:acl:invalidParameter", "Invalid parameter passed"),
  INVALID_PARAM_VALUE_URN("urn:dx:acl:invalidParameterValue", "Invalid parameter value passed"),

  INVALID_ATTR_VALUE_URN("urn:dx:acl:invalidAttributeValue", "Invalid attribute value"),

  INVALID_ATTR_PARAM_URN("urn:dx:acl:invalidAttributeParam", "Invalid attribute param"),

  INVALID_OPERATION_URN("urn:dx:acl:invalidOperation", "Invalid operation"),
  UNAUTHORIZED_ENDPOINT_URN(
      "urn:dx:acl:unauthorizedEndpoint", "Access to endpoint is not available"),
  UNAUTHORIZED_RESOURCE_URN(
      "urn,dx:rs:unauthorizedResource", "Access to resource is not available"),
  EXPIRED_TOKEN_URN("urn:dx:acl:expiredAuthorizationToken", "Token has expired"),
  MISSING_TOKEN_URN("urn:dx:acl:missingAuthorizationToken", "Token needed and not present"),
  INVALID_TOKEN_URN("urn:dx:acl:invalidAuthorizationToken", "Token is invalid"),
  RESOURCE_NOT_FOUND_URN("urn:dx:acl:resourceNotFound", "Document of given id does not exist"),
  LIMIT_EXCEED_URN(
      "urn:dx:acl:requestLimitExceeded", "Operation exceeds the default value of limit"),
  INVALID_ID_VALUE_URN("urn:dx:acl:invalidIdValue", "Invalid id"),
  INVALID_PAYLOAD_FORMAT_URN(
      "urn:dx:acl:invalidPayloadFormat", "Invalid json format in post request [schema mismatch]"),
  BAD_REQUEST_URN("urn:dx:acl:badRequest", "bad request parameter"),
  INVALID_HEADER_VALUE_URN("urn:dx:acl:invalidHeaderValue", "Invalid header value"),
  POLICY_ALREADY_EXIST_URN("urn:dx:acl:conflict", "Policy already exist"),
  INTERNAL_SERVER_ERROR("urn:dx:acl:internalServerError", "Internal Server Error"),

  VERIFY_FORBIDDEN_URN("urn:apd:Deny", "Policy does not exist"),
  FORBIDDEN_URN("urn:dx:acl:forbidden", "Resource is forbidden to access"),
  NOT_YET_IMPLEMENTED_URN("urn:dx:acl:general", "urn not yet implemented in backend verticle."),
  BACKING_SERVICE_FORMAT_URN(
      "urn:dx:acl:backend", "format error from backing service [cat,auth etc.]"),
  DB_ERROR_URN("urn:dx:acl:DatabaseError", "Database error"),
  ROLE_NOT_FOUND("urn:dx:acl:invalidRole", "Role does not exist");
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
        .orElse(NOT_YET_IMPLEMENTED_URN);
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
