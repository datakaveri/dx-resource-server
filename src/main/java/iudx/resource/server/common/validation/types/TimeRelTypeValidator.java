package iudx.resource.server.common.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.*;

import iudx.resource.server.apiserver.exception.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TimeRelTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(GeoRelTypeValidator.class);

  private final String value;
  private final boolean required;
  private final boolean isAsyncQuery;

  public TimeRelTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
    this.isAsyncQuery = false;
  }

  public TimeRelTypeValidator(
      final String value, final boolean required, final boolean isAsyncQuery) {
    this.value = value;
    this.required = required;
    this.isAsyncQuery = isAsyncQuery;
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_TEMPORAL_REL_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        throw new DxRuntimeException(
            failureCode(), INVALID_TEMPORAL_REL_URN, failureMessage(value));
      }
    }
    if (isAsyncQuery && !VALIDATION_ALLOWED_TEMPORAL_REL_ASYNC.contains(value)) {
      LOGGER.error(
          "Validation error : Value " + value + " " + "is not allowed for async temporal queries");
      throw new DxRuntimeException(failureCode(), INVALID_TEMPORAL_REL_URN, failureMessage(value));
    }
    if (!isAsyncQuery && !VALIDATION_ALLOWED_TEMPORAL_REL.contains(value)) {
      LOGGER.error(
          "Validation error : Value " + value + " " + "is not allowed for temporal queries");
      throw new DxRuntimeException(failureCode(), INVALID_TEMPORAL_REL_URN, failureMessage(value));
    }
    return true;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_TEMPORAL_REL_URN.getMessage();
  }
}
