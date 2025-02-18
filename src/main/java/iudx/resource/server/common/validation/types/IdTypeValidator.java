package iudx.resource.server.common.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.*;

import iudx.resource.server.apiserver.exception.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class IdTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(IdTypeValidator.class);

  private final String value;
  private final boolean required;

  public IdTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }

  public boolean isvalidIudxId(final String value) {
    return VALIDATION_ID_PATTERN.matcher(value).matches();
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
      }
    }
    if (value.length() > VALIDATION_ID_MAX_LEN) {
      LOGGER.error("Validation error : Value exceed max character limit.");
      throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
    }
    if (!isvalidIudxId(value)) {
      LOGGER.error("Validation error : Invalid id.");
      throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage(value));
    }
    return true;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_ID_VALUE_URN.getMessage();
  }
}
