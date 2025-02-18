package iudx.resource.server.common.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.*;

import iudx.resource.server.apiserver.exception.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PaginationLimitTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(PaginationLimitTypeValidator.class);

  private final String value;
  private final boolean required;

  public PaginationLimitTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value passed");
        throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage());
      }
    }
    if (!isValidValue(value)) {
      LOGGER.error("Validation error : invalid pagination limit Value [ " + value + " ]");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage());
    }
    return true;
  }

  private boolean isValidValue(String value) {
    try {
      int size = Integer.parseInt(value);
      if (size > VALIDATION_PAGINATION_LIMIT_MAX || size < 0) {
        LOGGER.error(
            "Validation error : invalid pagination limit Value > 10000 or negative value passed [ "
                + value
                + " ]");
        throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
      }
      return true;
    } catch (Exception ex) {
      LOGGER.error(
          "Validation error : invalid pagination limit Value [ "
              + value
              + " ] only integer expected");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
    }
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
