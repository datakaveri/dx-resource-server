package org.cdpg.dx.rs.search.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.exception.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.ResponseUrn.*;
import static org.cdpg.dx.database.elastic.util.Constants.TIME_LIMIT;

@DataObject
public class QueryMapper1 {

    private static final Logger LOGGER = LogManager.getLogger(QueryMapper1.class);

    private boolean isTemporal = false;
    private boolean isGeoSearch = false;
    private boolean isResponseFilter = false;
    private boolean isAttributeSearch = false;

    private int timeLimit;
    private String timeLimitConfig;
    private String tenantPrefix;

    private List<String> id;
    private List<String> attrs;
    private String geometry;
    private String coordinates;
    private String georel;
    private String time;
    private String endtime;
    private String timerel;
    private List<String> attrQuery;
    private String geoProperty;

    public String getOptions() {
        return options;
    }

    private String options;
    private String offSet;
    private String limit;
    private String searchType;
    private ApplicableFilters1 applicableFilters;

    private double lat;
    private double lon;
    private double radius;
    private double maxdistance;
    private double mindistance;

    public QueryMapper1() {}

    public QueryMapper1(JsonObject json) {
        this.isTemporal = json.getBoolean("isTemporal", false);
        this.isGeoSearch = json.getBoolean("isGeoSearch", false);
        this.isResponseFilter = json.getBoolean("isResponseFilter", false);
        this.isAttributeSearch = json.getBoolean("isAttributeSearch", false);
        this.timeLimit = json.getInteger("timeLimit", 0);
        this.tenantPrefix = json.getString("tenantPrefix");
        this.timeLimitConfig = json.getString("timeLimitConfig");

        this.id = json.getJsonArray("id").getList();
        this.attrs = json.getJsonArray("attrs").getList();
        this.geometry = json.getString("geometry");
        this.coordinates = json.getString("coordinates");
        this.georel = json.getString("georel");
        this.time = json.getString("time");
        this.endtime = json.getString("endtime");
        this.timerel = json.getString("timerel");
        this.attrQuery = json.getJsonArray("attrQuery").getList();
        this.geoProperty = json.getString("geoProperty");
        this.options = json.getString("options");
        this.offSet = json.getString("offset");
        this.limit = json.getString("limit");
        this.searchType = json.getString("searchType");
        this.applicableFilters = (ApplicableFilters1) json.getValue("applicableFilters");

        this.lat = json.getDouble("lat", 0.0);
        this.lon = json.getDouble("lon", 0.0);
        this.radius = json.getDouble("radius", 0.0);
        this.maxdistance = json.getDouble("maxdistance", 0.0);
        this.mindistance = json.getDouble("mindistance", 0.0);
    }

