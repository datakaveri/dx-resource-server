//package org.cdpg.dx.rs.search.model;
//
//import io.vertx.core.MultiMap;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//import org.cdpg.dx.rs.search.util.validatorTypes.dtoUtil.GeoQ;
//import org.cdpg.dx.rs.search.util.validatorTypes.dtoUtil.TemporalQ;
//
//import java.util.*;
//
//import static iudx.resource.server.apiserver.util.Constants.*;
//
//public class RequestDTO {
//
//    // Path Params
//    private String id;
//    private String geoproperty;
//    private String georel;
//    private String geometry;
//    private List<Double> coordinates; // For Point
//    private List<List<Double>> bboxCoordinates; // For bbox
//    private List<List<Double>> linestringCoordinates; // For linestring
//    private List<List<Double>> polygonCoordinates; // For Polygon
//    private Integer offset;
//    private Integer limit;
//    private String timerel;
//    private String time;
//    private String endtime;
//    private String q; // Attribute or String-based search
//    private List<String> attrs; // Response filter
//    private String timeProperty;
//    private Double latitude;
//    private Double longitude;
//
//    // Request Body (entityOperations/query)
//    private String type;
//    private List<Map<String, String>> entities;
//    private GeoQ geoQ;
//    private TemporalQ temporalQ;
//    private String options; // For count queries
//
//
//
//
//    public RequestDTO() {}
//
//    // Constructor for Path Params (Vertx MultiMap)
//    public RequestDTO(MultiMap params) {
//        this.id = params.get("id");
//        this.geoproperty = params.get("geoproperty");
//        this.georel = params.get("georel");
//        this.geometry = params.get("geometry");
//        this.offset = params.contains("offset") ? Integer.parseInt(params.get("offset")) : null;
//        this.limit = params.contains("limit") ? Integer.parseInt(params.get("limit")) : null;
//        this.timerel = params.get("timerel");
//        this.time = params.get("time");
//        this.endtime = params.get("endtime");
//        this.q = params.get("q");
//        this.attrs = Arrays.asList(params.get("attrs").split(","));
//        this.timeProperty = params.get("timeProperty");
//        this.options = params.contains("options")?params.get("options"):null;
//
//            if (params.contains("coordinates") && params.get("coordinates") != null) {
//                String coordinatesString = params.get("coordinates");
//                List<List<Double>> parsedCoordinates = parseCoordinates(coordinatesString, this.geometry);
//                setGeometryCoordinates(parsedCoordinates, this.geometry);
//            }
//    }
//
//    // Constructor for Request Body (JsonObject)
//    public RequestDTO(JsonObject requestBody) {
//        this.type = requestBody.getString("type");
//        this.options = requestBody.getString("options");
//
//        if (requestBody.containsKey("entities")) {
//            JsonArray entitiesArray = requestBody.getJsonArray("entities");
//            this.id = entitiesArray.getJsonObject(0).getString("id");
//        }
//
//        if (requestBody.containsKey("geoQ")) {
//            JsonObject geoQJson = requestBody.getJsonObject("geoQ");
//            this.geoQ = new GeoQ(geoQJson);
//            if (geoQJson.containsKey("coordinates")) {
//                JsonArray coordinatesArray = geoQJson.getJsonArray("coordinates");
//                List<List<Double>> parsedCoordinates = parseCoordinates(coordinatesArray.toString(), this.geoQ.getGeometry());
//                setGeometryCoordinates(parsedCoordinates, this.geoQ.getGeometry());
//            }
//        }
//
//        if (requestBody.containsKey("temporalQ")) {
//            JsonObject temporalQJson = requestBody.getJsonObject("temporalQ");
//            this.temporalQ = new TemporalQ(temporalQJson);
//        }
//
//        this.q = requestBody.getString("q");
//        this.attrs = Arrays.asList(requestBody.getString("attrs").split(","));
//    }
//
//    private List<List<Double>> parseCoordinates(String coordinatesString, String geometryType) {
//        // Remove all square brackets
//        coordinatesString = coordinatesString.replaceAll("\\[", "").replaceAll("\\]", "");
//        String[] coordParts = coordinatesString.split(",");
//        List<List<Double>> coordinateList = new ArrayList<>();
//
//        if (geometryType != null) {
//            if (geometryType.equalsIgnoreCase("Point")) {
//                List<Double> pointCoordinates = new ArrayList<>();
//                for (String coordPart : coordParts) {
//                    if (!coordPart.trim().isEmpty()) {
//                        pointCoordinates.add(Double.parseDouble(coordPart.trim()));
//                    }
//                }
//                coordinateList.add(pointCoordinates);
//                return coordinateList;
//            } else if (geometryType.equalsIgnoreCase("bbox")) {
//                for (int i = 0; i < coordParts.length; i += 2) {
//                    if (!coordParts[i].trim().isEmpty() && !coordParts[i + 1].trim().isEmpty()) {
//                        List<Double> pair = new ArrayList<>();
//                        pair.add(Double.parseDouble(coordParts[i].trim()));
//                        pair.add(Double.parseDouble(coordParts[i + 1].trim()));
//                        coordinateList.add(pair);
//                    }
//                }
//                return coordinateList;
//            }else if (geometryType.equalsIgnoreCase("linestring") || geometryType.equalsIgnoreCase("Polygon")) {
//                for (int i = 0; i < coordParts.length; i += 2) {
//                    if (!coordParts[i].trim().isEmpty() && !coordParts[i + 1].trim().isEmpty()) {
//                        List<Double> pair = new ArrayList<>();
//                        pair.add(Double.parseDouble(coordParts[i].trim()));
//                        pair.add(Double.parseDouble(coordParts[i + 1].trim()));
//                        coordinateList.add(pair);
//                    }
//                }
//                return coordinateList;
//            }
//        }
//        return null;
//    }
//
//    private void setGeometryCoordinates(List<List<Double>> coordinatesList, String geometryType) {
//        if (geometryType == null || coordinatesList == null) {
//            return;
//        }
//        if (geometryType.equalsIgnoreCase("Point")) {
//            List<Double> pointCoordinates = coordinatesList.get(0);
//            this.coordinates = pointCoordinates;
//        } else if (geometryType.equalsIgnoreCase("bbox")) {
//            this.bboxCoordinates = coordinatesList;
//        } else if (geometryType.equalsIgnoreCase("linestring")) {
//            this.linestringCoordinates = coordinatesList;
//        } else if (geometryType.equalsIgnoreCase("polygon")) {
//            this.polygonCoordinates = coordinatesList;
//        }
//    }
//
//    // Getters and setters for RequestDTO
//    public String getId() {
//        return id;
//    }
//
//    public void setId(String id) {
//        this.id = id;
//    }
//
//    public String getGeoproperty() {
//        return geoproperty;
//    }
//
//    public void setGeoproperty(String geoproperty) {
//        this.geoproperty = geoproperty;
//    }
//
//    public String getGeorel() {
//        return georel;
//    }
//
//    public void setGeorel(String georel) {
//        this.georel = georel;
//    }
//
//    public String getGeometry() {
//        return geometry;
//    }
//
//    public void setGeometry(String geometry) {
//        this.geometry = geometry;
//    }
//
//    public Double getLatitude() {
//        return latitude;
//    }
//
//    public void setLatitude(Double latitude) {
//        this.latitude = latitude;
//    }
//
//    public Double getLongitude() {
//        return longitude;
//    }
//
//    public void setLongitude(Double longitude) {
//        this.longitude = longitude;
//    }
//
//    public List<Double> getCoordinates() {
//        return coordinates;
//    }
//
//    public void setCoordinates(List<Double> coordinates) {
//        this.coordinates = coordinates;
//    }
//
//    public List<List<Double>> getLinestringCoordinates() {
//        return linestringCoordinates;
//    }
//
//    public void setLinestringCoordinates(List<List<Double>> linestringCoordinates) {
//        this.linestringCoordinates = linestringCoordinates;
//    }
//
//    public List<List<Double>> getBboxCoordinates() {
//        return bboxCoordinates;
//    }
//
//    public void setBboxCoordinates(List<List<Double>> bboxCoordinates) {
//        this.bboxCoordinates = bboxCoordinates;
//    }
//
//    public List<List<Double>> getPolygonCoordinates() {
//        return polygonCoordinates;
//    }
//
//    public void setPolygonCoordinates(List<List<Double>> polygonCoordinates) {
//        this.polygonCoordinates = polygonCoordinates;
//    }
//
//    public Integer getOffset() {
//        return offset;
//    }
//
//    public void setOffset(Integer offset) {
//        this.offset = offset;
//    }
//
//    public Integer getLimit() {
//        return limit;
//    }
//
//    public void setLimit(Integer limit) {
//        this.limit = limit;
//    }
//
//    public String getTimerel() {
//        return timerel;
//    }
//
//    public void setTimerel(String timerel) {
//        this.timerel = timerel;
//    }
//
//    public String getTime() {
//        return time;
//    }
//
//    public void setTime(String time) {
//        this.time = time;
//    }
//
//    public String getEndtime() {
//        return endtime;
//    }
//
//    public void setEndtime(String endtime) {
//        this.endtime = endtime;
//    }
//
//    public String getQ() {
//        return q;
//    }
//
//    public void setQ(String q) {
//        this.q = q;
//    }
//
//    public List<String> getAttrs() {
//        return attrs;
//    }
//
//    public void setAttrs(List<String> attrs) {
//        this.attrs = attrs;
//    }
//
//    public String getTimeProperty() {
//        return timeProperty;
//    }
//
//    public void setTimeProperty(String timeProperty) {
//        this.timeProperty = timeProperty;
//    }
//
//    public String getType() {
//        return type;
//    }
//
//    public void setType(String type) {
//        this.type = type;
//    }
//
//    public List<Map<String, String>> getEntities() {
//        return entities;
//    }
//
//    public void setEntities(List<Map<String, String>> entities) {
//        this.entities = entities;
//    }
//
//    public GeoQ getGeoQ() {
//        return geoQ;
//    }
//
//    public void setGeoQ(GeoQ geoQ) {
//        this.geoQ = geoQ;
//    }
//
//    public TemporalQ getTemporalQ() {
//        return temporalQ;
//    }
//
//    public void setTemporalQ(TemporalQ temporalQ) {
//        this.temporalQ = temporalQ;
//    }
//
//    public String getOptions() {
//        return options;
//    }
//
//    public void setOptions(String options) {
//        this.options = options;
//    }
//
//    @Override
//    public String toString() {
//        return "RequestDTO{" +
//                "id='" + id + '\'' +
//                ", geoproperty='" + geoproperty + '\'' +
//                ", georel='" + georel + '\'' +
//                ", geometry='" + geometry + '\'' +
//                ", coordinates=" + coordinates +
//                ", bboxCoordinates=" + bboxCoordinates +
//                ", linestringCoordinates=" + linestringCoordinates +
//                ", polygonCoordinates=" + polygonCoordinates +
//                ", offset=" + offset +
//                ", limit=" + limit +
//                ", timerel='" + timerel + '\'' +
//                ", time='" + time + '\'' +
//                ", endtime='" + endtime + '\'' +
//                ", q='" + q + '\'' +
//                ", attrs='" + attrs + '\'' +
//                ", timeProperty='" + timeProperty + '\'' +
//                ", latitude=" + latitude +
//                ", longitude=" + longitude +
//                ", type='" + type + '\'' +
//                ", entities=" + entities +
//                ", geoQ=" + geoQ +
//                ", temporalQ=" + temporalQ +
//                ", options='" + options + '\'' +
//                '}';
//    }
//}
//


