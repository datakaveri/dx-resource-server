package org.cdpg.dx.rs.search.service;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.rs.search.model.ApplicableFilters;
import org.cdpg.dx.rs.search.model.RequestDTO;
import org.cdpg.dx.rs.search.util.ResponseModel;

import java.util.jar.JarEntry;

public interface SearchApiService {
    Future<ResponseModel> handleEntitiesQuery(RequestDTO params);
    Future<ResponseModel> handlePostEntitiesQuery(RequestDTO params);
    Future<ResponseModel> handleTemporalQuery(RequestDTO params);

    Future<RequestDTO> createRequestDto (MultiMap requestParams);
    Future<RequestDTO> createRequestDto (JsonObject requestBody);

}
