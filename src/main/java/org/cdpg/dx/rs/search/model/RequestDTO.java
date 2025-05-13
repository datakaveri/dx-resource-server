package org.cdpg.dx.rs.search.model;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.rs.search.util.dtoUtil.CoordinateParser;
import org.cdpg.dx.rs.search.util.dtoUtil.GeoQ;
import org.cdpg.dx.rs.search.util.dtoUtil.TemporalQ;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.cdpg.dx.database.elastic.util.Constants.ATTRIBUTE_QUERY_KEY;
import static org.cdpg.dx.database.elastic.util.Constants.COUNT;
import static org.cdpg.dx.util.Constants.*;

public class RequestDTO {
    private static final Logger LOGGER = LogManager.getLogger(RequestDTO.class);

    // Common fields for both path and body parameters
    private String id;
    private List<String> attrs;
    private String searchType;
    private Integer pageFrom;
    private Integer size;
    private ApplicableFilters applicableFilters;
    private boolean isResponseFilter;
    private boolean isGeoSearch;

    public boolean isTemporal() {
        return isTemporal;
    }

    private boolean isTemporal;
    private boolean isAttributeSearch;
    private String timeLimit;

    public boolean isCountQuery() {
        return isCountQuery;
    }

    public void setCountQuery(boolean countQuery) {
        isCountQuery = countQuery;
    }

    private boolean isCountQuery;
    public String getSearchIndex() {
        return searchIndex;
    }

    public void setSearchIndex(String searchIndex) {
        this.searchIndex = searchIndex;
    }

    private String searchIndex;

    public String getResourceGroupId() {
        return resourceGroupId;
    }

    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    private String resourceGroupId;
    // Request Body specific fields
    private GeoQ geoQ;
    private TemporalQ temporalQ;
    JsonArray query;

    public RequestDTO() {}

    // Constructor to build from MultiMap (typically for path params)
    public RequestDTO(MultiMap params,ApplicableFilters applicableFilters,String timeLimit) {
        LOGGER.debug("Trying to create request DTO from params");

        this.id = params.get("id");
        this.applicableFilters=applicableFilters;
        this.resourceGroupId = applicableFilters.getGroupId();
        this.pageFrom = params.contains("offset") ? Integer.parseInt(params.get("offset")) : DEFAULT_FROM_VALUE;
        this.size = params.contains("limit") ? Integer.parseInt(params.get("limit")) : DEFAULT_SIZE_VALUE;
        this.query = params.get("q")!=null?getQueryTerms(params.get("q")):null;
        this.timeLimit=timeLimit;
        this.attrs = splitCommaSeparated(params.get("attrs"));
        this.isCountQuery = params.contains("options") && params.get("options").equals(COUNT);

        if (params.contains("coordinates") && params.get("coordinates") != null) {
            this.isGeoSearch=true;
            this.geoQ = new GeoQ(createGeoJson(params));
            List<List<Double>> parsedCoordinates = CoordinateParser.parse(params.get("coordinates"), geoQ.getGeometry());
            setGeometryCoordinates(parsedCoordinates, geoQ.getGeometry());
        }
        if(params.contains("time") && params.get("time") != null){
            this.isTemporal=true;
            this.temporalQ=new TemporalQ(createTimeJson(params));
        }
        this.searchType =getSearchType();
    }


    // Constructor to build from JSON object (typically for request body)
    public RequestDTO(JsonObject requestBody,ApplicableFilters applicableFilters,String timeLimit) {
        LOGGER.debug("Creating Request Dto in applicable filters from request body");
        this.isCountQuery = requestBody.containsKey("options") && requestBody.getString("options").equals(COUNT);
        this.query = requestBody.getString("q")!=null?getQueryTerms(requestBody.getString("q")):null;
        this.attrs = splitCommaSeparated(requestBody.getString("attrs"));
        this.applicableFilters=applicableFilters;
        this.resourceGroupId = applicableFilters.getGroupId();
        this.timeLimit=timeLimit;
        this.pageFrom = requestBody.containsKey("offset") ? Integer.parseInt(requestBody.getString("offset")) : DEFAULT_FROM_VALUE;
        this.size = requestBody.containsKey("limit") ? Integer.parseInt(requestBody.getString("limit")) : DEFAULT_SIZE_VALUE;

        if (requestBody.containsKey("entities")) {
            JsonArray entitiesArray = requestBody.getJsonArray("entities");
            // Example: extract id from the first entity in the array
            this.id = entitiesArray.getJsonObject(0).getString("id");
        }

        if (requestBody.containsKey("geoQ")) {
            JsonObject geoQJson = requestBody.getJsonObject("geoQ");
            this.geoQ = new GeoQ(geoQJson);
            if (geoQJson.containsKey("coordinates")) {
                this.isGeoSearch=true;
                List<List<Double>> parsedCoordinates = CoordinateParser.parse(
                        geoQJson.getJsonArray("coordinates").toString(), geoQ.getGeometry());
                setGeometryCoordinates(parsedCoordinates, geoQ.getGeometry());
            }
        }

        if (requestBody.containsKey("temporalQ")) {
            this.isTemporal=true;
            this.temporalQ = new TemporalQ(requestBody.getJsonObject("temporalQ"));
        }
        this.searchType =getSearchType();
    }

