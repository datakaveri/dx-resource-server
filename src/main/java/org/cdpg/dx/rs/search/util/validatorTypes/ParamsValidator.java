package org.cdpg.dx.rs.search.util.validatorTypes;


import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.rs.search.model.ApplicableFilters;
import org.cdpg.dx.rs.search.util.RequestType;
import org.cdpg.dx.rs.search.util.UnifiedValidators;

import static org.cdpg.dx.util.Constants.*;

/**
 * Refactored validator class with improved structure and reduced redundancy.
 */
public class ParamsValidator {

    private static final Logger LOGGER = LogManager.getLogger(ParamsValidator.class);

    private final AllowedParamsValidator allowedParamsValidator;
    private final QueryFiltersValidator queryFiltersValidator;
    private final ParameterExtractor parameterExtractor;

    public ParamsValidator(ApplicableFilters applicableFilters) {
        this.allowedParamsValidator = new AllowedParamsValidator();
        this.queryFiltersValidator = new QueryFiltersValidator(applicableFilters);
        this.parameterExtractor = new ParameterExtractor();
    }

    /**
     * Validates request parameters from MultiMap.
     */
    public Future<Boolean> validate(MultiMap paramsMap, RequestType requestType) {
        if (!allowedParamsValidator.areParamsAllowed(paramsMap)) {
            return Future.failedFuture(MSG_BAD_QUERY);
        }
        return validateParams(paramsMap, requestType)
                .compose(validParams -> queryFiltersValidator.validate(paramsMap))
                .map(result -> true)
                .recover(err -> Future.failedFuture(MSG_BAD_QUERY));
    }

    /**
     * Validates request parameters from JSON body.
     */
    public Future<Boolean> validate(JsonObject requestJson, RequestType requestType) {
        MultiMap paramsMap = parameterExtractor.extractParams(requestJson);
        return validate(paramsMap, requestType);
    }

    /**
     * Routes validation based on request type.
     */
    private Future<Boolean> validateParams(MultiMap multiMap, RequestType requestType) {
        return switch (requestType) {
            case ENTITY -> Future.succeededFuture(commonValidation(multiMap, false));
            case TEMPORAL -> Future.succeededFuture(commonValidation(multiMap, true));
            case POST_ENTITIES, POST_TEMPORAL ->
                    Future.failedFuture("Request type not implemented");
            default -> Future.failedFuture("Invalid request type");
        };
    }

    /**
     * Unified validation logic for entity and temporal requests.
     */
    private boolean commonValidation(MultiMap multiMap, boolean isTemporal) {
        String id = multiMap.get(NGSILDQUERY_ID);
        String attrs = multiMap.get(NGSILDQUERY_ATTRIBUTE);
        String q = multiMap.get(NGSILDQUERY_Q);
        String limit = multiMap.get(NGSILDQUERY_SIZE);
        String offSet = multiMap.get(NGSILDQUERY_FROM);
        String publicKey = multiMap.get(HEADER_PUBLIC_KEY);
        String options = multiMap.get(IUDXQUERY_OPTIONS);

        JsonObject geoQ = new JsonObject()
                .put(NGSILDQUERY_GEOREL, multiMap.get(NGSILDQUERY_GEOREL))
                .put(NGSILDQUERY_GEOMETRY, multiMap.get(NGSILDQUERY_GEOMETRY))
                .put(NGSILDQUERY_GEOPROPERTY, multiMap.get(NGSILDQUERY_GEOPROPERTY))
                .put(NGSILDQUERY_COORDINATES, multiMap.get(NGSILDQUERY_COORDINATES));

        JsonObject temporalQ = new JsonObject()
                .put(NGSILDQUERY_TIMEREL, multiMap.get(NGSILDQUERY_TIMEREL))
                .put(NGSILDQUERY_TIME, multiMap.get(NGSILDQUERY_TIME))
                .put(NGSILDQUERY_ENDTIME, multiMap.get(NGSILDQUERY_ENDTIME));

        return UnifiedValidators.validateId(id, true)
                && UnifiedValidators.validateAttributes(attrs)
                && UnifiedValidators.validateGeoQ(geoQ, false)
                && UnifiedValidators.validateQType(q)
                && UnifiedValidators.validatePaginationLimit(limit)
                && UnifiedValidators.validatePaginationOffset(offSet)
                && UnifiedValidators.validateTempQ(temporalQ, isTemporal)
                && UnifiedValidators.validateHeader(publicKey)
                && UnifiedValidators.validateOptions(options);
    }
}