    public QueryMapper1(int timeLimit, String tenantPrefix, String timeLimitConfig) {
        this.timeLimit = timeLimit;
        this.tenantPrefix = tenantPrefix;
        this.timeLimitConfig = timeLimitConfig;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("isTemporal", isTemporal)
                .put("isGeoSearch", isGeoSearch)
                .put("isResponseFilter", isResponseFilter)
                .put("isAttributeSearch", isAttributeSearch)
                .put("timeLimit", timeLimit)
                .put("tenantPrefix", tenantPrefix)
                .put("timeLimitConfig", timeLimitConfig)
                .put("id", id)
                .put("attrs", attrs)
                .put("geometry", geometry)
                .put("coordinates", coordinates)
                .put("georel", georel)
                .put("time", time)
                .put("endtime", endtime)
                .put("timerel", timerel)
                .put("attrQuery", attrQuery)
                .put("geoProperty", geoProperty)
                .put("options", options)
                .put("offset", offSet)
                .put("limit", limit)
                .put("searchType", searchType)
                .put("applicableFilters", applicableFilters)
                .put("lat", lat)
                .put("lon", lon)
                .put("radius", radius)
                .put("maxdistance", maxdistance)
                .put("mindistance", mindistance);
    }
    public JsonObject toQueryMapperJson(NgsildQueryParams1 params, boolean isTemporal) {
        return toQueryMapperJson(params, isTemporal, false);
    }
    public JsonObject toQueryMapperJson(NgsildQueryParams1 params, boolean isTemporal, boolean isAsyncQuery) {
        LOGGER.info("Generating query JSON from NgsildQueryParams: {}", params);

        this.isTemporal = isTemporal;
        JsonObject json = new JsonObject();

        if (params.getId() != null) {
            json.put(JSON_ID, new JsonArray(params.getId().stream().map(Object::toString).toList()));
        }
        if (params.getAttrs() != null) {
            isResponseFilter = true;
            json.put(JSON_ATTRIBUTE_FILTER, new JsonArray(params.getAttrs()));
        }
        if (isGeoQuery(params)) {
            if(allGeoParamsPresent(params))
            processGeoQuery(json, params);
            else
                throw
                        new DxRuntimeException(
                                BAD_REQUEST.getValue(),
                                INVALID_GEO_PARAM_URN,
                                "incomplete geo-query geoproperty, geometry, georel, "
                                        + "coordinates all are mandatory.");
        }
        if (isTemporal && params.getTemporalRelation().getTemprel() != null && params.getTemporalRelation().getTime() != null) {
            processTemporalQuery(json, params, isAsyncQuery);
        }
        if (params.getQ() != null) {
            isAttributeSearch = true;
            JsonArray query = new JsonArray();
            String[] qterms = params.getQ().split(";");
            for (String term : qterms) {
                query.add(getQueryTerms(term));
            }
            // JsonArray query
            json.put(JSON_ATTR_QUERY, query);
            attrQuery = query.getList();
        }
        if (params.getGeoProperty() != null) {
            json.put(JSON_GEOPROPERTY, geoProperty);
        }
        if (params.getPageFrom() != null) {
            // String offSet;
            offSet = params.getPageFrom();;
            json.put(NGSILDQUERY_FROM, params.getPageFrom());
        }
        if (params.getPageSize() != null) {
            // String limit;
            limit = params.getPageSize();
            json.put(NGSILDQUERY_SIZE, limit);
        }
        // String searchType;

        if(!timeLimitConfig.isEmpty()){
            json.put(TIME_LIMIT,timeLimitConfig);
        }
        if(!tenantPrefix.isEmpty()){
            json.put("tenant",tenantPrefix);
        }
        json.put(JSON_SEARCH_TYPE, getSearchType(isAsyncQuery));

        return json;
    }

    private void processGeoQuery(JsonObject json, NgsildQueryParams1 params) {
        isGeoSearch = true;
        if (params.getGeometry().equalsIgnoreCase(GEOM_POINT) &&
                params.getGeoRel().getRelation().equals(JSON_NEAR) &&
                params.getGeoRel().getMaxDistance() != null) {

            String[] coords = params.getCoordinates().replaceAll("\\[|\\]", "").split(",");
            lat = Double.parseDouble(coords[0]);
            lon = Double.parseDouble(coords[1]);
            radius = params.getGeoRel().getMaxDistance();

            json.put(JSON_LAT, lat);
            json.put(JSON_LON, lon);
            json.put(JSON_RADIUS, radius);
        } else {
            geometry = params.getGeometry();
            coordinates = params.getCoordinates();
            georel = (params.getGeoRel() != null && params.getGeoRel().getRelation() != null)
                    ? params.getGeoRel().getRelation()
                    : JSON_WITHIN;


            json.put(JSON_GEOMETRY, geometry);
            json.put(JSON_COORDINATES, coordinates);
            json.put(JSON_GEOREL, georel);

            if (params.getGeoRel().getMaxDistance() != null) {
                maxdistance = params.getGeoRel().getMaxDistance();

                json.put(JSON_MAXDISTANCE, maxdistance);
            } else if (params.getGeoRel().getMinDistance() != null) {
                mindistance = params.getGeoRel().getMinDistance();

                json.put(JSON_MINDISTANCE, mindistance);
            }
        }
    }

    private void processTemporalQuery(JsonObject json, NgsildQueryParams1 params, boolean isAsyncQuery) {
        time= params.getTemporalRelation().getTime();
        timerel = params.getTemporalRelation().getTemprel();

        json.put(JSON_TIME, time);
        json.put(JSON_TIMEREL, timerel);

        if (params.getTemporalRelation().getEndTime() != null) {
            endtime = params.getTemporalRelation().getEndTime();
            json.put(JSON_ENDTIME, params.getTemporalRelation().getEndTime());
        }
        isValidTimeInterval(
                JSON_DURING, time, endtime, isAsyncQuery);
    }

