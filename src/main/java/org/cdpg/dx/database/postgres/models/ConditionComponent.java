package org.cdpg.dx.database.postgres.models;

import java.util.List;

public interface ConditionComponent {
    String toSQL();
    List<Object> getQueryParams();
}
