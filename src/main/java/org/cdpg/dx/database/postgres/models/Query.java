package org.cdpg.dx.database.postgres.models;

import java.util.List;

public interface Query {
    String toSQL();
    List<Object> getQueryParams();
}