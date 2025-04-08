package org.cdpg.dx.rs.search.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.database.elastic.model.QueryDecoder;
import org.cdpg.dx.database.elastic.model.QueryModel;
import org.cdpg.dx.database.elastic.service.ElasticsearchService;
import org.cdpg.dx.rs.search.model.*;

import static iudx.resource.server.apiserver.util.Constants.JSON_COUNT;
public class SearchApiServiceImpl1 implements SearchApiService {

    private static final Logger LOGGER = LogManager.getLogger(SearchApiServiceImpl1.class);
    private final CatalogueService catalogueService;
    private final QueryValidator1 queryValidator;
    private final ElasticsearchService elasticsearchService;

    public SearchApiServiceImpl1(CatalogueService catalogueService,
                                 ElasticsearchService elasticsearchService) {
        this.catalogueService = catalogueService;
        this.queryValidator = new QueryValidator1(catalogueService);
        this.elasticsearchService = elasticsearchService;
    }

    @Override
    public Future<ResponseModel1> handleEntitiesQuery(RequestParamsDTO1 params) {
        LOGGER.info("Processing entity search query.");
        return processSearchQuery(params, false);
    }

    @Override
    public Future<ResponseModel1> handlePostEntitiesQuery(RequestParamsDTO1 params) {
        LOGGER.info("Processing POST entity search query.");
        return processSearchQuery(params, params.getRequestBody().containsKey("temporalQ"));
    }

    @Override
    public Future<ResponseModel1> handleTemporalQuery(RequestParamsDTO1 params) {
        LOGGER.info("Processing temporal search query.");
        return processSearchQuery(params, true);
    }

    private Future<ResponseModel1> processSearchQuery(RequestParamsDTO1 params, boolean isTemporal) {
        Promise<ResponseModel1> promise = Promise.promise();

        catalogueService.fetchCatalogueInfo(params.getQueryParams().get("id"))
                .compose(filters -> {
                    ApplicableFilters1 filters1=new ApplicableFilters1(filters);
                    params.setGroupId(filters1.getGroupId());
                   return queryValidator.validate(filters1, params.getQueryParams());
                })
                .compose(valid -> executeSearch(params, isTemporal))
                .onSuccess(promise::complete)
                .onFailure(promise::fail);

        return promise.future();
    }

    private Future<ResponseModel1> executeSearch(RequestParamsDTO1 params, boolean isTemporal) {
        Promise<ResponseModel1> promise = Promise.promise();
        NgsildQueryParams1 ngsildQuery = new NgsildQueryParams1(params);
        QueryMapper1 queryMapper1 = new QueryMapper1(params.getTimeLimit(), params.getTenantPrefix(), params.getTimeLimitConfig());
        JsonObject queryJson = queryMapper1.toQueryMapperJson(ngsildQuery, isTemporal);
        queryJson.put("resourceGroup",params.getGroupId());

        queryJson.put("searchIndex", getSearchIndex(queryJson, params.getTenantPrefix()));
        executeDatabaseQuery(queryJson, queryMapper1, promise);

        return promise.future();
    }

    private void executeDatabaseQuery(JsonObject query, QueryMapper1 queryMapper1, Promise<ResponseModel1> promise) {
        if (JSON_COUNT.equalsIgnoreCase(queryMapper1.getOptions())) {
            executeCountQuery(query, promise);
        } else {
            executeSearchQuery(query, promise);
        }
    }

    private void executeCountQuery(JsonObject query, Promise<ResponseModel1> promise) {
        QueryModel queryModel = new QueryDecoder().getQuery(query);
        elasticsearchService.count(query.getString("searchIndex"), queryModel)
                .onSuccess(count -> promise.complete(new ResponseModel1(count)))
                .onFailure(promise::fail);
    }

    private void executeSearchQuery(JsonObject query, Promise<ResponseModel1> promise) {
        QueryModel queryModel = new QueryDecoder().getQuery(query);
        elasticsearchService.search(query.getString("searchIndex"), queryModel)
                .onSuccess(results -> promise.complete(new ResponseModel1(results)))
                .onFailure(promise::fail);
    }


    private String getSearchIndex(JsonObject json, String tenantPrefix) {
        String resourceGroup = json.getString("resourceGroup");
        return tenantPrefix.equalsIgnoreCase("none") ? resourceGroup : tenantPrefix + "__" + resourceGroup;
    }
}
