package iudx.resource.server.apiserver.util;


import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ResponseUtils {

  private static RespBuilder createResponseBuilder(String type, String title, String detail) {
    return new RespBuilder()
        .withType(type)
        .withTitle(title)
        .withDetail(detail);
  }

  public static String invalidSchemaResponseWithResult(String result) {
    return createResponseBuilder("TYPE_INVALID_SCHEMA", "TITLE_INVALID_SCHEMA", "TITLE_INVALID_SCHEMA")
        .withResult(result)
        .getResponse();
  }

  public static String invalidSchemaResponse(String detail) {
    return createResponseBuilder("TYPE_INVALID_SCHEMA", "TITLE_INVALID_SCHEMA", detail)
        .getResponse();
  }

  public static String linkValidationFailureResponse(String detail) {
    return createResponseBuilder("TYPE_LINK_VALIDATION_FAILED", "TITLE_LINK_VALIDATION_FAILED", detail)
        .getResponse();
  }

  public static String invalidUuidResponse(String detail) {
    return createResponseBuilder("TYPE_INVALID_UUID", "TITLE_INVALID_UUID", detail)
        .getResponse();
  }

  public static String itemNotFoundResponse(String detail) {
    return createResponseBuilder("TYPE_ITEM_NOT_FOUND", "TITLE_ITEM_NOT_FOUND", detail)
        .getResponse();
  }

 /* public static String itemNotFoundResponse(String id, String detail) {
    return createResponseBuilder(TYPE_ITEM_NOT_FOUND, TITLE_ITEM_NOT_FOUND, detail)
        .withResult(id, detail)
        .getResponse();
  }

  public static String itemNotFoundResponse(String id, String method, String detail) {
    return createResponseBuilder(TYPE_ITEM_NOT_FOUND, TITLE_ITEM_NOT_FOUND, detail)
        .withResult(id, method, FAILED, detail)
        .getResponse();
  }

  public static JsonObject itemNotFoundJsonResp(String detail) {
    return createResponseBuilder(TYPE_ITEM_NOT_FOUND, TITLE_ITEM_NOT_FOUND, detail)
        .getJsonResponse();
  }

  public static String invalidSyntaxResponse(String detail) {
    return createResponseBuilder(TYPE_INVALID_SYNTAX, TITLE_INVALID_SYNTAX, detail)
        .getResponse();
  }

  public static String internalErrorResp() {
    return createResponseBuilder(TYPE_INTERNAL_SERVER_ERROR, TITLE_INTERNAL_SERVER_ERROR,
        DETAIL_INTERNAL_SERVER_ERROR)
        .getResponse();
  }

  public static String itemAlreadyExistsResponse(String id, String detail) {
    return createResponseBuilder(TYPE_ALREADY_EXISTS, TITLE_ALREADY_EXISTS, detail)
        .withResult(id, INSERT, FAILED, detail)
        .getResponse();
  }

  public static String insertionFailedResponse(String id) {
    return createResponseBuilder(FAILED, "Insertion Failed", "Insertion Failed")
        .withResult(id, INSERT, FAILED)
        .getResponse();
  }

  public static JsonObject successResp(String id, String detail) {
    return createResponseBuilder(TYPE_SUCCESS, TITLE_SUCCESS, detail)
        .withResult(id)
        .getJsonResponse();
  }

  public static JsonObject successResponse(JsonArray result) {
    return createResponseBuilder(TYPE_SUCCESS, TITLE_SUCCESS, "")
        .withResult(result)
        .getJsonResponse();
  }

  public static JsonObject successResponse(String id) {
    return createResponseBuilder(TYPE_SUCCESS, TITLE_SUCCESS, "")
        .withResult(id, INSERT, TYPE_SUCCESS)
        .getJsonResponse();
  }

  public static JsonObject delSuccessResponse(String id) {
    return createResponseBuilder(TYPE_SUCCESS, TITLE_SUCCESS, "")
        .withResult(id)
        .getJsonResponse();
  }

  public static String failureResponse(String id) {
    return createResponseBuilder(TYPE_FAIL, "Insertion Failed", "Insertion Failed")
        .withResult(id, INSERT, FAILED)
        .getResponse();
  }

  public static String failureResp(String id) {
    return createResponseBuilder(FAILED, "Fail: Insertion of rating failed",
        "Fail: Insertion of rating failed")
        .withResult(id)
        .getResponse();
  }

  public static JsonObject successfulItemOperationResp(JsonObject doc, String detail) {
    return createResponseBuilder(TYPE_SUCCESS, TITLE_SUCCESS, detail)
        .withResult(doc)
        .getJsonResponse();
  }

  public static String operationNotAllowedResponse(String id, String method, String cause) {
    return createResponseBuilder(TYPE_OPERATION_NOT_ALLOWED, TITLE_OPERATION_NOT_ALLOWED, cause)
        .withResult(id, method, FAILED, cause)
        .getResponse();
  }

  public static String operationNotAllowedResponse(String id, String detail) {
    return createResponseBuilder(TYPE_OPERATION_NOT_ALLOWED, TITLE_OPERATION_NOT_ALLOWED, detail)
        .withResult(id, detail)
        .getResponse();
  }

  public static String invalidSearchError() {
    return createResponseBuilder(TYPE_INVALID_SEARCH_ERROR, TITLE_INVALID_SEARCH_ERROR,
        TITLE_INVALID_SEARCH_ERROR)
        .getResponse();
  }

  public static String invalidParameterResp() {
    return createResponseBuilder(FAILED, ERROR_INVALID_PARAMETER, ERROR_INVALID_PARAMETER)
        .getResponse();
  }*/
}