    private List<String> splitCommaSeparated(String input) {
        if (input == null || input.isBlank()) {
            return Collections.emptyList();
        }
        this.isResponseFilter=true;
        return Arrays.asList(input.split(","));
    }

    /**
     * Set the appropriate geometry coordinates based on the geometry type.
     */
    private void setGeometryCoordinates(List<List<Double>> coordinatesList, String geometryType) {
        if (geometryType == null || coordinatesList == null) {
            return;
        }
        switch (geometryType.toLowerCase()) {
            case "point":
                // For a point, we expect a single list of two coordinates.
                geoQ.setPointCoordinates(coordinatesList.getFirst());
                break;
            case "bbox":
                geoQ.setBboxCoordinates(coordinatesList);
                break;
            case "linestring":
                geoQ.setLinestringCoordinates(coordinatesList);
                break;
            case "polygon":
                geoQ.setPolygonCoordinates(coordinatesList);
                break;
            default:
                // Could log an unsupported geometry type here if needed.
                break;
        }
    }

    private JsonObject createTimeJson(MultiMap params) {
        return new JsonObject().put("time", params.get("time"))
                .put("endtime", params.get("endTime"))
                .put("timerel", params.get("timerel"))
                .put("timeProperty", params.get("timeProperty"));
    }

    private JsonObject createGeoJson(MultiMap params) {
        return new JsonObject()
                .put("geoproperty", params.get("geoproperty"))
                .put("georel", params.get("georel"))
                .put("geometry", params.get("geometry"));
    }

    private String getSearchType() {
        StringBuilder searchType = new StringBuilder();
        if (isTemporal) {
            searchType.append(JSON_TEMPORAL_SEARCH);
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
                : searchType.substring(0, searchType.length() - 1);
    }

    /**
     * Convert this DTO into a JsonObject.
     */

    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        if (id != null) json.put("id", new JsonArray().add(id));
        if (pageFrom != null) json.put("offset", pageFrom);
        if (size != null) json.put("limit", size);
        if (query != null && !query.isEmpty()) json.put(ATTRIBUTE_QUERY_KEY, query);

        if (applicableFilters != null && applicableFilters.getItemFilters() != null && !applicableFilters.getItemFilters().isEmpty()) {
            json.put("applicableFilters", applicableFilters.getItemFilters());
        }

        if (attrs != null && !attrs.isEmpty()) {
            json.put("attrs", new JsonArray(attrs));
        }

        if (searchType != null) json.put("searchType", searchType);
        if (geoQ != null) {
            for(String key : geoQ.toJson().fieldNames()){
                json.put(key,geoQ.toJson().getValue(key));
            }
        }

        if (temporalQ != null) {
            for(String key : temporalQ.toJson().fieldNames()){
                json.put(key,temporalQ.toJson().getValue(key));
            }
        }
        if (timeLimit != null && !timeLimit.isEmpty()) {
            json.put("timeLimit", timeLimit);
        }
        json.put("isCountQuery", isCountQuery);

        return json;
    }

    JsonArray getQueryTerms(final String textQuery) {
        this.query = new JsonArray();
        String[] qterms = textQuery.split(";");
        for (String term : qterms) {

            this.isAttributeSearch=true;
            JsonObject json = new JsonObject();
            String jsonOperator = "";
            String jsonValue = "";
            String jsonAttribute = "";

            String[] attributes = term.split(";");
            for (String attr : attributes) {

                String[] attributeQueryTerms =
                        attr.split("((?=>)|(?<=>)|(?=<)|(?<=<)|(?<==)|(?=!)|(?<=!)|(?==)|(?===))");
                if (attributeQueryTerms.length == 3) {
                    jsonOperator = attributeQueryTerms[1];
                    jsonValue = attributeQueryTerms[2];
                    json.put(JSON_OPERATOR, jsonOperator).put(JSON_VALUE, jsonValue);
                } else if (attributeQueryTerms.length == 4) {
                    jsonOperator = attributeQueryTerms[1].concat(attributeQueryTerms[2]);
                    jsonValue = attributeQueryTerms[3];
                    json.put(JSON_OPERATOR, jsonOperator).put(JSON_VALUE, jsonValue);
                }
                jsonAttribute = attributeQueryTerms[0];
                json.put(JSON_ATTRIBUTE, jsonAttribute);

            }
            query.add(json);

        }
        return query;
    }
    public Integer getPageFrom() {
        return pageFrom;
    }


    public Integer getSize() {
        return size;
    }
    public String getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(String timeLimit) {
        this.timeLimit = timeLimit;
    }
}
