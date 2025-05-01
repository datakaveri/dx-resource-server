package org.cdpg.dx.rs.search.util.dtoUtil;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

import static org.cdpg.dx.util.Constants.NGSILDQUERY_MAXDISTANCE;
import static org.cdpg.dx.util.Constants.NGSILDQUERY_MINDISTANCE;


public class GeoQ {
    private static final Logger LOGGER = LogManager.getLogger(GeoQ.class);

    private String geometry;
    private List<Double> pointCoordinates;              // For Point
    private List<List<Double>> bboxCoordinates;      // For bbox
    private List<List<Double>> linestringCoordinates;  // For linestring
    private List<List<Double>> polygonCoordinates;// For Polygon
    private Double lat;
    private Double lon;
    private String geoRel;
    private String geoproperty;
    private Double maxDistance;
    private Double minDistance;

    public GeoQ() {}

    /**
     * Constructs a GeoQ instance from the given JSON object.
     */
    public GeoQ(JsonObject geoQJson) {
        this.geometry = geoQJson.getString("geometry");
        this.geoRel = geoQJson.getString("georel");  // Fixed: use geoRel from JSON
        parseGeoRel(); // Parse the geo relation to extract distances
        this.geoproperty = geoQJson.getString("geoproperty");
        // Additional fields (like coordinates) can be set later as needed.
    }

    /**
     * Parses the geoRel string to extract the primary geo relation and any distance parameters.
     * Expected format: "relation;key=value", e.g., "near;maxDistance=1000"
     */
    private void parseGeoRel() {
        LOGGER.info("Inside parseGeoRel.");
        if (this.geoRel != null && !this.geoRel.isEmpty()) {
            String[] parts = this.geoRel.split(";");
            // Primary geo relation
            this.geoRel = parts[0];
            if (parts.length == 2) {
                String[] distanceParts = parts[1].split("=");
                if (distanceParts.length == 2) {
                    try {
                        double distanceValue = Double.parseDouble(distanceParts[1]);
                        if (distanceParts[0].equalsIgnoreCase(NGSILDQUERY_MAXDISTANCE)) {
                            this.maxDistance = distanceValue;
                        } else if (distanceParts[0].equalsIgnoreCase(NGSILDQUERY_MINDISTANCE)) {
                            this.minDistance = distanceValue;
                        }
                    } catch (NumberFormatException e) {
                        // Handle invalid distance format if needed (e.g., log the error)
                    }
                }
            }
        }
    }

    /**
     * Serializes this GeoQ object to a JsonObject.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("geometry", geometry);
        json.put("georel", geoRel);
        json.put("geoproperty", geoproperty);
        if (pointCoordinates != null) {
            json.put("lat", lat);
            json.put("lon",lon);
        }
        if (bboxCoordinates != null) {
            json.put("coordinates", new JsonArray(bboxCoordinates));
        }
        if (linestringCoordinates != null) {
            json.put("coordinates", new JsonArray(Arrays.asList(linestringCoordinates.toArray())));
        }
        if (polygonCoordinates != null) {
            json.put("coordinates", new JsonArray(polygonCoordinates));
        }
        if (maxDistance != null) {
            json.put("radius", maxDistance);
        }
        if (minDistance != null) {
            json.put("radius", minDistance);
        }
        return json;
    }

    // Getters and Setters

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public List<Double> getPointCoordinates() {
        return pointCoordinates;
    }

    public void setPointCoordinates(List<Double> pointCoordinates) {
        this.lat=pointCoordinates.get(0);
        this.lon=pointCoordinates.get(1);
        this.pointCoordinates = pointCoordinates;
    }

    public List<List<Double>> getBboxCoordinates() {
        return bboxCoordinates;
    }

    public void setBboxCoordinates(List<List<Double>> bboxCoordinates) {
        this.bboxCoordinates = bboxCoordinates;
    }

    public List<List<Double>> getLinestringCoordinates() {
        return linestringCoordinates;
    }

    public void setLinestringCoordinates(List<List<Double>> linestringCoordinates) {
        this.linestringCoordinates = linestringCoordinates;
    }

    public List<List<Double>> getPolygonCoordinates() {
        return polygonCoordinates;
    }

    public void setPolygonCoordinates(List<List<Double>> polygonCoordinates) {
        this.polygonCoordinates = polygonCoordinates;
    }

    public String getGeoRel() {
        return geoRel;
    }

    public void setGeoRel(String geoRel) {
        this.geoRel = geoRel;
        parseGeoRel();
    }

    public String getGeoproperty() {
        return geoproperty;
    }

    public void setGeoproperty(String geoproperty) {
        this.geoproperty = geoproperty;
    }

    public Double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(Double maxDistance) {
        this.maxDistance = maxDistance;
    }

    public Double getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(Double minDistance) {
        this.minDistance = minDistance;
    }


}