package org.cdpg.dx.rs.search.model;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.rs.search.util.dtoUtil.CoordinateParser;
import org.cdpg.dx.rs.search.util.dtoUtil.GeoQ;
import org.cdpg.dx.rs.search.util.dtoUtil.TemporalQ;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RequestDTO {

    // Common fields for both path and body parameters
    private String id;
    private String q;
    private List<String> attrs;
    private String type;
    private String options;
    private Integer offset;
    private Integer limit;

    // Request Body specific fields
    private GeoQ geoQ;
    private TemporalQ temporalQ;

    public RequestDTO() {}

    // Constructor to build from MultiMap (typically for path params)
    public RequestDTO(MultiMap params) {
        this.id = params.get("id");

        this.offset = params.contains("offset") ? Integer.parseInt(params.get("offset")) : null;
        this.limit = params.contains("limit") ? Integer.parseInt(params.get("limit")) : null;
        this.q = params.get("q");
        this.attrs = splitCommaSeparated(params.get("attrs"));
        this.options = params.get("options");

        if (params.contains("coordinates") && params.get("coordinates") != null) {
            this.geoQ = new GeoQ(createGeoJson(params));
            List<List<Double>> parsedCoordinates = CoordinateParser.parse(params.get("coordinates"), geoQ.getGeometry());
            setGeometryCoordinates(parsedCoordinates, geoQ.getGeometry());
        }
        if(params.contains("time") && params.get("time") != null){
            this.temporalQ=new TemporalQ(createTimeJson(params));
        }
    }



    // Constructor to build from JSON object (typically for request body)
    public RequestDTO(JsonObject requestBody) {
        this.type = requestBody.getString("type");
        this.options = requestBody.getString("options");
        this.q = requestBody.getString("q");
        this.attrs = splitCommaSeparated(requestBody.getString("attrs"));

        if (requestBody.containsKey("entities")) {
            JsonArray entitiesArray = requestBody.getJsonArray("entities");
            // Example: extract id from the first entity in the array
            this.id = entitiesArray.getJsonObject(0).getString("id");
        }

        if (requestBody.containsKey("geoQ")) {
            JsonObject geoQJson = requestBody.getJsonObject("geoQ");
            this.geoQ = new GeoQ(geoQJson);
            if (geoQJson.containsKey("coordinates")) {
                List<List<Double>> parsedCoordinates = CoordinateParser.parse(
                        geoQJson.getJsonArray("coordinates").toString(), geoQ.getGeometry());
                setGeometryCoordinates(parsedCoordinates, geoQ.getGeometry());
            }
        }

        if (requestBody.containsKey("temporalQ")) {
            this.temporalQ = new TemporalQ(requestBody.getJsonObject("temporalQ"));
        }
    }

    private List<String> splitCommaSeparated(String input) {
        if (input == null || input.isBlank()) {
            return Collections.emptyList();
        }
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

    /**
     * Convert this DTO into a JsonObject.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("id", id)
                .put("offset", offset)
                .put("limit", limit)
                .put("q", q)
                .put("attrs", new JsonArray(attrs))
                .put("type", type)
                .put("geoQ", geoQ != null ? geoQ.toJson() : null)
                .put("temporalQ", temporalQ != null ? temporalQ.toJson() : null)
                .put("options", options);
        return json;
    }
}




