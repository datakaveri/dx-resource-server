package org.cdpg.dx.rs.search.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.CatalogueService;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.elastic.exception.EsQueryException;
import iudx.resource.server.database.elastic.model.QueryDecoder;
import iudx.resource.server.database.elastic.model.QueryModel;
import iudx.resource.server.database.elastic.service.ElasticsearchService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.rs.search.model.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static iudx.resource.server.apiserver.util.Constants.JSON_COUNT;
import static iudx.resource.server.database.elastic.util.Constants.ITEM_TYPES;
import static iudx.resource.server.database.elastic.util.Constants.MALFORMED_ID;

public class SearchApiServiceImpl1 implements SearchApiService {

    private static final Logger LOGGER = LogManager.getLogger(SearchApiServiceImpl1.class);
    private final CatalogueService catalogueService;
    private final QueryValidator1 queryValidator;
    private final ElasticsearchService elasticsearchService;
    private final CacheService cacheService;

    public SearchApiServiceImpl1(CatalogueService catalogueService,
                                 ElasticsearchService elasticsearchService,
                                 CacheService cacheService) {
        this.catalogueService = catalogueService;
        this.queryValidator = new QueryValidator1(catalogueService);
        this.elasticsearchService = elasticsearchService;
        this.cacheService = cacheService;
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

        catalogueService.getApplicableFilters(params.getQueryParams().get("id"))
                .compose(filters -> queryValidator.validate(filters, params.getQueryParams()))
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

        checkQueryAndGetTenant(queryJson)
                .onSuccess(updatedJson -> {
                    updatedJson.put("searchIndex", getSearchIndex(updatedJson, params.getTenantPrefix()));
                    executeDatabaseQuery(updatedJson, queryMapper1, promise);
                })
                .onFailure(promise::fail);

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

    private Future<JsonObject> checkQueryAndGetTenant(JsonObject request) {
        Promise<JsonObject> promise = Promise.promise();
        cacheService.get(new JsonObject().put("type", "CATALOGUE_CACHE").put("key", request.getJsonArray("id").getString(0)))
                .onSuccess(cacheResponse -> {
                    Set<String> types = new HashSet<>(cacheResponse.getJsonArray("type").getList());
                    Set<String> itemTypes = types.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
                    itemTypes.retainAll(ITEM_TYPES);
                    if (!itemTypes.contains("Resource")) {
                        promise.fail(new EsQueryException(ResponseUrn.BAD_REQUEST_URN, MALFORMED_ID));
                    } else {
                        request.put("resourceGroup", cacheResponse.getString("resourceGroup"));
                        promise.complete(request);
                    }
                })
                .onFailure(promise::fail);
        return promise.future();
    }

    private String getSearchIndex(JsonObject json, String tenantPrefix) {
        String resourceGroup = json.getString("resourceGroup");
        return tenantPrefix.equalsIgnoreCase("none") ? resourceGroup : tenantPrefix + "__" + resourceGroup;
    }
}
