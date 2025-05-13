package org.cdpg.dx.rs.search.util.validatorTypes;


import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
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
        LOGGER.debug("Inside multiMap validation ");
        Promise<Boolean> promise = Promise.promise();
        if (!allowedParamsValidator.areParamsAllowed(paramsMap)) {
            LOGGER.error("Invalid Parameter in request.");
            promise.fail(MSG_BAD_QUERY);
            return promise.future();
        }
        return validateParams(paramsMap, requestType)
                .compose(validParams -> queryFiltersValidator.validate(paramsMap))
                .onSuccess(successHandler -> {
                    promise.complete(true);
                }).onFailure(failureHandler -> {
                    promise.fail("Invalid multimap parameter");
                });

    }

    /**
     * Validates request parameters from JSON body.
     */
    public Future<Boolean> validate(JsonObject requestJson, RequestType requestType) {
        MultiMap paramsMap = parameterExtractor.extractParams(requestJson);
        if (UnifiedValidators.validateJsonSchema(requestJson, requestType)) {
            LOGGER.debug("Request body json valid.");
            return validate(paramsMap, requestType);
        } else return Future.failedFuture("Invalid Json Schema.");
    }

    /**
     * Routes validation based on request type.
     */
    private Future<Boolean> validateParams(MultiMap multiMap, RequestType requestType) {
        LOGGER.debug("Inside Validate Params for request type {}", requestType);
        switch (requestType) {
            case ENTITY, POST_ENTITIES -> {
                if (commonValidation(multiMap, false)) {
                    return Future.succeededFuture(commonValidation(multiMap, false));
                } else {
                    return Future.failedFuture("Parameters not valid for spatial.");
                }
            }
            case TEMPORAL, POST_TEMPORAL -> {
                if (commonValidation(multiMap, false)) {
                    return Future.succeededFuture(commonValidation(multiMap, true));
                } else {
                    return Future.failedFuture("Parameters not valid for temporal.");
                }
            }
            default -> Future.failedFuture("Invalid request type");
        }
        return Future.succeededFuture();
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

        JsonObject geoQ = null;
        if (isSpatialQuery(paramsMap)) {
            geoQ = new JsonObject()
                    .put(NGSILDQUERY_GEOREL, paramsMap.get(NGSILDQUERY_GEOREL))
                    .put(NGSILDQUERY_GEOMETRY, paramsMap.get(NGSILDQUERY_GEOMETRY))
                    .put(NGSILDQUERY_GEOPROPERTY, paramsMap.get(NGSILDQUERY_GEOPROPERTY))
                    .put(NGSILDQUERY_COORDINATES, paramsMap.get(NGSILDQUERY_COORDINATES));
        }

        JsonObject temporalQ = null;
        if (isTemporalQuery(paramsMap)) {
            temporalQ = new JsonObject()
                    .put(NGSILDQUERY_TIMEREL, paramsMap.get(NGSILDQUERY_TIMEREL))
                    .put(NGSILDQUERY_TIME, paramsMap.get(NGSILDQUERY_TIME))
                    .put(NGSILDQUERY_ENDTIME, paramsMap.get(NGSILDQUERY_ENDTIME));
        }

        boolean isValid = true;

        if (id != null) {
            isValid &= UnifiedValidators.validateId(id, true);
            LOGGER.debug("Is id valid {}", isValid);
        }
        if (attrs != null) {
            isValid &= UnifiedValidators.validateAttributes(attrs);
            LOGGER.debug("Is attrs valid {}", isValid);

        }
        if (geoQ != null) {
            isValid &= UnifiedValidators.validateGeoQ(geoQ, false);
            LOGGER.debug("Is geoQ valid {}", isValid);
        }
        if (q != null) {
            isValid &= UnifiedValidators.validateQType(q);
            LOGGER.debug("Is q valid {}", isValid);

        }
        if (limit != null) {
            isValid &= UnifiedValidators.validatePaginationLimit(limit);
            LOGGER.debug("Is limit valid {}", isValid);

        }
        if (offSet != null) {
            isValid &= UnifiedValidators.validatePaginationOffset(offSet);
            LOGGER.debug("Is offSet valid {}", isValid);
        }
        if (temporalQ != null) {
            isValid &= UnifiedValidators.validateTempQ(temporalQ, isTemporal);
            LOGGER.debug("Is temporalQ valid {}", isValid);

        }
        if (publicKey != null) {
            isValid &= UnifiedValidators.validateHeader(publicKey);
            LOGGER.debug("Is publicKey valid {}", isValid);

        }
        if (options != null) {
            isValid &= UnifiedValidators.validateOptions(options);
            LOGGER.debug("Is options valid {}", isValid);

        }
        LOGGER.debug("Is params valid ? {}", isValid);
        return isValid;
    }

    private Boolean isTemporalQuery(MultiMap params) {
        return params.contains(NGSILDQUERY_TIMEREL)
                || params.contains(NGSILDQUERY_TIME)
                || params.contains(NGSILDQUERY_ENDTIME)
                || params.contains(NGSILDQUERY_TIME_PROPERTY);
    }

    private Boolean isSpatialQuery(MultiMap params) {
        return params.contains(NGSILDQUERY_GEOREL)
                || params.contains(NGSILDQUERY_GEOMETRY)
                || params.contains(NGSILDQUERY_GEOPROPERTY)
                || params.contains(NGSILDQUERY_COORDINATES);
    }
}