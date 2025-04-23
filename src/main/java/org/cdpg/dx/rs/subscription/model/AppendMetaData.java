package org.cdpg.dx.rs.subscription.model;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.database.postgres.models.QueryResult;


public record AppendMetaData(QueryResult selectQueryResult, JsonObject catalogue) {}
