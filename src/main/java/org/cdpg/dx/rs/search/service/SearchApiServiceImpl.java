package org.cdpg.dx.rs.search.service;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import net.sf.saxon.trans.SymbolicName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.elastic.service.ElasticsearchService;
import org.cdpg.dx.rs.search.model.ApplicableFilters;
import org.cdpg.dx.rs.search.model.RequestDTO;
import org.cdpg.dx.rs.search.util.RequestType;
import org.cdpg.dx.rs.search.util.ResponseModel;
import org.cdpg.dx.rs.search.util.validatorTypes.ParamsValidator;

public class SearchApiServiceImpl implements SearchApiService {

    private static final Logger LOGGER = LogManager.getLogger(SearchApiServiceImpl.class);
//    private final QueryValidator1 queryValidator;
    private final ElasticsearchService elasticsearchService;

    private final ApplicableFilters applicableFilters;

    public SearchApiServiceImpl(ApplicableFilters applicableFilters,
                                ElasticsearchService elasticsearchService) {
        this.applicableFilters=applicableFilters;
        this.elasticsearchService = elasticsearchService;
    }

    @Override
    public Future<ResponseModel> handleEntitiesQuery(RequestDTO params) {
        return null;
    }

    @Override
    public Future<ResponseModel> handlePostEntitiesQuery(RequestDTO params) {
        return null;
    }

    @Override
    public Future<ResponseModel> handleTemporalQuery(RequestDTO params) {
        return null;
    }

    @Override
    public Future<RequestDTO> createRequestDto(MultiMap requestParams, ApplicableFilters applicableFilters) {
        Promise<RequestDTO> promise= Promise.promise();
        ParamsValidator paramsValidator = new ParamsValidator(applicableFilters);
        Future<Boolean> isParamsValid;
        RequestType requestType = RequestType.valueOf(requestParams.get("requestType"));
            isParamsValid = paramsValidator.validate(requestParams, requestType);
        isParamsValid.onSuccess(successHandler->{
                promise.complete(new RequestDTO(requestParams));
        }).onFailure(failureHandler->{
            promise.fail("Invalid params");
        });

        return promise.future();
    }

    @Override
    public Future<RequestDTO> createRequestDto(JsonObject requestBody,ApplicableFilters applicableFilters){
        Promise<RequestDTO> promise= Promise.promise();
        ParamsValidator paramsValidator = new ParamsValidator(applicableFilters);
        Future<Boolean> isParamsValid;
        RequestType requestType = RequestType.valueOf(requestBody.getString("requestType"));
        isParamsValid = paramsValidator.validate(requestBody, requestType);
        isParamsValid.onSuccess(successHandler->{
            promise.complete(new RequestDTO(requestBody));
        }).onFailure(failureHandler->{
            promise.fail("Invalid params");
        });

        return promise.future();
    }

}