    private void isValidTimeInterval(String timeRel, String time, String endTime, boolean isAsyncQuery) {
        LOGGER.info("Validating time interval: timerel={}, time={}, endTime={}, isAsyncQuery={}", timeRel, time, endTime, isAsyncQuery);

        if (timeRel.equalsIgnoreCase(JSON_DURING)) {
            if (isNullOrEmpty(time) || isNullOrEmpty(endTime)) {
                throw new DxRuntimeException(
                        BAD_REQUEST.getValue(),
                        INVALID_TEMPORAL_PARAM_URN,
                        "Both 'time' and 'endTime' are mandatory for 'during' temporal queries."
                );
            }

            try {
                ZonedDateTime start = ZonedDateTime.parse(time);
                ZonedDateTime end = ZonedDateTime.parse(endTime);
                long totalDaysAllowed = Duration.between(start, end).toDays();

                if ((isAsyncQuery && totalDaysAllowed > timeLimit) || (!isAsyncQuery && totalDaysAllowed > timeLimit)) {
                    throw new DxRuntimeException(
                            BAD_REQUEST.getValue(),
                            INVALID_TEMPORAL_PARAM_URN,
                            "Time interval greater than " + timeLimit + " days is not allowed."
                    );
                }
            } catch (Exception ex) {
                LOGGER.error("Invalid time format: time={}, endTime={}", time, endTime, ex);
                throw new DxRuntimeException(
                        BAD_REQUEST.getValue(),
                        INVALID_TEMPORAL_PARAM_URN,
                        "Invalid time format provided."
                );
            }
        }
    }

    JsonObject getQueryTerms(final String queryTerms) {
        JsonObject json = new JsonObject();

        String[] attributes = queryTerms.split(";");
        LOGGER.info("Processing attributes: {}", Arrays.toString(attributes));

        for (String attr : attributes) {
            // Use a cleaner approach to extract operator, value, and attribute
            String[] attributeQueryTerms = attr.split("((?<==)|(?==)|(?=>)|(?=<)|(?=!)|(?<=!)|(?===))");

            LOGGER.info("Parsed terms: {}", Arrays.toString(attributeQueryTerms));

            if (attributeQueryTerms.length < 3 || attributeQueryTerms.length > 4) {
                throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage());
            }

            String jsonOperator = attributeQueryTerms.length == 3 ? attributeQueryTerms[1] :
                    attributeQueryTerms[1] + attributeQueryTerms[2];

            String jsonValue = attributeQueryTerms.length == 3 ? attributeQueryTerms[2] :
                    attributeQueryTerms[3];

            json.put(JSON_OPERATOR, jsonOperator);
            json.put(JSON_VALUE, jsonValue);
            json.put(JSON_ATTRIBUTE, attributeQueryTerms[0]);  // Keep track of attribute name
        }

        return json;
    }


    private boolean isNullOrEmpty(String value) {
        if (value != null && !value.isEmpty()) {
            return false;
        }
        return true;
    }

    private boolean isGeoQuery(NgsildQueryParams1 params) {
        return params.getGeoRel().getRelation() != null ||
                params.getCoordinates() != null ||
                params.getGeometry() != null ||
                params.getGeoProperty() != null;
    }

    private boolean allGeoParamsPresent(NgsildQueryParams1 params){
        return  params.getGeoRel().getRelation() != null
                && params.getCoordinates() != null
                && params.getGeometry() != null
                && params.getGeoProperty() != null;
    }

    private String getSearchType(boolean isAsyncQuery) {
        return isTemporal ? JSON_TEMPORAL_SEARCH :
                isGeoSearch ? JSON_GEO_SEARCH :
                        isResponseFilter ? JSON_RESPONSE_FILTER_SEARCH :
                                isAttributeSearch ? JSON_ATTRIBUTE_SEARCH : "";
    }
    public int failureCode() {
        return HttpStatusCode.BAD_REQUEST.getValue();
    }
    public String failureMessage() {
        return INVALID_PARAM_VALUE_URN.getMessage();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
