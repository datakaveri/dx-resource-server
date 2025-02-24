package iudx.resource.server.common.validation.types;

import iudx.resource.server.apiserver.exception.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.apiserver.util.Constants.BEARER_TOKEN_MIN_LENGTH;
import static iudx.resource.server.apiserver.util.Constants.BEARER_TOKEN_PATTERN;
import static iudx.resource.server.common.ResponseUrn.INVALID_TOKEN_URN;

public class BearerTokenTypeValidator implements Validator {

    public static final Logger LOGGER = LogManager.getLogger(BearerTokenTypeValidator.class);

    private final String value;
    private final boolean required;

    public BearerTokenTypeValidator(final String value, final boolean required) {
        this.value = value;
        this.required = required;
    }

    @Override
    public boolean isValid() {
        LOGGER.debug("value : " + value);
        if (required && (value == null || value.isBlank())) {
            LOGGER.error("validation error: null or blank value for required mandatory field");
            throw new DxRuntimeException(failureCode(), INVALID_TOKEN_URN, failureMessage());
        } else {
            if (value == null) {
                return true;
            }
            if (value.isBlank()) {
                LOGGER.error("Validation error: blank value passed");
                throw new DxRuntimeException(failureCode(), INVALID_TOKEN_URN, failureMessage());
            }
        }
        if (value.length() < BEARER_TOKEN_MIN_LENGTH) {
            LOGGER.error("Validation error : Value mismatch character limit.");
            throw new DxRuntimeException(failureCode(), INVALID_TOKEN_URN, failureMessage());
        }
        if (!isValidId(value)) {
            LOGGER.error("Validation error : Invalid ID");
            throw new DxRuntimeException(failureCode(), INVALID_TOKEN_URN, failureMessage());
        }
        return true;
    }

    private boolean isValidId(String value) {
        return BEARER_TOKEN_PATTERN.matcher(value).matches();
    }

    @Override
    public int failureCode() {
        return HttpStatusCode.UNAUTHORIZED.getValue();
    }

    @Override
    public String failureMessage() {
        return INVALID_TOKEN_URN.getMessage();
    }
}
