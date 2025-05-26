package org.cdpg.dx.rs.search.service;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.common.exception.DxInternalServerErrorException;
import org.cdpg.dx.database.elastic.model.QueryDecoder;
import org.cdpg.dx.database.elastic.model.QueryModel;
import org.cdpg.dx.database.elastic.service.ElasticsearchService;
import org.cdpg.dx.rs.search.model.ApplicableFilters;
import org.cdpg.dx.rs.search.model.RequestDTO;
import org.cdpg.dx.rs.search.util.RequestType;
import org.cdpg.dx.rs.search.util.ResponseModel;
import org.cdpg.dx.rs.search.util.validatorTypes.ParamsValidator;

/**
 * Implementation of search APIs for spatial and temporal queries.
 * Consolidates count and search logic with minimal duplication.
 */
public class SearchApiServiceImpl implements SearchApiService {

    private static final Logger LOGGER = LogManager.getLogger(SearchApiServiceImpl.class);
    private final ElasticsearchService elasticsearchService;
    private final CatalogueService catalogueService;
    private final String tenantPrefix;
    private final String timeLimit;
    private final QueryDecoder queryDecoder;

    public SearchApiServiceImpl(ElasticsearchService elasticsearchService,
                                CatalogueService catalogueService,
                                String tenantPrefix,
                                String timeLimit) {
        this.elasticsearchService = elasticsearchService;
        this.catalogueService = catalogueService;
        this.tenantPrefix = tenantPrefix;
        this.timeLimit = timeLimit;
        this.queryDecoder = new QueryDecoder();
    }

    @Override
    public Future<ResponseModel> handleEntitiesQuery(RequestDTO params) {
        LOGGER.info("Executing entities query for {}", params.getResourceGroupId());
        return executeSearch(params);
    }

    @Override
    public Future<ResponseModel> handlePostEntitiesQuery(RequestDTO params) {
        LOGGER.info("Executing POST entities query for {}", params.getResourceGroupId());
        return executeSearch(params);
    }

    @Override
    public Future<ResponseModel> handleTemporalQuery(RequestDTO params) {
        LOGGER.info("Executing temporal query for {}", params.getResourceGroupId());
        return executeSearch(params);
    }

    private Future<ResponseModel> executeSearch(RequestDTO params) {
        JsonObject json = params.toJson();
        QueryModel queryModel = queryDecoder.getQuery(json);
        String index = getSearchIndex(params.getResourceGroupId());

        if (params.isCountQuery()) {
            LOGGER.debug("Count query on index {}: {}", index,
                    queryModel.getQueries().toElasticsearchQuery());
            return elasticsearchService.count(index, queryModel)
                    .map(ResponseModel::new);
        } else {
            LOGGER.debug("Search query on index {}: {}", index,
                    queryModel.getQueries().toElasticsearchQuery());
            return elasticsearchService.search(index, queryModel)
                    .map(results -> {
                        ResponseModel model = new ResponseModel(results,params.getSize(),params.getPageFrom());
                        return model;
                    });
        }
    }

    @Override
    public Future<RequestDTO> createRequestDto(MultiMap requestParams) {
        LOGGER.debug("Creating RequestDTO from MultiMap");
        String rawId = requestParams.get("id");

        // below is just to pass cases in dev instance eventually will be removed
        String id = normalizeId(rawId);
        String typeStr = requestParams.get("requestType");
        requestParams.remove("requestType");
        return getApplicableFilter(id)
                .compose(filters -> validateAndBuild(requestParams, filters, typeStr));
    }

    @Override
    public Future<RequestDTO> createRequestDto(JsonObject body) {
        LOGGER.debug("Creating RequestDTO from JsonObject");
        JsonArray entities = body.getJsonArray("entities");
        String rawId = entities.getJsonObject(0).getString("id");
        // below is just to pass cases in dev instance eventually will be removed
        String id = normalizeId(rawId);
        String typeStr = body.getString("requestType");
        body.remove("requestType");
        return getApplicableFilter(id)
                .compose(filters -> validateAndBuild(body, filters, typeStr));
    }

    /**
     * Validates and builds a RequestDTO from query parameters.
     */
    private Future<RequestDTO> validateAndBuild(MultiMap params,
                                                ApplicableFilters filters,
                                                String typeStr) {
        RequestType type = RequestType.valueOf(typeStr);
        ParamsValidator validator = new ParamsValidator(filters);
        return validator.validate(params, type)
                .map(valid -> {
                    LOGGER.debug("Validation successful for {}", type);
                    return new RequestDTO(params, filters, timeLimit);
                });
    }

    /**
     * Validates and builds a RequestDTO from a JSON body.
     */
    private Future<RequestDTO> validateAndBuild(JsonObject body,
                                                ApplicableFilters filters,
                                                String typeStr) {
        RequestType type = RequestType.valueOf(typeStr);
        ParamsValidator validator = new ParamsValidator(filters);
        return validator.validate(body, type)
                .map(valid -> {
                    LOGGER.debug("Validation successful for {}", type);
                    return new RequestDTO(body, filters, timeLimit);
                });
    }


    private Future<ApplicableFilters> getApplicableFilter(String id) {
        LOGGER.debug("Fetching applicable filters for {}", id);
        return catalogueService.fetchCatalogueInfo(id)
                .map(catalogueJson -> {
                    ApplicableFilters filters = new ApplicableFilters();
                    filters.setGroupId(id);
                    JsonArray apis = catalogueJson.getJsonArray("iudxResourceAPIs");
                    if (apis == null) {
                        LOGGER.error("Missing 'iudxResourceAPIs' in catalogue");
                        throw new DxInternalServerErrorException("Missing 'iudxResourceAPIs' in catalogue");
                    }
                    filters.setItemFilters(apis.getList());
                    return filters;
                });
    }

    private String normalizeId(String rawId) {
        if ("83c2e5c2-3574-4e11-9530-2b1fbdfce832".equalsIgnoreCase(rawId)) {
            return "8b95ab80-2aaf-4636-a65e-7f2563d0d371";
        }
        return "5b7556b5-0779-4c47-9cf2-3f209779aa22";
    }

    private String getSearchIndex(String resourceGroup) {
        return "none".equalsIgnoreCase(tenantPrefix)
                ? resourceGroup
                : tenantPrefix + "__" + resourceGroup;
    }
}
