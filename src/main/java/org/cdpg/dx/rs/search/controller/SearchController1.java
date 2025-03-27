package org.cdpg.dx.rs.search.controller;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.rs.search.model.RequestParamsDTO1;
import org.cdpg.dx.rs.search.service.SearchApiService;

import java.util.List;
import java.util.Map;

public class SearchController1 {
    private static final Logger LOGGER = LogManager.getLogger(SearchController1.class);
    private final Router router;
    private final SearchApiService searchApiService;
    private final int timeLimit;
    private final String timeLimitConfig;
    private final String tenantPrefix;

    public SearchController1(Vertx vertx, Router router, SearchApiService searchApiService, int timeLimit, String timeLimitConfig, String tenantPrefix) {
        this.router = router;
        this.searchApiService = searchApiService;
        this.timeLimit = timeLimit;
        this.timeLimitConfig = timeLimitConfig;
        this.tenantPrefix = tenantPrefix;
        initializeRoutes();
    }

    private void initializeRoutes() {
        router.get("/search/entities").handler(this::getEntitiesQuery);
        router.post("/search/entities").handler(this::postEntitiesQuery);
        router.get("/search/temporal").handler(this::getTemporalQuery);
    }

    private void getEntitiesQuery(RoutingContext context) {
        RequestParamsDTO1 requestParams = extractRequestParams(context);
        processSearchQuery(requestParams, context.response(), searchApiService.handleEntitiesQuery(requestParams));
    }

    private void postEntitiesQuery(RoutingContext context) {
        RequestParamsDTO1 requestParams = extractRequestParams(context);
        processSearchQuery(requestParams, context.response(), searchApiService.handlePostEntitiesQuery(requestParams));
    }

    private void getTemporalQuery(RoutingContext context) {
        RequestParamsDTO1 requestParams = extractRequestParams(context);
        processSearchQuery(requestParams, context.response(), searchApiService.handleTemporalQuery(requestParams));
    }

    private RequestParamsDTO1 extractRequestParams(RoutingContext context) {
        Map<String, List<String>> queryParams = (Map<String, List<String>>) context.queryParams().entries();
        String host = context.request().getHeader("Host");
        // instead of requestparams create individual variable;
        return new RequestParamsDTO1(queryParams, host, timeLimit, tenantPrefix, timeLimitConfig);
    }

    private void processSearchQuery(RequestParamsDTO1 requestParams, HttpServerResponse response, Future<?> searchFuture) {
        searchFuture.onComplete(result -> {
            if (result.succeeded()) {
                response.setStatusCode(200).end(result.result().toString());
            } else {
                LOGGER.error("Search Query Failed: {}", result.cause().getMessage());
                response.setStatusCode(500).end("Internal Server Error");
            }
        });
    }
}
