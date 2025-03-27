package org.cdpg.dx.rs.search.service;

import io.vertx.core.Future;
import org.cdpg.dx.rs.search.model.RequestParamsDTO1;
import org.cdpg.dx.rs.search.model.ResponseModel1;

public interface SearchApiService {
    Future<ResponseModel1> handleEntitiesQuery(RequestParamsDTO1 params);
    Future<ResponseModel1> handlePostEntitiesQuery(RequestParamsDTO1 params);
    Future<ResponseModel1> handleTemporalQuery(RequestParamsDTO1 params);
}
