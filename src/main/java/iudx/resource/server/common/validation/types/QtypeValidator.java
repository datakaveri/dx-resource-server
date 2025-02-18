package iudx.resource.server.common.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.*;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.exception.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class QtypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(QtypeValidator.class);

  private final String value;
  private final boolean required;

  public QtypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }

  private boolean isValidOperator(final String value, final boolean isNumericString) {
    LOGGER.info(value);
    LOGGER.info(value.equalsIgnoreCase("=="));
    LOGGER.info(isNumericString);
    LOGGER.info(VALIDATION_ALLOWED_OPERATORS.contains(value));
    return isNumericString
        ? VALIDATION_ALLOWED_OPERATORS.contains(value)
        : value.equalsIgnoreCase("==");
  }

  private boolean isValidId(final JsonObject json) {
    if (json.containsKey("id")) {
      String id = json.getString(JSON_VALUE);
      Matcher matcher = VALIDATION_ID_PATTERN.matcher(id);
      return matcher.matches();
    } else {
      return true;
    }
  }


  public boolean isValidAttribute(final String value) {
    LOGGER.info("value,{},{}", value, VALIDATION_Q_ATTR_PATTERN.matcher(value).matches());
    return VALIDATION_Q_ATTR_PATTERN.matcher(value).matches();
  }

  public boolean isValidAttributeValue(final String value) {
    LOGGER.info("value,{},{}", value, VALIDATION_Q_VALUE_PATTERN.matcher(value).matches());
    return VALIDATION_Q_VALUE_PATTERN.matcher(value).matches();
  }



  JsonObject getQueryTerms(final String queryTerms) {
    JsonObject json = new JsonObject();
    String jsonOperator = "";
    String jsonValue = "";
    String jsonAttribute = "";

    String[] attributes = queryTerms.split(";");
    LOGGER.info("Attributes : {} ", attributes);

    for (String attr : attributes) {

      String[] attributeQueryTerms =
          attr.split("((?=>)|(?<=>)|(?=<)|(?<=<)|(?<==)|(?=!)|(?<=!)|(?==)|(?===))");
      LOGGER.info(Arrays.stream(attributeQueryTerms).collect(Collectors.toList()));
      LOGGER.info(attributeQueryTerms.length);
      if (attributeQueryTerms.length == 3) {
        jsonOperator = attributeQueryTerms[1];
        jsonValue = attributeQueryTerms[2];
        json.put(JSON_OPERATOR, jsonOperator).put(JSON_VALUE, jsonValue);
      } else if (attributeQueryTerms.length == 4) {
        jsonOperator = attributeQueryTerms[1].concat(attributeQueryTerms[2]);
        jsonValue = attributeQueryTerms[3];
        json.put(JSON_OPERATOR, jsonOperator).put(JSON_VALUE, jsonValue);
      } else {
        throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
      }
      jsonAttribute = attributeQueryTerms[0];
      json.put(JSON_ATTRIBUTE, jsonAttribute);
      boolean isNumericString = isNumericString(jsonValue);
      if (!isValidOperator(jsonOperator, isNumericString)) {
        LOGGER.info("invalid operator : {} ", jsonOperator);
        throw new DxRuntimeException(
            failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(jsonOperator));
      }
      if (!isValidAttribute(jsonAttribute)) {
        LOGGER.info("invalid attribute : {} ", jsonAttribute);
        throw new DxRuntimeException(
            failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(jsonAttribute));
      }
      if (!isValidAttributeValue(jsonValue)) {
        LOGGER.info("invalid json value : {} ", jsonValue);
        throw new DxRuntimeException(
            failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(jsonValue));
      }
    }

    return json;
  }

  private boolean isNumericString(String jsonValue) {
    boolean isNumericString;
    LOGGER.debug("Parsing value : {} as a float", jsonValue);
    try {
      Float.parseFloat(jsonValue);
      isNumericString = true;
    } catch (NumberFormatException ne) {
      LOGGER.info("String based search");
      isNumericString = false;
    }
    return isNumericString;
  }

  @Override
  public boolean isValid() {
    LOGGER.info("value : " + value + " required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage());
      }
    }
    if (value.length() > 512) {
      LOGGER.error("Validation error : Exceeding max length(512 characters) criteria");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
    }
    JsonObject qjson;
    try {
      qjson = getQueryTerms(value);
    } catch (Exception ex) {
      LOGGER.error("Validation error : Operation not allowed.");
      throw ex;
    }

    return isValidId(qjson);
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_PARAM_VALUE_URN.getMessage();
  }
}
