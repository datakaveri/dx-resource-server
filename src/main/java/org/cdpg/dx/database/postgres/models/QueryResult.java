package org.cdpg.dx.database.postgres.models;

import io.vertx.core.json.JsonObject;
import java.util.List;

public record QueryResult(List<JsonObject> rows, int totalCount, boolean hasMore) {}
