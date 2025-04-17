package org.cdpg.dx.rs.search.util.dtoUtil;


import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.wololo.jts2geojson.GeoJSONReader;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.cdpg.dx.util.Constants.*;

public class GeoValidator {

    private static final Logger LOGGER = LogManager.getLogger(GeoValidator.class);

    // Regex patterns for latitude and longitude validation
    private static final String LATITUDE_PATTERN =
            "^(\\+|-)?(?:90(?:(?:\\.0{1,6})?)|(?:[0-9]|[1-8][0-9])(?:(?:\\.[0-9]{1,6})?))$";
    private static final String LONGITUDE_PATTERN =
            "^(\\+|-)?(?:180(?:(?:\\.0{1,6})?)|(?:[0-9]|[1-9][0-9]|1[0-7][0-9])(?:(?:\\.[0-9]{1,6})?))$";
    private static final Pattern COORDINATE_EXTRACT_PATTERN = Pattern.compile("[+-]?\\d+\\.?\\d*");

    // Configuration constants
    private static final int VALIDATION_COORDINATE_PRECISION_ALLOWED = 6;
    private static final int VALIDATION_ALLOWED_COORDINATES = 10;

    private static final List<String> SPATIAL_KEYS = List.of(
            NGSILDQUERY_GEOREL,
            NGSILDQUERY_GEOMETRY,
            NGSILDQUERY_GEOPROPERTY,
            NGSILDQUERY_COORDINATES
    );

    private final DecimalFormat decimalFormat = new DecimalFormat("#.######");

    /**
     * Validates a geo query JSON object.
     *
     * @param geoQuery JsonObject containing keys "geometry", "geoproperty", "georel", and "coordinates".
     * @return true if all fields are valid; false otherwise.
     */
    public boolean validateGeo(final JsonObject geoQuery) {
        if (hasPartialSpatialParams(geoQuery)) {
            LOGGER.error("Partial spatial parameters provided");
            return false;
        }

        String geometry = geoQuery.getString(NGSILDQUERY_GEOMETRY);
        String coordinates = geoQuery.getString(NGSILDQUERY_COORDINATES);
        String geoRel = geoQuery.getString(NGSILDQUERY_GEOREL);
        String geoProperty = geoQuery.getString(NGSILDQUERY_GEOPROPERTY);

        boolean validGeom = isValidGeometry(geometry);
        boolean validGeoProp = isValidGeoProperty(geoProperty);
        boolean validGeoRel = isValidGeoRelation(geoRel);
        boolean validCoords = isValidCoordinatesStructure(coordinates);
        boolean validCoordsWithGeo = isValidCoordinatesForGeometry(geometry, coordinates);

        return validGeom && validGeoProp && validGeoRel && validCoords && validCoordsWithGeo;
    }

    private boolean hasPartialSpatialParams(JsonObject geoQuery) {
        long presentParams = SPATIAL_KEYS.stream()
                .filter(key -> geoQuery.containsKey(key) && geoQuery.getString(key) != null)
                .count();
        return presentParams > 0 && presentParams < SPATIAL_KEYS.size();
    }

    private boolean isValidGeometry(String geometry) {
        return isValidEnum(geometry, GeometryType.class, VALIDATION_ALLOWED_GEOM);
    }

    private boolean isValidGeoProperty(String geoProperty) {
        return isValidField(geoProperty, VALIDATION_ALLOWED_GEOPROPERTY);
    }

    private boolean isValidGeoRelation(String geoRel) {
        if (geoRel == null) return true;
        String[] parts = geoRel.split(";");
        return VALIDATION_ALLOWED_GEOREL.contains(parts[0]);
    }

    private boolean isValidCoordinatesStructure(String coordinates) {
        if (coordinates == null) return false;
        if (coordinates.isBlank()) return false;
        if (!coordinates.startsWith("[") || !coordinates.endsWith("]")) {
            LOGGER.error("Invalid coordinate structure: missing brackets");
            return false;
        }

        List<String> coordValues = extractCoordinateValues(coordinates);
        if (coordValues.isEmpty()) {
            LOGGER.error("No coordinates found");
            return false;
        }

        for (int coordIndex = 0; coordIndex < coordValues.size(); coordIndex++) {
            String coord = coordValues.get(coordIndex);
            if (!isValidCoordinateValue(coord, coordIndex % 2 == 0)) {
                return false;
            }
        }

        return true;
    }

    private List<String> extractCoordinateValues(String coordinates) {
        Matcher matcher = COORDINATE_EXTRACT_PATTERN.matcher(coordinates);
        return matcher.results()
                .map(MatchResult::group)
                .collect(Collectors.toList());
    }

