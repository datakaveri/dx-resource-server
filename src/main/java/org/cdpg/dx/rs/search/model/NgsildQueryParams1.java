package org.cdpg.dx.rs.search.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static iudx.resource.server.apiserver.search.util.Constants.*;
import static iudx.resource.server.apiserver.search.util.Util.toUriFunction;

/** NGSILDQueryParams Class to parse query parameters from HTTP request. */
public class NgsildQueryParams1 {
    private static final Logger LOGGER = LogManager.getLogger(NgsildQueryParams1.class);

    private List<URI> id = new ArrayList<>();
    private List<String> type = new ArrayList<>();
    private List<String> attrs = new ArrayList<>();
    private List<String> idPattern = new ArrayList<>();
    private String textQuery;
    private GeoRelation1 geoRel;
    private String geometry;
    private String coordinates;
    private String geoProperty;
    private TemporalRelation1 temporalRelation;
    private String options;
    private String pageFrom;
    private String pageSize;

    public NgsildQueryParams1() {}

    public NgsildQueryParams1(JsonObject json) {
        create(json);
    }

    public NgsildQueryParams1(RequestParamsDTO1 queryParams) {
        this.setGeoRel(new GeoRelation1());
        this.setTemporalRelation(new TemporalRelation1());
        this.create(queryParams);
    }

    private void create(JsonObject requestJson) {
        LOGGER.info("create from json started");
        requestJson.forEach(entry -> {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue().toString();
            LOGGER.info("key :: {} value :: {}", key, value);

            switch (key) {
                case NGSILDQUERY_Q -> this.textQuery = requestJson.getString(NGSILDQUERY_Q);
                case NGSILDQUERY_ATTRIBUTE -> this.attrs = Arrays.asList(value.split(","));
                case NGSILDQUERY_TYPE -> this.type = Arrays.asList(value.split(","));
                case "geoQ" -> parseGeoQuery(requestJson.getJsonObject(key));
                case "temporalq" -> parseTemporalQuery(requestJson.getJsonObject(key));
                case "entities" -> parseEntities(new JsonArray(value));
                case IUDXQUERY_OPTIONS -> this.options = value;
                case NGSILDQUERY_FROM -> this.pageFrom = value;
                case NGSILDQUERY_SIZE -> this.pageSize = value;
            }
        });
    }

    private void create(RequestParamsDTO1 queryParams) {
        queryParams.getQueryParams().entries().forEach(entry -> {
            switch (entry.getKey()) {
                case NGSILDQUERY_ID -> this.id = Arrays.stream(entry.getValue().split(","))
                        .map(toUriFunction)
                        .toList();
                case NGSILDQUERY_ATTRIBUTE -> this.attrs = Arrays.asList(entry.getValue().split(","));
                case NGSILDQUERY_GEOREL -> parseGeoRel(entry.getValue());
                case NGSILDQUERY_GEOMETRY -> this.geometry = entry.getValue();
                case NGSILDQUERY_COORDINATES -> this.coordinates = entry.getValue();
                case NGSILDQUERY_TIMEREL -> this.temporalRelation.setTemprel(entry.getValue());
                case NGSILDQUERY_TIME -> this.temporalRelation.setTime(entry.getValue());
                case NGSILDQUERY_ENDTIME -> this.temporalRelation.setEndTime(entry.getValue());
                case NGSILDQUERY_Q -> this.textQuery = entry.getValue();
                case NGSILDQUERY_GEOPROPERTY -> this.geoProperty = entry.getValue();
                case IUDXQUERY_OPTIONS -> this.options = entry.getValue();
                case NGSILDQUERY_SIZE -> this.pageSize = entry.getValue();
                case NGSILDQUERY_FROM -> this.pageFrom = entry.getValue();
            }
        });
    }

    private void parseGeoQuery(JsonObject geoJson) {
        this.setGeometry(geoJson.getString("geometry"));
        this.setGeoProperty(geoJson.getString("geoproperty"));
        this.setCoordinates(geoJson.getJsonArray("coordinates").toString());
        if (geoJson.containsKey("georel")) {
            parseGeoRel(geoJson.getString("georel"));
        }
    }

    private void parseGeoRel(String georel) {
        String[] values = georel.split(";");
        this.geoRel.setRelation(values[0]);
        if (values.length == 2) {
            String[] distance = values[1].split("=");
            if (NGSILDQUERY_MAXDISTANCE.equalsIgnoreCase(distance[0])) {
                this.geoRel.setMaxDistance(Double.parseDouble(distance[1]));
            } else if (NGSILDQUERY_MINDISTANCE.equalsIgnoreCase(distance[0])) {
                this.geoRel.setMinDistance(Double.parseDouble(distance[1]));
            }
        }
    }

    private void parseTemporalQuery(JsonObject temporalJson) {
        this.temporalRelation.setTemprel(temporalJson.getString("timerel"));
        this.temporalRelation.setTime(temporalJson.getString("time"));
        this.temporalRelation.setEndTime(temporalJson.getString("endtime"));
    }

    private void parseEntities(JsonArray array) {
        array.forEach(obj -> {
            JsonObject entity = (JsonObject) obj;
            String id = entity.getString("id");
            String idPattern = entity.getString("idPattern");
            if (id != null) {
                this.id.add(toUri(id));
            }
            if (idPattern != null) {
                this.idPattern.add(idPattern);
            }
        });
    }

    public List<URI> getId() {
        return id;
    }

    public void setId(List<URI> id) {
        this.id = id;
    }

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public List<String> getAttrs() {
        return attrs;
    }

    public void setAttrs(List<String> attrs) {
        this.attrs = attrs;
    }

    public List<String> getIdPattern() {
        return idPattern;
    }

    public void setIdPattern(List<String> idPattern) {
        this.idPattern = idPattern;
    }

    public String getQ() {
        return textQuery;
    }

    public void setQ(String textQuery) {
        this.textQuery = textQuery;
    }

    public GeoRelation1 getGeoRel() {
        return geoRel;
    }

    public void setGeoRel(GeoRelation1 geoRel) {
        this.geoRel = geoRel;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }

    public String getGeoProperty() {
        return geoProperty;
    }

    public void setGeoProperty(String geoProperty) {
        this.geoProperty = geoProperty;
    }

    public TemporalRelation1 getTemporalRelation() {
        return temporalRelation;
    }

    public void setTemporalRelation(TemporalRelation1 temporalRelation) {
        this.temporalRelation = temporalRelation;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getPageFrom() {
        return pageFrom;
    }

    public String getPageSize() {
        return pageSize;
    }

    private URI toUri(String source) {
        try {
            return new URI(source);
        } catch (URISyntaxException e) {
            LOGGER.error("Invalid URI: {}", source, e);
            return null;
        }
    }

    @Override
    public String toString() {
        return "NGSILDQueryParams{" +
                "id=" + id +
                ", type=" + type +
                ", attrs=" + attrs +
                ", idPattern=" + idPattern +
                ", textQuery='" + textQuery + '\'' +
                ", geoRel=" + geoRel +
                ", geometry='" + geometry + '\'' +
                ", coordinates='" + coordinates + '\'' +
                ", geoProperty='" + geoProperty + '\'' +
                ", temporalRelation=" + temporalRelation +
                ", options='" + options + '\'' +
                "}";
    }
}
