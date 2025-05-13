package org.cdpg.dx.rs.search.util.validatorTypes;

import io.vertx.core.MultiMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.elastic.service.ElasticsearchServiceImpl;

import java.util.*;

import static org.cdpg.dx.util.Constants.*;

/**
 * Checks that only allowed parameters are present.
 * This class adheres to the Single Responsibility Principle.
 */
public class AllowedParamsValidator {
  private static final Logger LOGGER = LogManager.getLogger(AllowedParamsValidator.class);

  private static Set<String> validParams = new HashSet<String>();
  private static Set<String> validHeaders = new HashSet<String>();

  static {
    validParams.add(NGSILDQUERY_TYPE);
    validParams.add(NGSILDQUERY_ID);
    validParams.add(NGSILDQUERY_IDPATTERN);
    validParams.add(NGSILDQUERY_ATTRIBUTE);
    validParams.add(NGSILDQUERY_Q);
    validParams.add(NGSILDQUERY_GEOREL);
    validParams.add(NGSILDQUERY_GEOMETRY);
    validParams.add(NGSILDQUERY_COORDINATES);
    validParams.add(NGSILDQUERY_GEOPROPERTY);
    validParams.add(NGSILDQUERY_TIMEPROPERTY);
    validParams.add(NGSILDQUERY_TIME);
    validParams.add(NGSILDQUERY_TIMEREL);
    validParams.add(NGSILDQUERY_ENDTIME);
    validParams.add(NGSILDQUERY_ENTITIES);
    validParams.add(NGSILDQUERY_GEOQ);
    validParams.add(NGSILDQUERY_TEMPORALQ);
    // Need to check with the timeProperty in Post Query property for NGSI-LD release v1.3.1
    validParams.add(NGSILDQUERY_TIME_PROPERTY);
    validParams.add(NGSILDQUERY_FROM);
    validParams.add(NGSILDQUERY_SIZE);

    // for IUDX count query
    validParams.add(IUDXQUERY_OPTIONS);
  }

  static {
    validHeaders.add(HEADER_OPTIONS);
    validHeaders.add(HEADER_TOKEN);
    validHeaders.add("User-Agent");
    validHeaders.add("Content-Type");
    validHeaders.add(HEADER_CSV);
    validHeaders.add(HEADER_JSON);
    validHeaders.add(HEADER_PARQUET);
  }
  /**
   * Validate a http request.
   *
   * @param parameterMap parameters map of request query
   */
  public boolean areParamsAllowed(MultiMap parameterMap) {
    LOGGER.debug("Checking if the params are allowed");
    final List<Map.Entry<String, String>> entries = parameterMap.entries();
    boolean isValid=true;
    List<String> invalidParams = new ArrayList<>();
    for (final Map.Entry<String, String> entry : entries) {
      if (!validParams.contains(entry.getKey())) {
        invalidParams.add(entry.getKey());
        LOGGER.error("Validation error : extra field {} not allowed", entry.getKey());
        isValid=false;
      }
    }
    if(!isValid) {
      LOGGER.error("Invalid Params " + invalidParams);
    }return isValid;
  }
}