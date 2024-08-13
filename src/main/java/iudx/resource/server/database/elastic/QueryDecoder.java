package iudx.resource.server.database.elastic;

import static iudx.resource.server.common.ResponseUrn.UNAUTHORIZED_ATTRS_URN;
import static iudx.resource.server.database.archives.Constants.*;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.elastic.exception.EsQueryException;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryDecoder {

  private static final Logger LOGGER = LogManager.getLogger(QueryDecoder.class);

  public static <T> List<T> getCommonAttributes(List<T> list1, List<T> list2) {
    Set<T> set1 = new HashSet<>(list1);
    if (set1.isEmpty()) {
      return list2;
    }
    Set<T> set2 = new HashSet<>(list2);
    set1.retainAll(set2);
    return new ArrayList<>(set1);
  }

  public Query getQuery(JsonObject jsonQuery) {
    return getQuery(jsonQuery, false);
  }

  public Query getQuery(JsonObject jsonQuery, boolean isAsyncQuery) {

    String searchType = jsonQuery.getString(SEARCH_TYPE);
    Boolean isValidQuery = false;
    boolean temporalQuery = false;

    String[] timeLimitConfig = getTimeLimitArray(jsonQuery, isAsyncQuery);
    int defaultDateForDevDeployment = 0;

    Map<FilterType, List<Query>> queryLists = new HashMap<>();

    for (FilterType filterType : FilterType.values()) {
      queryLists.put(filterType, new ArrayList<Query>());
    }

    // add id to every elastic query
    JsonArray id = jsonQuery.getJsonArray("id");
    FieldValue field = FieldValue.of(id.getString(0));
    TermsQueryField termQueryField = TermsQueryField.of(e -> e.value(List.of(field)));
    Query idTermsQuery = TermsQuery.of(query -> query.field("id").terms(termQueryField))._toQuery();

    queryLists.get(FilterType.FILTER).add(idTermsQuery);
    ElasticsearchQueryDecorator queryDecorator = null;
    if (searchType.matches(TEMPORAL_SEARCH_REGEX)
        && jsonQuery.containsKey(REQ_TIMEREL)
        && jsonQuery.containsKey(TIME_KEY)) {

      if (!isAsyncQuery) {
        defaultDateForDevDeployment = Integer.valueOf(timeLimitConfig[2]);
      }
      queryDecorator =
          new TemporalQueryFiltersDecorator(queryLists, jsonQuery, defaultDateForDevDeployment);
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
      defaultDateForDevDeployment = Integer.valueOf(timeLimitConfig[2]);
      new TemporalQueryFiltersDecorator(queryLists, jsonQuery, defaultDateForDevDeployment)
          .addDefaultTemporalFilters(queryLists, jsonQuery);
    }

    Query q = getBoolQuery(queryLists);

    LOGGER.info("query : {}", q.toString());
    return q;
  }

  private String[] getTimeLimitArray(JsonObject jsonQuery, boolean isAsyncQuery) {
    if (isAsyncQuery) {
      return new String[] {};
    }
    String[] timeLimitConfig = jsonQuery.getString(TIME_LIMIT).split(",");
    return timeLimitConfig;
  }

  public SourceConfig getSourceConfigFilters(JsonObject queryJson) {
    String searchType = queryJson.getString(SEARCH_TYPE);
    JsonArray maxAccessibleAttrs = queryJson.getJsonArray("accessibleAttrs");
    JsonArray responseFilteringFields = queryJson.getJsonArray(RESPONSE_ATTRS);
    LOGGER.debug(
        "searchType: {}, maxAccessibleAttrs: {} , responseFilteringFields: {}",
        searchType,
        maxAccessibleAttrs,
        responseFilteringFields);

    if (!searchType.matches(RESPONSE_FILTER_REGEX)) {
      List<String> accessibleAttrsList = maxAccessibleAttrs.getList();

      if (!accessibleAttrsList.isEmpty()) {
        return getSourceFilter(accessibleAttrsList);
      }
      return getSourceFilter(Collections.emptyList());
    }
    if (responseFilteringFields == null) {
      LOGGER.error("response filtering fields are not passed in attrs parameter");
      throw new EsQueryException("response filtering fields are not passed in attrs parameter");
    }

    List<String> commonAttributes =
        getCommonAttributes(maxAccessibleAttrs.getList(), responseFilteringFields.getList());

    LOGGER.debug("finalResponseFilteringFields: {}", commonAttributes);
    if (commonAttributes.isEmpty()) {
      JsonObject json = new JsonObject();
      json.put(TYPE_KEY, 401);
      json.put(TITLE, UNAUTHORIZED_ATTRS_URN.getUrn());
      throw new EsQueryException(json.toString());
    }

    return getSourceFilter(commonAttributes);
  }

  private SourceConfig getSourceFilter(List<String> sourceFilterList) {
    LOGGER.trace("sourceFilterList: " + sourceFilterList);
    SourceFilter sourceFilter = SourceFilter.of(f -> f.includes(sourceFilterList));
    SourceConfig sourceFilteringFields = SourceConfig.of(c -> c.filter(sourceFilter));
    return sourceFilteringFields;
  }

  private Query getBoolQuery(Map<FilterType, List<Query>> filterQueries) {

    Builder boolQuery = new BoolQuery.Builder();

    for (Map.Entry<FilterType, List<Query>> entry : filterQueries.entrySet()) {
      if (FilterType.FILTER.equals(entry.getKey())
          && filterQueries.get(FilterType.FILTER).size() > 0) {
        boolQuery.filter(filterQueries.get(FilterType.FILTER));
      }

      if (FilterType.MUST_NOT.equals(entry.getKey())
          && filterQueries.get(FilterType.MUST_NOT).size() > 0) {
        boolQuery.mustNot(filterQueries.get(FilterType.MUST_NOT));
      }

      if (FilterType.MUST.equals(entry.getKey()) && filterQueries.get(FilterType.MUST).size() > 0) {
        boolQuery.must(filterQueries.get(FilterType.MUST));
      }

      if (FilterType.SHOULD.equals(entry.getKey())
          && filterQueries.get(FilterType.SHOULD).size() > 0) {
        boolQuery.should(filterQueries.get(FilterType.SHOULD));
      }
    }

    return boolQuery.build()._toQuery();
  }
}
