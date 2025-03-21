package org.cdpg.dx.database.postgres.models;


public record OrderBy(String column, Direction direction) {
    public enum Direction { ASC, DESC }

    public String toSQL() {
        return column + " " + direction.name();
    }
}