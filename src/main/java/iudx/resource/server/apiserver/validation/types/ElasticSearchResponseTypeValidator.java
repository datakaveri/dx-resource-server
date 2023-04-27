package iudx.resource.server.apiserver.validation.types;

import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.INVALID_HEADER_VALUE_URN;
import static iudx.resource.server.common.ResponseUrn.INVALID_ID_VALUE_URN;

public class ElasticSearchResponseTypeValidator implements Validator {

    private static final Logger LOGGER = LogManager.getLogger(ElasticSearchResponseTypeValidator.class);

    private final String value;
    private final boolean required;

    public ElasticSearchResponseTypeValidator(final String value, final boolean required) {
        this.value = value;
        this.required = required;
    }


    @Override
    public boolean isValid() {
        LOGGER.info("inside isValid ");
        LOGGER.debug("value : " + value + "required : " + required);

        if (required && (value == null || value.isBlank())) {
            LOGGER.error("Validation error : null or blank value for required mandatory field");
            throw new DxRuntimeException(failureCode(), INVALID_ID_VALUE_URN, failureMessage());
        } else if (!required && value == null) {
            return true;
        } else if (!required && value.isEmpty()) {
            LOGGER.error("Validation error : The value of the header format is empty ");
            throw new DxRuntimeException(failureCode(), INVALID_HEADER_VALUE_URN, failureMessage());
        } else if (value.equals(HEADER_CSV) || value.equals(HEADER_PARQUET)) {
            return true;
        }
        LOGGER.error("Validation error : Invalid header format type");
        throw new DxRuntimeException(failureCode(), INVALID_HEADER_VALUE_URN, failureMessage(value));
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