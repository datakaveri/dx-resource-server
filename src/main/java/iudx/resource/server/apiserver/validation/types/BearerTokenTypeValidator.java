package iudx.resource.server.apiserver.validation.types;

import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.apiserver.util.Constants.BEARER_TOKEN_MIN_LENGTH;
import static iudx.resource.server.apiserver.util.Constants.BEARER_TOKEN_PATTERN;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.ResponseUrn.INVALID_ID_VALUE_URN;

public class BearerTokenTypeValidator implements Validator{

  public static final Logger LOGGER = LogManager.getLogger(BearerTokenTypeValidator.class);

  private final String value;
  private final boolean required;

  public BearerTokenTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }


  @Override
  public boolean isValid() {
    LOGGER.debug("value : {}, Required : {}",  value, required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("validation error: null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error: blank value passed");
        throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
      }
    }
    if (value.length() <= BEARER_TOKEN_MIN_LENGTH) {
      LOGGER.error("Validation error : Value mismatch character limit.");
      throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
    }
    if (!isValidId(value)) {
      LOGGER.error("Validation error : Invalid ID");
      throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
    }
    return true;
  }

  private boolean isValidId(String value) {
    return BEARER_TOKEN_PATTERN.matcher(value).matches();
  }


  @Override
  public int failureCode() {
    return BAD_REQUEST.getValue();
  }


  @Override
  public String failureMessage() {
    return INVALID_ID_VALUE_URN.getMessage();
  }

}