package org.cdpg.dx.rs.search.controller;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.rs.search.service.SearchApiService;

import java.util.List;
import java.util.Map;

public class SearchController {
    private static final Logger LOGGER = LogManager.getLogger(SearchController.class);
    private final Router router;
    private final SearchApiService searchApiService;
    private final int timeLimit;
    private final String timeLimitConfig;
    private final String tenantPrefix;

    public SearchController(Vertx vertx, Router router, SearchApiService searchApiService, int timeLimit, String timeLimitConfig, String tenantPrefix) {
        this.router = router;
        this.searchApiService = searchApiService;
        this.timeLimit = timeLimit;
        this.timeLimitConfig = timeLimitConfig;
        this.tenantPrefix = tenantPrefix;
//        initializeRoutes();
    }

//    private void initializeRoutes() {
//        router.get("/search/entities").handler(this::getEntitiesQuery);
//        router.post("/search/entities").handler(this::postEntitiesQuery);
//        router.get("/search/temporal").handler(this::getTemporalQuery);
//    }
//
//    private void getEntitiesQuery(RoutingContext context) {
//        RequestParamsDTO1 requestParams = extractRequestParams(context);
//        processSearchQuery(requestParams, context.response(), searchApiService.handleEntitiesQuery(requestParams));
//    }
//
//    private void postEntitiesQuery(RoutingContext context) {
//        RequestParamsDTO1 requestParams = extractRequestParams(context);
//        processSearchQuery(requestParams, context.response(), searchApiService.handlePostEntitiesQuery(requestParams));
//    }
//
//    private void getTemporalQuery(RoutingContext context) {
//        RequestParamsDTO1 requestParams = extractRequestParams(context);
//        processSearchQuery(requestParams, context.response(), searchApiService.handleTemporalQuery(requestParams));
//    }

}
