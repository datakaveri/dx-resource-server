package iudx.resource.server.apiserver.query;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.ResponseUrn.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * QueryMapper class to convert NGSILD query into json object for the purpose of information
 * exchange among different verticals. TODO Need to add documentation.
 */
public class QueryMapper {

  private static final Logger LOGGER = LogManager.getLogger(QueryMapper.class);
  private boolean isTemporal = false;
  private boolean isGeoSearch = false;
  private boolean isResponseFilter = false;
  private boolean isAttributeSearch = false;
  private RoutingContext context;
  private int timeLimit;

  public QueryMapper(RoutingContext context, int timeLimit) {
    this.context = context;
    this.timeLimit = timeLimit;
  }

  public JsonObject toJson(NgsildQueryParams params, boolean isTemporal) {
    return toJson(params, isTemporal, false);
  }

  /**
   * This method is used to create a json object from NGSILDQueryParams.
   *
   * @param params A map of query parameters passed.
   * @param isTemporal flag indicating whether temporal or not.
   * @param isAsyncQuery flag indicating whether the call is made for Async API or not.
   * @return JsonObject result.
   */
  public JsonObject toJson(NgsildQueryParams params, boolean isTemporal, boolean isAsyncQuery) {
    LOGGER.trace("Info QueryMapper#toJson() started");
    LOGGER.debug("Info : params" + params);
    this.isTemporal = isTemporal;
    JsonObject json = new JsonObject();

    if (params.getId() != null) {
      JsonArray jsonArray = new JsonArray();
      params.getId().forEach(s -> jsonArray.add(s.toString()));
      json.put(JSON_ID, jsonArray);
      LOGGER.debug("Info : json " + json);
    }
    if (params.getAttrs() != null) {
      isResponseFilter = true;
      JsonArray jsonArray = new JsonArray();
      params.getAttrs().forEach(attribute -> jsonArray.add(attribute));
      json.put(JSON_ATTRIBUTE_FILTER, jsonArray);
      LOGGER.debug("Info : json " + json);
    }
    if (isGeoQuery(params)) {
      if (params.getGeoRel().getRelation() != null
          && params.getCoordinates() != null
          && params.getGeometry() != null
          && params.getGeoProperty() != null) {
        isGeoSearch = true;
        if (params.getGeometry().equalsIgnoreCase(GEOM_POINT)
            && params.getGeoRel().getRelation().equals(JSON_NEAR)
            && params.getGeoRel().getMaxDistance() != null) {
          String[] coords = params.getCoordinates().replaceAll("\\[|\\]", "").split(",");
          json.put(JSON_LAT, Double.parseDouble(coords[0]));
          json.put(JSON_LON, Double.parseDouble(coords[1]));
          json.put(JSON_RADIUS, params.getGeoRel().getMaxDistance());
        } else {
          json.put(JSON_GEOMETRY, params.getGeometry());
          json.put(JSON_COORDINATES, params.getCoordinates());
          json.put(JSON_GEOREL, getOrDefault(params.getGeoRel().getRelation(), JSON_WITHIN));
          if (params.getGeoRel().getMaxDistance() != null) {
            json.put(JSON_MAXDISTANCE, params.getGeoRel().getMaxDistance());
          } else if (params.getGeoRel().getMinDistance() != null) {
            json.put(JSON_MINDISTANCE, params.getGeoRel().getMinDistance());
          }
        }
        LOGGER.debug("Info : json " + json);
      } else {
        LOGGER.debug("context :{}", context);
        DxRuntimeException ex =
            new DxRuntimeException(
                BAD_REQUEST.getValue(),
                INVALID_GEO_PARAM_URN,
                "incomplete geo-query geoproperty, geometry, georel, "
                    + "coordinates all are mandatory.");
        this.context.fail(400, ex);
        return null;
      }
    }
    if (isTemporal
        && params.getTemporalRelation().getTemprel() != null
        && params.getTemporalRelation().getTime() != null) {
      isTemporal = true;
      if (params.getTemporalRelation().getTemprel().equalsIgnoreCase(JSON_DURING)
          || params.getTemporalRelation().getTemprel().equalsIgnoreCase(JSON_BETWEEN)) {
        LOGGER.debug("Info : inside during ");

        json.put(JSON_TIME, params.getTemporalRelation().getTime());
        json.put(JSON_ENDTIME, params.getTemporalRelation().getEndTime());
        json.put(JSON_TIMEREL, params.getTemporalRelation().getTemprel());

        isValidTimeInterval(
            JSON_DURING, json.getString(JSON_TIME), json.getString(JSON_ENDTIME), isAsyncQuery);
      } else {
        json.put(JSON_TIME, params.getTemporalRelation().getTime().toString());
        json.put(JSON_TIMEREL, params.getTemporalRelation().getTemprel());
      }
      LOGGER.debug("Info : json " + json);
    }
    if (params.getQ() != null) {
      isAttributeSearch = true;
      JsonArray query = new JsonArray();
      String[] qterms = params.getQ().split(";");
      for (String term : qterms) {
        query.add(getQueryTerms(term));
      }
      json.put(JSON_ATTR_QUERY, query);
      LOGGER.debug("Info : json " + json);
    }
    if (params.getGeoProperty() != null) {
      json.put(JSON_GEOPROPERTY, params.getGeoProperty());
      LOGGER.debug("Info : json " + json);
    }
    if (params.getOptions() != null) {
      json.put(IUDXQUERY_OPTIONS, params.getOptions());
      LOGGER.debug("Info : json " + json);
    }
    if (params.getPageFrom() != null) {
      json.put(NGSILDQUERY_FROM, params.getPageFrom());
    }
    if (params.getPageSize() != null) {
      json.put(NGSILDQUERY_SIZE, params.getPageSize());
    }

    json.put(JSON_SEARCH_TYPE, getSearchType(isAsyncQuery));
    LOGGER.debug("Info : json " + json);
    return json;
  }

