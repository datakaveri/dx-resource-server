package org.cdpg.dx.rs.search.model;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.search.model.ApplicableFilters;
import iudx.resource.server.common.CatalogueService;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.validation.types.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.wololo.jts2geojson.GeoJSONReader;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import static iudx.resource.server.apiserver.search.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.INVALID_GEO_PARAM_URN;
import static iudx.resource.server.common.ResponseUrn.INVALID_GEO_VALUE_URN;

public class QueryValidator1 {

    private static final Logger LOGGER = LogManager.getLogger(QueryValidator1.class);

    private static final Set<String> validParams = new HashSet<>();
    private static final Set<String> validHeaders = new HashSet<>();

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
        validParams.add(NGSILDQUERY_TIME_PROPERTY);
        validParams.add(NGSILDQUERY_FROM);
        validParams.add(NGSILDQUERY_SIZE);
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

    private final CatalogueService catalogueService;

    public QueryValidator1(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    /**
     * Validate a http request.
     *
     * @param parameterMap parameters map of request query
     */
    private boolean validateParams(MultiMap parameterMap) {
        for (Entry<String, String> entry : parameterMap.entries()) {
            if (!validParams.contains(entry.getKey())) {
                LOGGER.error("Validation error : extra field {} not allowed", entry.getKey());
                return false;
            }
        }
        return true;
    }

    /**
     * Validate request parameters.
     *
     * @param applicableFilters applicable filters
     * @param paramsMap         map of request parameters
     * @return Future future JsonObject
     */
    public Future<Boolean> validate(ApplicableFilters applicableFilters, MultiMap paramsMap) {
        Promise<Boolean> promise = Promise.promise();
        boolean isFilterValid = getApplicableFilters(applicableFilters, paramsMap).isFilterValid();

        if (validateParams(paramsMap) && isFilterValid) {
            String geom = paramsMap.get(NGSILDQUERY_GEOMETRY);
            String coords = paramsMap.get(NGSILDQUERY_COORDINATES);

            if (geom != null && coords != null && !isValidCoordinatesForGeometry(geom, coords)) {
                LOGGER.error("Invalid geometry coordinates");
                promise.fail(MSG_BAD_QUERY);
            } else {
                promise.complete(true);
            }
        } else {
            promise.fail(MSG_BAD_QUERY);
        }
        return promise.future();
    }

    /**
     * Validate request parameters.
     *
     * @param requestJson      requestJson of request parameters
     * @param applicableFilters applicable filters
     * @return Future future JsonObject
     */
    public Future<Boolean> validate(JsonObject requestJson, ApplicableFilters applicableFilters) {
        LOGGER.info("Inside VALIDATION ");
        Promise<Boolean> promise = Promise.promise();
        MultiMap paramsMap = convertJsonToMultiMap(requestJson);

        String attrs = paramsMap.get(NGSILDQUERY_ATTRIBUTE);
        String q = paramsMap.get(NGSILDQUERY_Q);
        String coordinates = paramsMap.get(NGSILDQUERY_COORDINATES);
        String geoRel = paramsMap.get(NGSILDQUERY_GEOREL);
        String[] georelArray = geoRel != null ? geoRel.split(";") : null;

        boolean validations1 =
                !(new AttrsTypeValidator(attrs, false).isValid())
                        || !(new QtypeValidator(q, false).isValid())
                        || !(new CoordinatesTypeValidator(coordinates, false).isValid())
                        || !(new GeoRelTypeValidator(georelArray != null ? georelArray[0] : null, false)
                        .isValid())
                        || !(georelArray != null && georelArray.length == 2
                        ? isValidDistance(georelArray[1])
                        : isValidDistance(null));

        validate(applicableFilters, paramsMap)
                .onComplete(
                        handler -> {
                            if (handler.succeeded() && !validations1) {
                                promise.complete(true);
                            } else {
                                promise.fail(MSG_BAD_QUERY);
                            }
                        });
        return promise.future();
    }

    private ApplicableFilters getApplicableFilters(ApplicableFilters applicableFilters, MultiMap paramsMap) {
        List<String> allFilters = applicableFilters.getAllFilters();

        if (isTemporalQuery(paramsMap) && !allFilters.contains("TEMPORAL")) {
            applicableFilters.setFilterValid(false);
            return applicableFilters;
        }
        if (isSpatialQuery(paramsMap) && !allFilters.contains("SPATIAL")) {
            applicableFilters.setFilterValid(false);
            return applicableFilters;
        }
        if (isAttributeQuery(paramsMap) && !allFilters.contains("ATTR")) {
            applicableFilters.setFilterValid(false);
            return applicableFilters;
        }

        applicableFilters.setFilterValid(true);
        return applicableFilters;
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

    private Boolean isAttributeQuery(MultiMap params) {
        return params.contains(NGSILDQUERY_ATTRIBUTE);
    }

    private boolean isValidCoordinatesForGeometry(String geom, String coordinates) {
        if (geom == null && coordinates == null) {
            return true;
        }
        JsonObject json = new JsonObject();
        json.put("coordinates", new JsonArray(coordinates));
        if (geom.equalsIgnoreCase("point")) {
            json.put("type", "Point");
            return isValidCoordinates(json.toString());
        } else if (geom.equalsIgnoreCase("polygon")) {
            json.put("type", "Polygon");
            return isValidCoordinates(json.toString());
        } else if (geom.equalsIgnoreCase("linestring")) {
            json.put("type", "LineString");
            return isValidCoordinates(json.toString());
        } else if (geom.equalsIgnoreCase("bbox")) {
            String[] bboxEdges = coordinates.replaceAll("\\[", "").replaceAll("\\]", "").split(",");
            if (bboxEdges.length != 4) {
                throw new DxRuntimeException(
                        HttpStatusCode.BAD_REQUEST.getValue(),
                        INVALID_GEO_PARAM_URN,
                        INVALID_GEO_PARAM_URN.getMessage());
            }
            json.put("type", "LineString");
            return isValidCoordinates(json.toString());
        } else {
            return false;
        }
    }

    private boolean isValidCoordinates(String geoJson) {
        try {
            GeoJSONReader reader = new GeoJSONReader();
            org.locationtech.jts.geom.Geometry geom = reader.read(geoJson);
            boolean isValidNosCoords = false;
            boolean isPolygon = false;
            if ("Polygon".equalsIgnoreCase(geom.getGeometryType())) {
                isPolygon = true;
                Coordinate[] coords = geom.getCoordinates();
                isValidNosCoords = coords.length < 11;
            }
            return geom.isValid() && (!isPolygon || isValidNosCoords);
        } catch (Exception ex) {
            throw new DxRuntimeException(
                    HttpStatusCode.BAD_REQUEST.getValue(),
                    INVALID_GEO_PARAM_URN,
                    INVALID_GEO_PARAM_URN.getMessage());
        }
    }

    private boolean isValidDistance(String value) {
        if (value == null) {
            return true;
        }
        try {
            String[] distanceArray = value.split("=");
            if (distanceArray.length == 2) {
                String distanceValue = distanceArray[1];
                Validator validator = new DistanceTypeValidator(distanceValue, false);
                return validator.isValid();
            } else {
                throw new DxRuntimeException(
                        HttpStatusCode.BAD_REQUEST.getValue(),
                        INVALID_GEO_VALUE_URN,
                        INVALID_GEO_VALUE_URN.getMessage());
            }
        } catch (Exception ex) {
            LOGGER.error(ex);
            throw new DxRuntimeException(
                    HttpStatusCode.BAD_REQUEST.getValue(),
                    INVALID_GEO_VALUE_URN,
                    INVALID_GEO_VALUE_URN.getMessage());
        }
    }

    private MultiMap convertJsonToMultiMap(JsonObject requestJson) {
        MultiMap paramsMap = MultiMap.caseInsensitiveMultiMap();
        requestJson.forEach(
                entry -> {
                    if (entry.getKey().equalsIgnoreCase("geoQ")
                            || entry.getKey().equalsIgnoreCase("temporalQ")) {
                        JsonObject innerObject = (JsonObject) entry.getValue();
                        paramsMap.add(entry.getKey().toString(), entry.getValue().toString());
                        innerObject.forEach(
                                innerentry -> {
                                    paramsMap.add(innerentry.getKey().toString(), innerentry.getValue().toString());
                                });
                    } else if (entry.getKey().equalsIgnoreCase("entities")) {
                        paramsMap.add(entry.getKey().toString(), entry.getValue().toString());
                        JsonArray array = (JsonArray) entry.getValue();
                        JsonObject innerObject = array.getJsonObject(0);
                        innerObject.forEach(
                                innerentry -> {
                                    paramsMap.add(innerentry.getKey().toString(), innerentry.getValue().toString());
                                });
                    } else {
                        paramsMap.add(entry.getKey().toString(), entry.getValue().toString());
                    }
                });
        return paramsMap;
    }
}
