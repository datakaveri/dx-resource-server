package org.cdpg.dx.database.elastic.service;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.JsonpMapperFeatures;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import jakarta.json.stream.JsonGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.cdpg.dx.common.exception.DxInternalServerErrorException;
import org.cdpg.dx.database.elastic.ElasticClient;
import org.cdpg.dx.database.elastic.model.ElasticsearchResponse;
import org.cdpg.dx.database.elastic.model.QueryModel;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.cdpg.dx.database.elastic.util.Constants.AGGREGATIONS;


public class ElasticsearchServiceImpl implements ElasticsearchService {
    private static final Logger LOGGER = LogManager.getLogger(ElasticsearchServiceImpl.class);

    static ElasticClient client;
    private static ElasticsearchAsyncClient asyncClient;

    public ElasticsearchServiceImpl(ElasticClient client) {
        ElasticsearchServiceImpl.client = client;
        asyncClient = client.getClient();
    }

    @Override
    public Future<List<ElasticsearchResponse>> search(String index, QueryModel queryModel) {
        // Convert QueryModel into Elasticsearch Query and aggregations
        LOGGER.info("Inside search");
        Map<String, Aggregation> elasticsearchAggregations = new HashMap<>();
        if (queryModel.getAggregations() != null && !queryModel.getAggregations().isEmpty()) {
            queryModel.getAggregations().forEach(aggregation -> elasticsearchAggregations.put(aggregation.getAggregationName(), aggregation.toElasticsearchAggregations()));
        }
        QueryModel queries = queryModel.getQueries();
        LOGGER.debug("QUERIES "+queries.toJson());
        Query query = queries == null ? null : queries.toElasticsearchQuery();
        LOGGER.debug("QUERY "+query);

        String size = queryModel.getLimit();
        String from = queryModel.getOffset();
        SearchRequest.Builder requestBuilder = new SearchRequest.Builder().index(index);

        // Optional parameters, set only if not null
        if (query != null) {
            requestBuilder.query(query);
        }
        if (size != null) {
            requestBuilder.size(Integer.valueOf(size));
        }
        if (from != null) {
            requestBuilder.from(Integer.valueOf(from));
        }
        if (!elasticsearchAggregations.isEmpty()) {
            requestBuilder.aggregations(elasticsearchAggregations);
        }
        SourceConfig sourceConfig = queryModel.toSourceConfig();
        if (sourceConfig != null) {
            requestBuilder.source(sourceConfig);
        }
        List<SortOptions> sortOptions = queryModel.toSortOptions();
        if (sortOptions != null) {
            requestBuilder.sort(sortOptions);
        }
        SearchRequest request = requestBuilder.build();
        LOGGER.info("Final SearchRequest: {}", request);

        return executeSearch(request).map(this::convertToElasticSearchResponse);
    }

    @Override
    public Future<Integer> count(String index, QueryModel queryModel) {
        // Convert QueryModel into Elasticsearch Query
        Query query = queryModel.getQueries() == null ? null : queryModel.getQueries().toElasticsearchQuery();
        LOGGER.info("Count query {}",query);
        // Create a CountRequest.Builder for the count query
        CountRequest.Builder requestBuilder = new CountRequest.Builder().index(index);

        // Add query if present
        if (query != null) {
            requestBuilder.query(query);
        }

        CountRequest request = requestBuilder.build();
        LOGGER.debug("Final CountRequest: {}", request);

        // Execute the count query
        return executeCount(request);
    }
    private Future<Integer> executeCount(CountRequest request) {
        Promise<Integer> promise = Promise.promise();
        LOGGER.info("REQUEST {}", request);

        asyncClient.count(request).whenComplete((response, error) -> {
            if (error != null) {
                // Log specific error type for better debugging
                LOGGER.error("Count operation failed. Error type: {}, Message: {}",
                        error.getClass().getSimpleName(),
                        error.getMessage());

                // You might want to handle specific exceptions differently
                {
                    LOGGER.error("Elasticsearch cluster is unreachable");
                    promise.fail(new DxInternalServerErrorException("Elasticsearch cluster is unreachable"));
                }
            } else {
                try {
                    Integer count = Math.toIntExact(response.count());
                    LOGGER.debug("Total document count: {}", count);
                    promise.complete(count);
                } catch (ArithmeticException e) {
                    LOGGER.error("Count value too large for Integer conversion");
                    promise.fail(new DxBadRequestException("Count value too large for Integer conversion"));
                }
            }
        });

        return promise.future();
    }


    private List<ElasticsearchResponse> convertToElasticSearchResponse(SearchResponse<ObjectNode> response) {
        long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
        LOGGER.debug("Total Hits in Elasticsearch Response: {}", totalHits);

        // Parse hits
        List<ElasticsearchResponse> responses = response.hits().hits().stream().map(hit -> {
            String id = hit.id();
            JsonObject source = hit.source() != null ? JsonObject.mapFrom(hit.source()) : new JsonObject();
            return new ElasticsearchResponse(id, source);
        }).collect(Collectors.toList());

        // Parse aggregations if present
        if (response.aggregations() != null && !response.aggregations().isEmpty()) {
            JsonpMapper mapper = asyncClient._jsonpMapper().withAttribute(JsonpMapperFeatures.SERIALIZE_TYPED_KEYS, false);
            StringWriter writer = new StringWriter();
            try (JsonGenerator generator = mapper.jsonProvider().createGenerator(writer)) {
                mapper.serialize(response, generator);
            } catch (Exception e) {
                LOGGER.error("Error serializing aggregations: ", e);
                throw new DxInternalServerErrorException("Failed to process aggregations", e);
            }
            String result = writer.toString();

            // Parse the aggregations object from the serialized result
            JsonObject aggs = new JsonObject(result).getJsonObject(AGGREGATIONS);
            ElasticsearchResponse.setAggregations(aggs);
        }

        return responses;
    }


    private Future<SearchResponse<ObjectNode>> executeSearch(SearchRequest request) {
        Promise<SearchResponse<ObjectNode>> promise = Promise.promise();
        asyncClient.search(request, ObjectNode.class).whenComplete((response, error) -> {
            if (error != null) {
                LOGGER.error("Search operation failed due to {}: {}", error.getClass().getSimpleName(), error.getMessage(), error);

                promise.fail(new DxInternalServerErrorException(error.getMessage(),error));
            } else {
                promise.complete(response);
            }
        });
        return promise.future();
    }

}