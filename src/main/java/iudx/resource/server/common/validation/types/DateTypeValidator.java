package iudx.resource.server.common.validation.types;

import static iudx.resource.server.common.ResponseUrn.*;

import iudx.resource.server.apiserver.exception.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DateTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(DateTypeValidator.class);

  private final String value;
  private final boolean required;

  public DateTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }

  private boolean isValidDate(final String value) {
    String dateString = value.trim().replaceAll("\\s", "+"); // since + is treated as space in uri
    // params
    try {
      ZonedDateTime.parse(dateString);
      return true;
    } catch (DateTimeParseException e) {
      throw new DxRuntimeException(failureCode(), INVALID_ATTR_VALUE_URN, failureMessage(value));
    }
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      throw new DxRuntimeException(failureCode(), INVALID_ATTR_VALUE_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        throw new DxRuntimeException(failureCode(), INVALID_ATTR_VALUE_URN, failureMessage(value));
      }
    }
    return isValidDate(value);
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_TEMPORAL_DATE_FORMAT_URN.getMessage();
  }
}
