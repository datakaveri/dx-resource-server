package org.cdpg.dx.database.elastic.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.elastic.exception.EsQueryException;
import org.cdpg.dx.database.elastic.util.QueryType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cdpg.dx.database.elastic.util.Constants.*;

public class GeoQueryFiltersDecorator implements ElasticsearchQueryDecorator {

  private static final Logger LOGGER = LogManager.getLogger(GeoQueryFiltersDecorator.class);
  private Map<QueryType, List<QueryModel>> queryFilters;
  private JsonObject requestQuery;
  private String geoQuery =
          "{ \"geo_shape\": { \"%s\": { \"shape\": %s, \"relation\": \"%s\" } } }";

  public GeoQueryFiltersDecorator(
          Map<QueryType, List<QueryModel>> queryFilters, JsonObject requestQuery) {
    this.queryFilters = queryFilters;
    this.requestQuery = requestQuery;
  }

  @Override
  public Map<QueryType, List<QueryModel>> add() {
    QueryModel geoWrapperQuery = new QueryModel(QueryType.GEO_SHAPE);
    if (requestQuery.containsKey(LON)
            && requestQuery.containsKey(LAT)
            && requestQuery.containsKey(GEO_RADIUS)) {
      // circle
      requestQuery.put(GEOMETRY, "Point");
      String relation = requestQuery.containsKey(GEOREL) ? requestQuery.getString(GEOREL) : WITHIN;

      Map<String, Object> geoJsonParams = getGeoJson(requestQuery);
      geoJsonParams.put("relation", relation);
      geoJsonParams.put(GEO_PROPERTY, "location");
      geoWrapperQuery.setQueryParameters(geoJsonParams);
    } else if (requestQuery.containsKey(GEOMETRY)
            && (requestQuery.getString(GEOMETRY).equalsIgnoreCase(POLYGON)
            || requestQuery.getString(GEOMETRY).equalsIgnoreCase(LINESTRING))
            && requestQuery.containsKey(GEOREL)
            && requestQuery.containsKey(COORDINATES_KEY)
            && requestQuery.containsKey(GEO_PROPERTY)) {
      // polygon & linestring
      String relation = requestQuery.getString(GEOREL);

      if (!isValidCoordinates(
              requestQuery.getString(GEOMETRY), new JsonArray(requestQuery.getString("coordinates")))) {
        throw new EsQueryException("Coordinate mismatch (Polygon)");
      }
      Map<String, Object> geoJsonParams = getGeoJson(requestQuery);
      geoJsonParams.put("relation", relation);
      geoJsonParams.put(GEO_PROPERTY, "location");
      geoWrapperQuery.setQueryParameters(geoJsonParams);
    } else if (requestQuery.containsKey(GEOMETRY)
            && requestQuery.getString(GEOMETRY).equalsIgnoreCase(BBOX)
            && requestQuery.containsKey(GEOREL)
            && requestQuery.containsKey(COORDINATES_KEY)
            && requestQuery.containsKey(GEO_PROPERTY)) {
      // bbox
      String relation = requestQuery.getString(GEOREL);
      Map<String, Object> geoJsonParams = getGeoJson(requestQuery);
      geoJsonParams.put("relation", relation);
      geoJsonParams.put(GEO_PROPERTY, "location");
      geoWrapperQuery.setQueryParameters(geoJsonParams);
    } else {
      throw new EsQueryException("Missing/Invalid geo parameters");
    }
    List<QueryModel> queryList = queryFilters.get(QueryType.FILTER);
    queryList.add(geoWrapperQuery);
    return queryFilters;
  }

  private Map<String, Object> getGeoJson(JsonObject json) {
    LOGGER.debug(json);
    String geom;
    JsonArray coordinates;
    Map<String, Object> queryParameters = new HashMap<String, Object>();
    if ("Point".equalsIgnoreCase(json.getString(GEOMETRY))) {
      double lat = json.getDouble(LAT);
      double lon = json.getDouble(LON);
      geom = "Circle";
      queryParameters.put("radius", json.getString("radius") + "m");
      coordinates = new JsonArray().add(lon).add(lat);
    } else if ("bbox".equalsIgnoreCase(json.getString(GEOMETRY))) {
      geom = "envelope";
      coordinates = new JsonArray(json.getString("coordinates"));
    } else {
      geom = json.getString(GEOMETRY);
      coordinates = new JsonArray(json.getString("coordinates"));
    }
    queryParameters.put("type", geom);
    queryParameters.put("coordinates", coordinates);
    LOGGER.debug(queryParameters.toString());
    return queryParameters;
  }

  private boolean isValidCoordinates(String geometry, JsonArray coordinates) {
    int length = coordinates.getJsonArray(0).size();
    if (geometry.equalsIgnoreCase(POLYGON)
            && !coordinates
            .getJsonArray(0)
            .getJsonArray(0)
            .getDouble(0)
            .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
            && !coordinates
            .getJsonArray(0)
            .getJsonArray(0)
            .getDouble(1)
            .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1))) {
      return false;
    }
    return true;
  }
}