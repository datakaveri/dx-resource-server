package org.cdpg.dx.rs.search.util.validatorTypes;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import org.cdpg.dx.rs.search.model.ApplicableFilters;

import java.util.List;

import static org.cdpg.dx.util.Constants.*;


/**
 * Validates query filters against RS group/item capabilities.
 * This class follows the Open/Closed Principle since new filters or rules can be added
 * without modifying its external behavior.
 */
public class QueryFiltersValidator {
  private final ApplicableFilters applicableFilters;

  public QueryFiltersValidator(ApplicableFilters applicableFilters) {
    this.applicableFilters = applicableFilters;
  }

  public Future<Boolean> validate(MultiMap paramsMap) {
    Promise<Boolean> promise = Promise.promise();
    List<String> filters = applicableFilters.getItemFilters();
    if (isTemporalQuery(paramsMap) && !filters.contains("TEMPORAL")) {
      promise.fail("Temporal parameters are not supported by RS Item.");
      return promise.future();
    }
    if (isSpatialQuery(paramsMap) && !filters.contains("SPATIAL")) {
      promise.fail("Spatial parameters are not supported by RS Item.");
      return promise.future();
    }
    if (isAttributeQuery(paramsMap) && !filters.contains("ATTR")) {
      promise.fail("Attribute parameters are not supported by RS Item.");
      return promise.future();
    }
    promise.complete(true);
    return promise.future();
  }

  private boolean isTemporalQuery(MultiMap params) {
    return params.contains(NGSILDQUERY_TIMEREL) ||
            params.contains(NGSILDQUERY_TIME) ||
            params.contains(NGSILDQUERY_ENDTIME) ||
            params.contains(NGSILDQUERY_TIME_PROPERTY);
  }

  private boolean isSpatialQuery(MultiMap params) {
    return params.contains(NGSILDQUERY_GEOREL) ||
            params.contains(NGSILDQUERY_GEOMETRY) ||
            params.contains(NGSILDQUERY_GEOPROPERTY) ||
            params.contains(NGSILDQUERY_COORDINATES);
  }

  private boolean isAttributeQuery(MultiMap params) {
    return params.contains(NGSILDQUERY_ATTRIBUTE);
  }
}