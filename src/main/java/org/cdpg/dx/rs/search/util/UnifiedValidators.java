package org.cdpg.dx.rs.search.util;


import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.SearchValidationError;
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
            throw new SearchValidationError("Attribute value cannot be null");
        }

        String[] attrs = attribute.split(",");
        if (attrs.length > VALIDATION_MAX_ATTRS) {
            LOGGER.error("Attributes cannot be more than max limit {}",VALIDATION_MAX_ATTRS);
            throw new SearchValidationError("Attributes cannot be more than max limit");
        }

        for (String attr : attrs) {
            if (attr.length() > VALIDATIONS_MAX_ATTR_LENGTH || !ATTRS_VALUE_REGEX.matcher(attr).matches()) {
                LOGGER.error("Attribute cannot be more than max limit or attribute in invalid format" );
                throw new SearchValidationError("Attribute cannot be more than max limit or attribute in invalid format");
            }
        }
        return true;
    }

    public static boolean validateId(String value, boolean required) {
        LOGGER.debug("Validating Id");

        if (value == null) {
            throw new SearchValidationError("Id cannot be null");
        }
        if (value.isBlank()) {
            throw new SearchValidationError("Id cannot be blank");
        }

        if (value.length() > VALIDATION_ID_MAX_LEN) {
            LOGGER.error("ID exceeds maximum length: {}", VALIDATION_ID_MAX_LEN);
            throw new SearchValidationError("Id must be in required format");
        }

        if (!VALIDATION_ID_PATTERN.matcher(value).matches()) {
            throw new SearchValidationError("Id must be in required format");
        }

        return true;
    }

    public static boolean validatePaginationLimit(String value) {
        LOGGER.debug("Validating pagination limit: {}", value);

        if (value == null) {
            return true;
        }
        if (value.isBlank()) {
            throw new SearchValidationError("limit cannot be blank");
        }

        try {
            int limit = Integer.parseInt(value);
            return limit >= 0 && limit <= VALIDATION_PAGINATION_LIMIT_MAX;
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid limit format: {}", value);
            throw new SearchValidationError("Invalid limit format");
        }
    }

    public static boolean validatePaginationOffset(String offset) {
        LOGGER.debug("Validating pagination offset: {}", offset);

        if (offset == null) {
            return true;
        }
        if (offset.isBlank()) {
            throw new SearchValidationError("offset cannot be blank");
        }

        try {
            int offsetValue = Integer.parseInt(offset);
            return offsetValue >= 0 && offsetValue <= VALIDATION_PAGINATION_OFFSET_MAX;
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid offset format: {}", offset);
            throw new SearchValidationError("Invalid offset format");
        }
    }

    public static boolean validateQType(String value) {
        LOGGER.debug("Validating Q type value {}", value);
        if (value == null || value.isBlank()) {
            LOGGER.error("Q-type cannot be null or blank");
            throw new SearchValidationError("Q-type cannot be null or blank");
        }

        String[] terms = value.split(";");
        if (terms.length == 0 || terms.length > MAX_QUERY_TERMS) {
            LOGGER.error("Q-type has invalid number of query terms");
            throw new SearchValidationError("Q-type has invalid number of query terms");
        }

        for (String term : terms) {
            Matcher matcher = QUERY_TERM_PATTERN.matcher(term.trim());
            if (!matcher.matches()) {
                LOGGER.error("Query term does not match required pattern");
                throw new SearchValidationError("Query term does not match required pattern");
            }
            String attribute = matcher.group(1);
            String operator = matcher.group(2);
            String queryValue = matcher.group(3).trim();

            if (!isValidQueryTerm(attribute, operator, queryValue)) {
                LOGGER.error("Invalid query term format for term: {}", term);
                throw new SearchValidationError("Invalid query term format");
            }
        }
        return true;
    }

    private static boolean isValidQueryTerm(String attribute, String operator, String value) {
        LOGGER.debug("Validating Valid query terms.");
        if (!VALIDATION_Q_ATTR_PATTERN.matcher(attribute).matches()) {
            LOGGER.error("Invalid query attribute format: {}", attribute);
            throw new SearchValidationError("Invalid query attribute format");
        }
        if (!VALID_OPERATORS.contains(operator)) {
            LOGGER.error("Invalid operator in query: {}", operator);
            throw new SearchValidationError("Invalid operator in query");
        }
        if (!VALIDATION_Q_VALUE_PATTERN.matcher(value).matches()) {
            LOGGER.error("Invalid query value format: {}", value);
            throw new SearchValidationError("Invalid query value format");
        }
        boolean isNumericValue = isNumeric(value);
        if (!isNumericValue && NUMERIC_OPERATORS.contains(operator)) {
            LOGGER.error("Non-numeric value '{}' used with numeric operator '{}'", value, operator);
            throw new SearchValidationError("Non-numeric value used with numeric operator");
        }
        return true;
    }

    public static boolean validateGeoQ(JsonObject geoQ, boolean required) {
        LOGGER.debug("Validating geoQ: isEmpty = {}", geoQ == null || geoQ.isEmpty());
        if (geoQ == null || geoQ.isEmpty()) {
            if (required) {
                LOGGER.error("Geo query required but not provided");
                throw new SearchValidationError("Geo query required but not provided");
            }
            return true;
        }
        if (!geoValidator.validateGeo(geoQ)) {
            LOGGER.error("Geo query validation failed");
            throw new SearchValidationError("Geo query validation failed");
        }
        return true;
    }

    public static boolean validateTempQ(JsonObject tempQ, boolean required) {
        LOGGER.debug("Validating tempQ");
        if (tempQ == null || tempQ.isEmpty()) {
            if (required) {
                LOGGER.error("Temporal query required but not provided");
                throw new SearchValidationError("Temporal query required but not provided");
            }
            return true;
        }
        if (!temporalValidator.validateTemporalParams(tempQ)) {
            LOGGER.error("Temporal query validation failed");
            throw new SearchValidationError("Temporal query validation failed");
        }
        return true;
    }

    public static boolean validateHeader(String publicKey) {
        LOGGER.debug("Validating header.");
        if (publicKey == null) {
            return true;
        }
        if (publicKey.length() != 44) {
            LOGGER.error("Invalid public key length: {}", publicKey.length());
            throw new SearchValidationError("Public key must be 44 characters");
        }
        if (!Pattern.matches(ENCODED_PUBLIC_KEY_REGEX, publicKey)) {
            LOGGER.error("Public key format is invalid: {}", publicKey);
            throw new SearchValidationError("Public key format is invalid");
        }
        return true;
    }

    public static boolean validateOptions(String value) {
        LOGGER.debug("Validating options: {}", value);
        if (value == null) {
            return true;
        }
        if (!value.equals("count")) {
            LOGGER.error("Invalid value for options: {}", value);
            throw new SearchValidationError("Invalid value for options parameter");
        }
        return true;
    }

    public static boolean validateJsonSchema(JsonObject requestJson, RequestType requestType) {
        LOGGER.debug("Trying to validate JSON schema.");
        SchemaRouter schemaRouter = SchemaRouter.create(Vertx.vertx(), new SchemaRouterOptions());
        SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);
        String jsonSchema;
        try {
            jsonSchema = loadJson(requestType.getFilename());
            Schema schema = schemaParser.parse(new JsonObject(jsonSchema));
            schema.validateSync(requestJson);
        } catch (ValidationException | NoSyncValidationException e) {
            LOGGER.error("JSON Schema validation failed: {}", e.getMessage());
            throw new SearchValidationError("JSON Schema validation failed: " + e.getMessage());
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