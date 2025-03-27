package iudx.resource.server.database.elastic.model;

import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.elastic.GeoQueryFiltersDecorator;
import iudx.resource.server.database.elastic.exception.EsQueryException;
import iudx.resource.server.database.elastic.util.QueryType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static iudx.resource.server.database.elastic.util.Constants.*;

public class QueryDecoder {

  private static final Logger LOGGER = LogManager.getLogger(QueryDecoder.class);

  public QueryModel getQuery(JsonObject jsonQuery) {
    return getQuery(jsonQuery, false);
  }

  public QueryModel getQuery(JsonObject jsonQuery, boolean isAsyncQuery) {
    String searchType = jsonQuery.getString(SEARCH_TYPE);
    boolean isValidQuery = false;
    boolean temporalQuery = false;
    LOGGER.info("Processing query");

    String[] timeLimitConfig = getTimeLimitArray(jsonQuery, isAsyncQuery);
    int defaultDateForDevDeployment = 0;
    Map<QueryType, List<QueryModel>> queryLists = new HashMap<>();

    for (QueryType queryType : QueryType.values()) {
      queryLists.put(queryType, new ArrayList<>());
    }

    JsonArray id = jsonQuery.getJsonArray("id");
    if (id == null || id.isEmpty()) {
      throw new EsQueryException("Missing required field: id");
    }

    QueryModel idTermsQuery = new QueryModel(QueryType.TERMS);
    Map<String, Object> idTermsParams = new HashMap<>();
    idTermsParams.put(FIELD, "id");
    idTermsParams.put(VALUE, List.of(id.getString(0)));
    idTermsQuery.setQueryParameters(idTermsParams);
    queryLists.get(QueryType.FILTER).add(idTermsQuery);

    ElasticsearchQueryDecorator queryDecorator = null;
    if (searchType.matches(TEMPORAL_SEARCH_REGEX) && jsonQuery.containsKey(REQ_TIMEREL) && jsonQuery.containsKey(TIME_KEY)) {
      if (!isAsyncQuery) {
        defaultDateForDevDeployment = Integer.parseInt(timeLimitConfig[2]);
      }
      queryDecorator = new TemporalQueryFiltersDecorator(queryLists, jsonQuery, defaultDateForDevDeployment);
      queryDecorator.add();
      temporalQuery = true;
      isValidQuery = true;
    }

    if (searchType.matches(ATTRIBUTE_SEARCH_REGEX)) {
      queryDecorator = new AttributeQueryFiltersDecorator(queryLists, jsonQuery);
      queryDecorator.add();
      isValidQuery = true;
    }

    if (searchType.matches(GEOSEARCH_REGEX)) {
      queryDecorator = new GeoQueryFiltersDecorator(queryLists, jsonQuery);
      queryDecorator.add();
      isValidQuery = true;
    }

    if (!isValidQuery) {
      throw new EsQueryException("Invalid search query");
    }

    boolean isTemporalResource = jsonQuery.getJsonArray("applicableFilters").contains("TEMPORAL");
    if (!isAsyncQuery && !temporalQuery && isTemporalResource) {
      defaultDateForDevDeployment = Integer.parseInt(timeLimitConfig[2]);
      new TemporalQueryFiltersDecorator(queryLists, jsonQuery, defaultDateForDevDeployment).addDefaultTemporalFilters(queryLists, jsonQuery);
    }

    QueryModel q = new QueryModel();
    q.setQueries(getBoolQuery(queryLists));
    return q;
  }

  private String[] getTimeLimitArray(JsonObject jsonQuery, boolean isAsyncQuery) {
    if (isAsyncQuery) {
      return new String[] {};
    }
    String timeLimit = jsonQuery.getString(TIME_LIMIT, "0,0,0");
    return timeLimit.split(",");
  }

  public SourceConfig getSourceConfigFilters(JsonObject queryJson) {
    String searchType = queryJson.getString(SEARCH_TYPE);
    if (!searchType.matches(RESPONSE_FILTER_REGEX)) {
      return getSourceFilter(Collections.emptyList());
    }

    JsonArray responseFilteringFields = queryJson.getJsonArray(RESPONSE_ATTRS);
    if (responseFilteringFields == null) {
      LOGGER.error("Response filtering fields are not passed in attrs parameter");
      throw new EsQueryException("Response filtering fields are not passed in attrs parameter");
    }
    return getSourceFilter(responseFilteringFields.getList());
  }

  private SourceConfig getSourceFilter(List<String> sourceFilterList) {
    SourceFilter sourceFilter = SourceFilter.of(f -> f.includes(sourceFilterList));
    return SourceConfig.of(c -> c.filter(sourceFilter));
  }

  private QueryModel getBoolQuery(Map<QueryType, List<QueryModel>> filterQueries) {
    QueryModel boolQuery = new QueryModel(QueryType.BOOL);

    if (!filterQueries.get(QueryType.FILTER).isEmpty()) {
      boolQuery.setFilterQueries(filterQueries.get(QueryType.FILTER));
    }
    if (!filterQueries.get(QueryType.MUST_NOT).isEmpty()) {
      boolQuery.setMustNotQueries(filterQueries.get(QueryType.MUST_NOT));
    }
    if (!filterQueries.get(QueryType.MUST).isEmpty()) {
      boolQuery.setMustQueries(filterQueries.get(QueryType.MUST));
    }
    if (!filterQueries.get(QueryType.SHOULD).isEmpty()) {
      boolQuery.setShouldQueries(filterQueries.get(QueryType.SHOULD));
    }

    return boolQuery;
  }
}
