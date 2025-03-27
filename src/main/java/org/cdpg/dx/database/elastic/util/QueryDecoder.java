//package iudx.resource.server.database.elastic.util;
//
//import co.elastic.clients.elasticsearch._types.FieldValue;
//import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
//import co.elastic.clients.elasticsearch._types.query_dsl.Query;
//import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
//import co.elastic.clients.elasticsearch.core.search.SourceConfig;
//import co.elastic.clients.elasticsearch.core.search.SourceFilter;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//import iudx.resource.server.database.elastic.model.AttributeQueryFiltersDecorator;
//import iudx.resource.server.database.elastic.model.ElasticsearchQueryDecorator;
//import iudx.resource.server.database.elastic.model.GeoQueryFiltersDecorator;
//import iudx.resource.server.database.elastic.model.QueryModel;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import java.util.*;
//
//import static iudx.resource.server.apiserver.search.util.Constants.*;
//import static iudx.resource.server.database.elastic.util.Constants.*;
//
//public class QueryDecoder {
//
//  private static final Logger LOGGER = LogManager.getLogger(QueryDecoder.class);
//
//  public Query getQuery(JsonObject jsonQuery) {
//    return getQuery(jsonQuery, false);
//  }
//
//  public Query getQuery(JsonObject jsonQuery, boolean isAsyncQuery) {
//
//    String searchType = jsonQuery.getString(SEARCH_TYPE);
//    Boolean isValidQuery = false;
//    boolean temporalQuery = false;
//
//    String[] timeLimitConfig = getTimeLimitArray(jsonQuery, isAsyncQuery);
//    int defaultDateForDevDeployment = 0;
//
//    Map<FilterType, List<Query>> queryLists = new HashMap<>();
//
//    for (FilterType FilterType : FilterType.values()) {
//      queryLists.put(FilterType, new ArrayList<Query>());
//    }
//
//    // add id to every elastic query
//    JsonArray id = jsonQuery.getJsonArray("id");
//    FieldValue field = FieldValue.of(id.getString(0));
//    TermsQueryField termQueryField = TermsQueryField.of(e -> e.value(List.of(field)));
////    Query idTermsQuery = TermsQuery.of(query -> query.field("id").terms(termQueryField))._toQuery();
//    Map<String,Object> idTermsQuery = new HashMap<>();
//    idTermsQuery.put("id",termQueryField);
//
//    QueryModel queryModel = new QueryModel(FilterType.TERMS,idTermsQuery);
//
//
//    //    QueryType idTermsQuery = Query
//
//    queryLists.get(FilterType.FILTER).add(idTermsQuery);
//    ElasticsearchQueryDecorator queryDecorator = null;
//    if (searchType.matches(TEMPORAL_SEARCH_REGEX)
//            && jsonQuery.containsKey(REQ_TIMEREL)
//            && jsonQuery.containsKey(TIME_KEY)) {
//
//      if (!isAsyncQuery) {
//        defaultDateForDevDeployment = Integer.valueOf(timeLimitConfig[2]);
//      }
//      queryDecorator =
//              new TemporalQueryFiltersDecorator(queryLists, jsonQuery, defaultDateForDevDeployment);
//      queryDecorator.add();
//      temporalQuery = true;
//      isValidQuery = true;
//    }
//
//    if (searchType.matches(ATTRIBUTE_SEARCH_REGEX)) {
//      queryDecorator = new AttributeQueryFiltersDecorator(queryLists, jsonQuery);
//      queryDecorator.add();
//      isValidQuery = true;
//    }
//
//    if (searchType.matches(GEOSEARCH_REGEX)) {
//      queryDecorator = new GeoQueryFiltersDecorator(queryLists, jsonQuery);
//      queryDecorator.add();
//      isValidQuery = true;
//    }
//
//    if (!isValidQuery) {
//      throw new EsQueryException("Invalid search query");
//    }
//
//    boolean isTemporalResource = jsonQuery.getJsonArray("applicableFilters").contains("TEMPORAL");
//    if (!isAsyncQuery && !temporalQuery && isTemporalResource) {
//      defaultDateForDevDeployment = Integer.valueOf(timeLimitConfig[2]);
//      new TemporalQueryFiltersDecorator(queryLists, jsonQuery, defaultDateForDevDeployment)
//              .addDefaultTemporalFilters(queryLists, jsonQuery);
//    }
//
//    Query q = getBoolQuery(queryLists);
//
//    LOGGER.info("query : {}", q.toString());
//    return q;
//  }
//
//  private String[] getTimeLimitArray(JsonObject jsonQuery, boolean isAsyncQuery) {
//    if (isAsyncQuery) {
//      return new String[] {};
//    }
//    String[] timeLimitConfig = jsonQuery.getString(TIME_LIMIT).split(",");
//    return timeLimitConfig;
//  }
//
//  public SourceConfig getSourceConfigFilters(JsonObject queryJson) {
//    String searchType = queryJson.getString(SEARCH_TYPE);
//
//    if (!searchType.matches(RESPONSE_FILTER_REGEX)) {
//      return getSourceFilter(Collections.emptyList());
//    }
//
//    JsonArray responseFilteringFileds = queryJson.getJsonArray(RESPONSE_ATTRS);
//    if (responseFilteringFileds == null) {
//      LOGGER.error("response filtering fields are not passed in attrs parameter");
//      throw new EsQueryException("response filtering fields are not passed in attrs parameter");
//    }
//
//    return getSourceFilter(responseFilteringFileds.getList());
//  }
//
//  private SourceConfig getSourceFilter(List<String> sourceFilterList) {
//    SourceFilter sourceFilter = SourceFilter.of(f -> f.includes(sourceFilterList));
//    SourceConfig sourceFilteringFields = SourceConfig.of(c -> c.filter(sourceFilter));
//    return sourceFilteringFields;
//  }
//
//  private Query getBoolQuery(Map<FilterType, List<Query>> filterQueries) {
//
//    BoolQuery.Builder boolQuery = new BoolQuery.Builder();
//
//    for (Map.Entry<FilterType, List<Query>> entry : filterQueries.entrySet()) {
//      if (FilterType.FILTER.equals(entry.getKey())
//              && filterQueries.get(FilterType.FILTER).size() > 0) {
//        boolQuery.filter(filterQueries.get(FilterType.FILTER));
//      }
//
//      if (FilterType.MUST_NOT.equals(entry.getKey())
//              && filterQueries.get(FilterType.MUST_NOT).size() > 0) {
//        boolQuery.mustNot(filterQueries.get(FilterType.MUST_NOT));
//      }
//
//      if (FilterType.MUST.equals(entry.getKey()) && filterQueries.get(FilterType.MUST).size() > 0) {
//        boolQuery.must(filterQueries.get(FilterType.MUST));
//      }
//
//      if (FilterType.SHOULD.equals(entry.getKey())
//              && filterQueries.get(FilterType.SHOULD).size() > 0) {
//        boolQuery.should(filterQueries.get(FilterType.SHOULD));
//      }
//    }
//
//    return boolQuery.build()._toQuery();
//  }
//}