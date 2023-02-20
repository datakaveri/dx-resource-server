package iudx.resource.server.apiserver.validation.types;

import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.INVALID_ID_VALUE_URN;

public final class ProviderIDTypeValidator implements Validator {

    private static final Logger LOGGER = LogManager.getLogger(ProviderIDTypeValidator.class);
    private static final Pattern regexIDPattern = ID_REGEX;
    private final String value;
    private final boolean required;
    private Integer minLength = VALIDATION_ID_MIN_LEN;
    private Integer maxLength = VALIDATION_ID_MAX_LEN;

    public ProviderIDTypeValidator(final String value, final boolean required) {
        this.value = value;
        this.required = required;
    }

    public boolean isvalidIUDXId(final String value) {
        return VALIDATION_PROVIDER_ID.matcher(value).matches();
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
        if (!isvalidIUDXId(value)) {
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
