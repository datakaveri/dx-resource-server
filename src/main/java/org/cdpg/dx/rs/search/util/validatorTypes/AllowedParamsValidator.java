package org.cdpg.dx.rs.search.util.validatorTypes;

import io.vertx.core.MultiMap;

import java.util.Set;

import static org.cdpg.dx.util.Constants.*;

/**
 * Checks that only allowed parameters are present.
 * This class adheres to the Single Responsibility Principle.
 */
public class AllowedParamsValidator {
  private static final Set<String> VALID_PARAMS = Set.of(
          NGSILDQUERY_TYPE, NGSILDQUERY_ID, NGSILDQUERY_IDPATTERN, NGSILDQUERY_ATTRIBUTE,
          NGSILDQUERY_Q, NGSILDQUERY_GEOREL, NGSILDQUERY_GEOMETRY, NGSILDQUERY_COORDINATES,
          NGSILDQUERY_GEOPROPERTY, NGSILDQUERY_TIMEPROPERTY, NGSILDQUERY_TIME, NGSILDQUERY_TIMEREL,
          NGSILDQUERY_ENDTIME, NGSILDQUERY_ENTITIES, NGSILDQUERY_GEOQ, NGSILDQUERY_TEMPORALQ,
          NGSILDQUERY_TIME_PROPERTY, NGSILDQUERY_FROM, NGSILDQUERY_SIZE, IUDXQUERY_OPTIONS
  );

  public boolean areParamsAllowed(MultiMap parameterMap) {
    return VALID_PARAMS.containsAll(parameterMap.names());
  }
}