    private boolean isValidCoordinateValue(String value, boolean isLongitude) {
        try {
            BigDecimal decimal = new BigDecimal(value);
            if (decimal.scale() > VALIDATION_COORDINATE_PRECISION_ALLOWED) {
                LOGGER.error("Coordinate exceeds allowed precision: {}", value);
                return false;
            }

            String formatted = decimalFormat.format(decimal.doubleValue());
            if (isLongitude) {
                if (!formatted.matches(LONGITUDE_PATTERN)) {
                    LOGGER.error("Invalid longitude: {}", value);
                    return false;
                }
            } else {
                if (!formatted.matches(LATITUDE_PATTERN)) {
                    LOGGER.error("Invalid latitude: {}", value);
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid coordinate format: {}", value);
            return false;
        }
    }

    private boolean isValidCoordinatesForGeometry(String geometry, String coordinates) {
        if (geometry == null || coordinates == null) return false;

        GeometryType geomType;
        try {
            geomType = GeometryType.valueOf(geometry.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Unsupported geometry type: {}", geometry);
            return false;
        }

        List<String> coordValues = extractCoordinateValues(coordinates);
        int coordCount = coordValues.size();

        switch (geomType) {
            case POINT:
                if (coordCount != 2) {
                    LOGGER.error("Point requires exactly 2 coordinates");
                    return false;
                }
                break;
            case LINESTRING:
                if (coordCount < 4 || coordCount % 2 != 0) {
                    LOGGER.error("LineString requires even coordinates >=4");
                    return false;
                }
                break;
            case POLYGON:
                if (coordCount < 8 || coordCount % 2 != 0) {
                    LOGGER.error("Polygon requires even coordinates >=8");
                    return false;
                }
                if (!isClosedPolygon(coordValues)) {
                    LOGGER.error("Polygon not closed");
                    return false;
                }
                break;
            case BBOX:
                if (coordCount != 4) {
                    LOGGER.error("BBOX requires exactly 4 coordinates");
                    return false;
                }
                break;
        }

        return validateWithJTS(geomType, coordValues);
    }

    private boolean isClosedPolygon(List<String> coordinates) {
        if (coordinates.size() < 8) return false;
        return coordinates.get(0).equals(coordinates.get(coordinates.size() - 2)) &&
                coordinates.get(1).equals(coordinates.get(coordinates.size() - 1));
    }

    private boolean validateWithJTS(GeometryType geomType, List<String> coordValues) {
        try {
            JsonObject geoJson = buildGeoJson(geomType, coordValues);
            GeoJSONReader reader = new GeoJSONReader();
            org.locationtech.jts.geom.Geometry geometry = reader.read(geoJson.toString());

            if (geomType == GeometryType.POLYGON) {
                Coordinate[] coords = geometry.getCoordinates();
                if (coords.length > VALIDATION_ALLOWED_COORDINATES + 1) { // +1 for closure
                    LOGGER.error("Polygon exceeds allowed coordinates");
                    return false;
                }
            }

            return geometry.isValid();
        } catch (Exception e) {
            LOGGER.error("JTS validation failed: {}", e.getMessage());
            return false;
        }
    }

    private JsonObject buildGeoJson(GeometryType geomType, List<String> coordValues) {
        JsonObject geoJson = new JsonObject();
        JsonArray coordinates = new JsonArray();

        switch (geomType) {
            case POINT:
                coordinates.add(parseDouble(coordValues.get(0)))
                        .add(parseDouble(coordValues.get(1)));
                geoJson.put("type", "Point");
                break;
            case LINESTRING:
            case BBOX:
                for (int coordIndex = 0; coordIndex < coordValues.size(); coordIndex += 2) {
                    coordinates.add(new JsonArray()
                            .add(parseDouble(coordValues.get(coordIndex)))
                            .add(parseDouble(coordValues.get(coordIndex + 1))));
                }
                geoJson.put("type", geomType == GeometryType.BBOX ? "LineString" : geomType.getGeoJsonType());
                break;
            case POLYGON:
                JsonArray ring = new JsonArray();
                for (int coordIndex = 0; coordIndex < coordValues.size(); coordIndex += 2) {
                    ring.add(new JsonArray()
                            .add(parseDouble(coordValues.get(coordIndex)))
                            .add(parseDouble(coordValues.get(coordIndex + 1))));
                }
                coordinates.add(ring);
                geoJson.put("type", "Polygon");
                break;
        }

        geoJson.put("coordinates", coordinates);
        return geoJson;
    }

    private double parseDouble(String value) {
        return Double.parseDouble(value);
    }

    private <T extends Enum<T>> boolean isValidEnum(String value, Class<T> enumClass, List<String> allowed) {
        if (value == null) return true;
        try {
            T enumValue = Enum.valueOf(enumClass, value.toUpperCase());
            return allowed.contains(enumValue.name().toLowerCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isValidField(String value, List<String> allowed) {
        if (value == null) return true;
        return allowed.contains(value.toLowerCase());
    }

    private enum GeometryType {
        POINT("Point"),
        LINESTRING("LineString"),
        POLYGON("Polygon"),
        BBOX("LineString");

        private final String geoJsonType;

        GeometryType(String geoJsonType) {
            this.geoJsonType = geoJsonType;
        }

        public String getGeoJsonType() {
            return geoJsonType;
        }
    }
}