  /*
   * check for a valid days interval for temporal queries
   */
  // TODO : decide how to enforce for before and after queries.
  private void isValidTimeInterval(
      String timeRel, String time, String endTime, boolean isAsyncQuery) {
    long totalDaysAllowed = 0;
    if (timeRel.equalsIgnoreCase(JSON_DURING)) {
      if (isNullorEmpty(time) || isNullorEmpty(endTime)) {
        DxRuntimeException ex =
            new DxRuntimeException(
                BAD_REQUEST.getValue(),
                INVALID_TEMPORAL_PARAM_URN,
                "time and endTime both are mandatory for during Query.");
        this.context.fail(400, ex);
      }

      try {
        ZonedDateTime start = ZonedDateTime.parse(time);
        ZonedDateTime end = ZonedDateTime.parse(endTime);
        Duration duration = Duration.between(start, end);
        totalDaysAllowed = duration.toDays();
      } catch (Exception ex) {
        DxRuntimeException exc =
            new DxRuntimeException(
                BAD_REQUEST.getValue(),
                INVALID_TEMPORAL_PARAM_URN,
                "time and endTime both are mandatory for during Query.");
        this.context.fail(400, exc);
      }
    }
    if (isAsyncQuery && totalDaysAllowed > timeLimit) {
      DxRuntimeException ex =
          new DxRuntimeException(
              BAD_REQUEST.getValue(),
              INVALID_TEMPORAL_PARAM_URN,
              "time interval greater than " + timeLimit + " days is not allowed");
      this.context.fail(400, ex);
    }
    if (!isAsyncQuery && totalDaysAllowed > timeLimit) {
      DxRuntimeException ex =
          new DxRuntimeException(
              BAD_REQUEST.getValue(),
              INVALID_TEMPORAL_PARAM_URN,
              "time interval greater than " + timeLimit + " days is not allowed");
      this.context.fail(400, ex);
    }
  }

  private boolean isGeoQuery(NgsildQueryParams params) {
    LOGGER.debug(
        "georel " + params.getGeoRel() + " relation : " + params.getGeoRel().getRelation());
    return params.getGeoRel().getRelation() != null
        || params.getCoordinates() != null
        || params.getGeometry() != null
        || params.getGeoProperty() != null;
  }

  private boolean isNullorEmpty(String value) {
    if (value != null && !value.isEmpty()) {
      return false;
    }
    return true;
  }

  private <T> T getOrDefault(T value, T def) {
    return (value == null) ? def : value;
  }

  private String getSearchType(boolean isAsyncQuery) {
    StringBuilder searchType = new StringBuilder();
    if (isTemporal) {
      searchType.append(JSON_TEMPORAL_SEARCH);
    } else if (!isTemporal && !isAsyncQuery) {
      searchType.append(JSON_LATEST_SEARCH);
    }
    if (isGeoSearch) {
      searchType.append(JSON_GEO_SEARCH);
    }
    if (isResponseFilter) {
      searchType.append(JSON_RESPONSE_FILTER_SEARCH);
    }
    if (isAttributeSearch) {
      searchType.append(JSON_ATTRIBUTE_SEARCH);
    }
    return searchType.toString().isEmpty()
        ? ""
        : searchType.substring(0, searchType.length() - 1).toString();
  }

  JsonObject getQueryTerms(final String queryTerms) {
    JsonObject json = new JsonObject();
    String jsonOperator = "";
    String jsonValue = "";
    String jsonAttribute = "";

    String[] attributes = queryTerms.split(";");
    LOGGER.info("Attributes : {} ", attributes);

    for (String attr : attributes) {

      String[] attributeQueryTerms =
          attr.split("((?=>)|(?<=>)|(?=<)|(?<=<)|(?<==)|(?=!)|(?<=!)|(?==)|(?===))");
      LOGGER.info(Arrays.stream(attributeQueryTerms).collect(Collectors.toList()));
      LOGGER.info(attributeQueryTerms.length);
      if (attributeQueryTerms.length == 3) {
        jsonOperator = attributeQueryTerms[1];
        jsonValue = attributeQueryTerms[2];
        json.put(JSON_OPERATOR, jsonOperator).put(JSON_VALUE, jsonValue);
      } else if (attributeQueryTerms.length == 4) {
        jsonOperator = attributeQueryTerms[1].concat(attributeQueryTerms[2]);
        jsonValue = attributeQueryTerms[3];
        json.put(JSON_OPERATOR, jsonOperator).put(JSON_VALUE, jsonValue);
      } else {
        throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage());
      }
      jsonAttribute = attributeQueryTerms[0];
      json.put(JSON_ATTRIBUTE, jsonAttribute);
    }

    return json;
  }

  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  public String failureMessage() {
    return INVALID_PARAM_VALUE_URN.getMessage();
  }
}
