package org.cdpg.dx.rs.search.util.validatorTypes;


import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import net.sf.saxon.trans.SymbolicName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.rs.search.model.ApplicableFilters;
import org.cdpg.dx.rs.search.util.RequestType;
import org.cdpg.dx.rs.search.util.UnifiedValidators;

import java.util.ArrayList;
import java.util.List;

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
                .recover(err -> Future.failedFuture(""));
    }

    /**
     * Validates request parameters from JSON body.
     */
    public Future<Boolean> validate(JsonObject requestJson, RequestType requestType) {
        MultiMap paramsMap = parameterExtractor.extractParams(requestJson);
        if(UnifiedValidators.validateJsonSchema(requestJson,requestType))
        return validate(paramsMap, requestType);
        else return Future.failedFuture("Invalid Schema.");
    }

    /**
     * Routes validation based on request type.
     */
    private Future<Boolean> validateParams(MultiMap multiMap, RequestType requestType) {
        return switch (requestType) {
            case ENTITY -> Future.succeededFuture(commonValidation(multiMap, false));
            case TEMPORAL -> Future.succeededFuture(commonValidation(multiMap, true));
            default -> Future.failedFuture("Invalid request type");
        };
    }

    /**
     * Unified validation logic for entity and temporal requests.
     */
    private boolean commonValidation(MultiMap paramsMap, boolean isTemporal) {
        String id = paramsMap.get(NGSILDQUERY_ID);
        String attrs = paramsMap.get(NGSILDQUERY_ATTRIBUTE);
        String q = paramsMap.get(NGSILDQUERY_Q);
        String limit = paramsMap.get(NGSILDQUERY_SIZE);
        String offSet = paramsMap.get(NGSILDQUERY_FROM);
        String publicKey = paramsMap.get(HEADER_PUBLIC_KEY);
        String options = paramsMap.get(IUDXQUERY_OPTIONS);

        JsonObject geoQ = new JsonObject()
                .put(NGSILDQUERY_GEOREL, paramsMap.get(NGSILDQUERY_GEOREL))
                .put(NGSILDQUERY_GEOMETRY, paramsMap.get(NGSILDQUERY_GEOMETRY))
                .put(NGSILDQUERY_GEOPROPERTY, paramsMap.get(NGSILDQUERY_GEOPROPERTY))
                .put(NGSILDQUERY_COORDINATES, paramsMap.get(NGSILDQUERY_COORDINATES));

        JsonObject temporalQ = new JsonObject()
                .put(NGSILDQUERY_TIMEREL, paramsMap.get(NGSILDQUERY_TIMEREL))
                .put(NGSILDQUERY_TIME, paramsMap.get(NGSILDQUERY_TIME))
                .put(NGSILDQUERY_ENDTIME, paramsMap.get(NGSILDQUERY_ENDTIME));

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