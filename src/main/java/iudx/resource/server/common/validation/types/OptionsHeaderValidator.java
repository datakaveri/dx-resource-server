package iudx.resource.server.common.validation.types;

import static iudx.resource.server.common.ResponseUrn.INVALID_HEADER_VALUE_URN;

import iudx.resource.server.apiserver.exception.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OptionsHeaderValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(OptionsHeaderValidator.class);

  private String value;
  private boolean required;

  public OptionsHeaderValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_HEADER_VALUE_URN, failureMessage(value));
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value passed");
        throw new DxRuntimeException(
            failureCode(), INVALID_HEADER_VALUE_URN, failureMessage(value));
      }
    }
    if (!value.equals("streaming")) {
      LOGGER.error("Validation error : streaming is only allowed value for options parameter");
      throw new DxRuntimeException(failureCode(), INVALID_HEADER_VALUE_URN, failureMessage(value));
    }
    return true;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_HEADER_VALUE_URN.getMessage();
  }
}
