package org.cdpg.dx.rs.search.util;


import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.rs.search.util.validatorTypes.GeoValidator;
import org.cdpg.dx.rs.search.util.validatorTypes.TemporalValidator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cdpg.dx.util.Constants.*;

public final class UnifiedValidators {

    private static final Logger LOGGER = LogManager.getLogger(UnifiedValidators.class);
    private static final GeoValidator geoValidator = new GeoValidator();
    private static final TemporalValidator temporalValidator = new TemporalValidator();
    private static final Pattern ATTRS_VALUE_REGEX = Pattern.compile("^[a-zA-Z0-9_]+");
    public static final Set<String> VALID_OPERATORS = Set.of("==", "!=", ">", "<", ">=", "<=");
    public static final int MAX_QUERY_TERMS = 5;
    // Additional constants
    private static final Set<String> NUMERIC_OPERATORS = Set.of(">", "<", ">=", "<=");
    private static final Pattern QUERY_TERM_PATTERN =
            Pattern.compile("^([a-zA-Z0-9_]+)(==|!=|>=|<=|>|<)(.+)$");
    private UnifiedValidators() {
        // Private constructor to prevent instantiation
    }

    public static boolean validateAttributes(String attribute) {
        LOGGER.debug("Validating attribute {}",attribute);
        if (attribute == null) {
            return true;
        }
        if (attribute.isBlank()) {
            return false;
        }

        String[] attrs = attribute.split(",");
        if (attrs.length > VALIDATION_MAX_ATTRS) {
            LOGGER.error("Attributes cannot be more than max limit {}",VALIDATION_MAX_ATTRS);
            return false;
        }

        for (String attr : attrs) {
            if (attr.length() > VALIDATIONS_MAX_ATTR_LENGTH || !ATTRS_VALUE_REGEX.matcher(attr).matches()) {
                LOGGER.error("Attribute cannot be more than max limit or attribute in invalid format" );
                return false;
            }
        }
        return true;
    }

    public static boolean validateId(String value, boolean required) {
        LOGGER.debug("Validating Id");

        if (value == null) {
            return !required;
        }
        if (value.isBlank()) {
            return !required;
        }

        if (value.length() > VALIDATION_ID_MAX_LEN) {
            LOGGER.error("ID exceeds maximum length: {}", VALIDATION_ID_MAX_LEN);
            return false;
        }

        if (!VALIDATION_ID_PATTERN.matcher(value).matches()) {
            LOGGER.error("Invalid ID format");
            return false;
        }

        return true;
    }

    public static boolean validatePaginationLimit(String value) {
        LOGGER.debug("Validating pagination limit: {}", value);

        if (value == null) {
            return true;
        }
        if (value.isBlank()) {
            return false;
        }

        try {
            int limit = Integer.parseInt(value);
            return limit >= 0 && limit <= VALIDATION_PAGINATION_LIMIT_MAX;
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid limit format: {}", value);
            return false;
        }
    }

    public static boolean validatePaginationOffset(String offset) {
        LOGGER.debug("Validating pagination offset: {}", offset);

        if (offset == null) {
            return true;
        }
        if (offset.isBlank()) {
            return false;
        }

        try {
            int offsetValue = Integer.parseInt(offset);
            return offsetValue >= 0 && offsetValue <= VALIDATION_PAGINATION_OFFSET_MAX;
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid offset format: {}", offset);
            return false;
        }
    }

    public static boolean validateQType(String value) {
        LOGGER.debug("Validating Q type value {}",value);
        if (value == null || value.isBlank()) {
            return false;
        }

        String[] terms = value.split(";");
        if (terms.length == 0 || terms.length > MAX_QUERY_TERMS) {
            return false;
        }

        for (String term : terms) {
            // RECHECK THIS matcher length can be 3 or 4
            Matcher matcher = QUERY_TERM_PATTERN.matcher(term.trim());
            if (!matcher.matches()) {
                return false;
            }
            String attribute = matcher.group(1);
            String operator = matcher.group(2);
            String queryValue = matcher.group(3).trim();

            if (!isValidQueryTerm(attribute, operator, queryValue)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidQueryTerm(String attribute, String operator, String value) {
        LOGGER.debug("Validating Valid query terms.");
        // Validate attribute format
        if (!VALIDATION_Q_ATTR_PATTERN.matcher(attribute).matches()) {
            return false;
        }

        // Validate operator
        if (!VALID_OPERATORS.contains(operator)) {
            return false;
        }

        // Validate value format
        if (!VALIDATION_Q_VALUE_PATTERN.matcher(value).matches()) {
            return false;
        }

        // Validate operator-value compatibility
        boolean isNumericValue = isNumeric(value);
        if (!isNumericValue && NUMERIC_OPERATORS.contains(operator)) {
            return false;  // Can't use numeric operators with non-numeric values
        }

        return true;
    }


    public static boolean validateGeoQ(JsonObject geoQ, boolean required) {
        LOGGER.debug("Validating geoQ {}",geoQ.isEmpty());
        if (geoQ == null || geoQ.isEmpty()) {
            return !required;
        }
        return geoValidator.validateGeo(geoQ);
    }

    public static boolean validateTempQ(JsonObject tempQ, boolean required) {
        LOGGER.debug("Validating tempQ");
        if (tempQ == null || tempQ.isEmpty()) {
            return !required;
        }
        return temporalValidator.validateTemporalParams(tempQ);
    }

    public static boolean validateHeader(String publicKey) {
        LOGGER.debug("Validating header.");
        if (publicKey == null) {
            return true;
        }
        if (publicKey.length() != 44) {
            LOGGER.error("Invalid public key length");
            return false;
        }
        return Pattern.matches(ENCODED_PUBLIC_KEY_REGEX, publicKey);
    }

    public static boolean validateOptions(String value) {
       LOGGER.debug("Validating optiosn {}",value);
        if (value == null) {
            return true;
        }
        return value.equals("count");
    }

    public static boolean validateJsonSchema(JsonObject requestJson,RequestType requestType) {
        LOGGER.debug("Trying to validate JSON schema.");
        SchemaRouter schemaRouter = SchemaRouter.create(Vertx.vertx(), new SchemaRouterOptions());
        SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);
        String jsonSchema = null;
        try {
           jsonSchema = loadJson(requestType.getFilename());
            Schema schema=schemaParser.parse(new JsonObject(jsonSchema));
            schema.validateSync(requestJson);
        } catch (ValidationException | NoSyncValidationException e) {
            return false;
        }
        return true;
    }

    private static String loadJson(String filename) {
        String jsonStr = null;
        Map<String,String> jsonSchemaMap=new HashMap<>();
        try (InputStream inputStream = UnifiedValidators.class.getClassLoader().getResourceAsStream(filename)) {
            jsonStr = CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
            jsonSchemaMap.put(filename, jsonStr);
        } catch (IOException e) {
            LOGGER.error("Failed to load json file {}",e.getLocalizedMessage());
            return e.getLocalizedMessage();
        }
        return jsonStr;
    }
    private static boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}