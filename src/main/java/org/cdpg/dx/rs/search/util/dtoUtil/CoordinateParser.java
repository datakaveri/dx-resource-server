package org.cdpg.dx.rs.search.util.dtoUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoordinateParser {

    /**
     * Parses a coordinate string into a list of coordinate pairs.
     * 
     * @param coordinatesString the coordinate string (e.g., \"[[1.0,2.0],[3.0,4.0]]\" or \"1.0,2.0,3.0,4.0\")
     * @param geometryType the geometry type, which affects parsing logic
     * @return a list of coordinate pairs (each pair is a List<Double>)
     */
    public static List<List<Double>> parse(String coordinatesString, String geometryType) {
        // Remove all square brackets
        coordinatesString = coordinatesString.replaceAll("\\[", "").replaceAll("]", "");
        String[] coordinates = coordinatesString.split(",");
        List<List<Double>> coordinateList = new ArrayList<>();

        if (geometryType == null) {
            return null;
        }

        // For "Point", we expect only one pair of coordinates.
        if (geometryType.equalsIgnoreCase("point")) {
            List<Double> point = new ArrayList<>();
            for (String coordinate : coordinates) {
                if (!coordinate.isEmpty()) {
                    point.add(Double.parseDouble(coordinate.trim()));
                }
            }
            coordinateList.add(point);
            return coordinateList;
        }
        
        // For bbox, linestring, and polygon, we expect coordinate pairs.
        for (int i = 0; i < coordinates.length - 1; i += 2) {
            String first = coordinates[i].trim();
            String second = coordinates[i + 1].trim();
            if (!first.isEmpty() && !second.isEmpty()) {
                coordinateList.add(Arrays.asList(Double.parseDouble(first), Double.parseDouble(second)));
            }
        }
        return coordinateList;
    }
}
