package org.cdpg.dx.database.elastic.model;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.json.JsonData;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.cdpg.dx.common.exception.DxInternalServerErrorException;
import org.cdpg.dx.database.elastic.util.AggregationFactory;
import org.cdpg.dx.database.elastic.util.AggregationType;
import org.cdpg.dx.database.elastic.util.BoolOperator;
import org.cdpg.dx.database.elastic.util.QueryType;

import static org.cdpg.dx.database.elastic.util.Constants.*;

/**
 * Represents a query model for constructing Elasticsearch searches, with support for basic query
 * types, boolean logic, filters, and aggregations.
 */
@DataObject(generateConverter = true, publicConverter = false)
public class QueryModel {
  private static final Logger LOGGER = LogManager.getLogger(QueryModel.class);
  String minimumShouldMatch;
  String limit;
  String offset;
  QueryModel queries;
  private QueryType queryType;
  private Map<String, Object> queryParameters;
  private List<QueryModel> subQueries;
  private List<QueryModel> mustQueries;
  private List<QueryModel> shouldQueries;
  private List<QueryModel> mustNotQueries;
  private List<QueryModel> filterQueries;
  private BoolOperator boolOperator;

  // For aggregations
  private List<QueryModel> aggregations;
  private String aggregationName;
  private AggregationType aggregationType;
  private Map<String, Object> aggregationParameters;
  private Map<String, QueryModel> aggregationsMap;

  // For source configuration
  private List<String> includeFields;
  private List<String> excludeFields;
  private Map<String, String> sortFields; // Key: Field name, Value: Sort order ("asc" or "desc")

  // Field for storing the script source (the actual script)
  private String scriptSource;

  // Field for storing the script language (e.g., "painless")
  private String scriptLanguage;

  // Field for storing the script parameters (if any)
  private Map<String, Object> scriptParams;

  /**
   * Constructor for initializing QueryModel from a JSON object.
   *
   * @param json the JsonObject representing the query.
   */
  public QueryModel(JsonObject json) {
    QueryModelConverter.fromJson(json, this);
  }

  /**
   * Constructor for creating a simple query (e.g., Match, Term).
   *
   * @param queryType the type of the query.
   * @param queryParameters the parameters required for the query (e.g., field, value).
   */
  public QueryModel(QueryType queryType, Map<String, Object> queryParameters) {
    this.queryType = queryType;
    this.queryParameters = queryParameters;
  }

  /**
   * Constructor for creating a QueryModel with boolean sub-queries using BoolOperator.
   *
   * @param boolOperator the boolean operator (MUST, SHOULD, MUST_NOT, FILTER).
   * @param subQueries the list of sub-queries for this boolean operation.
   */
  public QueryModel(BoolOperator boolOperator, List<QueryModel> subQueries) {
    this.boolOperator = boolOperator;
    switch (boolOperator) {
      case MUST:
        this.mustQueries = subQueries;
        break;
      case SHOULD:
        this.shouldQueries = subQueries;
        break;
      case MUST_NOT:
        this.mustNotQueries = subQueries;
        break;
      case FILTER:
        this.filterQueries = subQueries;
        break;
      default:
        throw new DxBadRequestException("Unsupported BoolOperator: " + boolOperator);
    }
    this.queryType = QueryType.BOOL; // Explicitly set the query type as BOOL for clarity.
  }

  /**
   * Constructor for creating a QueryModel with multiple types of boolean sub-queries.
   *
   * @param mustQueries List of queries to include in the MUST clause.
   * @param shouldQueries List of queries to include in the SHOULD clause.
   * @param mustNotQueries List of queries to include in the MUST_NOT clause.
   * @param filterQueries List of queries to include in the FILTER clause.
   */
  public QueryModel(
          List<QueryModel> mustQueries,
          List<QueryModel> shouldQueries,
          List<QueryModel> mustNotQueries,
          List<QueryModel> filterQueries) {
    this.queryType = QueryType.BOOL; // Set the type explicitly as BOOL
    this.mustQueries = mustQueries;
    this.shouldQueries = shouldQueries;
    this.mustNotQueries = mustNotQueries;
    this.filterQueries = filterQueries;
  }

  public QueryModel(AggregationType aggregationType, Map<String, Object> aggregationParameters) {
    this.aggregationType = aggregationType;
    this.aggregationParameters = aggregationParameters;
  }

  public QueryModel(QueryModel queries, List<QueryModel> aggregations) {
    this.queries = queries;
    this.aggregations = aggregations;
  }

  public QueryModel() {}

  public QueryModel(QueryType queryType) {
    this.queryType = queryType;
  }

  /**
   * Converts this model into a JsonObject.
   *
   * @return the JSON representation of this QueryModel object.
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    QueryModelConverter.toJson(this, json);
    return json;
  }

  public AggregationType getAggregationType() {
    return aggregationType;
  }

  public void setAggregationType(AggregationType aggregationType) {
    this.aggregationType = aggregationType;
  }

  public Map<String, Object> getAggregationParameters() {
    return aggregationParameters;
  }

  public void setAggregationParameters(Map<String, Object> aggregationParameters) {
    this.aggregationParameters = aggregationParameters;
  }

  public String getMinimumShouldMatch() {
    return minimumShouldMatch;
  }

  public void setMinimumShouldMatch(String minimumShouldMatch) {
    this.minimumShouldMatch = minimumShouldMatch;
  }

  public QueryModel getQueries() {
    return queries;
  }

  public void setQueries(QueryModel queries) {
    this.queries = queries;
  }

  public QueryType getQueryType() {
    return queryType;
  }

  public void setQueryType(QueryType queryType) {
    this.queryType = queryType;
  }

  public Map<String, Object> getQueryParameters() {
    return queryParameters;
  }

  public void setQueryParameters(Map<String, Object> queryParameters) {
    this.queryParameters = queryParameters;
  }

  public List<QueryModel> getSubQueries() {
    return subQueries;
  }

  public void setSubQueries(List<QueryModel> subQueries) {
    this.subQueries = subQueries;
  }

  public Map<String, QueryModel> getAggregationsMap() {
    return aggregationsMap;
  }

  public void setAggregationsMap(Map<String, QueryModel> aggregationsMap) {
    this.aggregationsMap = aggregationsMap;
  }

  public void addAggregationsMap(Map<String, QueryModel> aggregationsMap) {
    this.aggregationsMap.putAll(aggregationsMap);
  }

  public List<QueryModel> getAggregations() {
    return aggregations;
  }

  public void setAggregations(List<QueryModel> aggregations) {
    this.aggregations = aggregations;
  }

  public String getAggregationName() {
    return aggregationName;
  }

  public void setAggregationName(String aggregationName) {
    this.aggregationName = aggregationName;
  }

  public void addSubQuery(QueryModel subQuery) {
    if (this.subQueries == null) {
      this.subQueries = new ArrayList<>();
    }
    this.subQueries.add(subQuery);
  }

  public List<QueryModel> getMustQueries() {
    return mustQueries;
  }

  public void setMustQueries(List<QueryModel> mustQueries) {
    this.mustQueries = mustQueries;
  }

  public void addMustQuery(QueryModel mustQuery) {
    if (this.mustQueries == null) {
      this.mustQueries = new ArrayList<>(); // Initialize the list if null
    }
    this.mustQueries.add(mustQuery);
  }

  public void addAllMustQuery(List<QueryModel> mustQueries) {
    this.mustQueries.addAll(mustQueries);
  }

  public List<QueryModel> getShouldQueries() {
    return shouldQueries;
  }

  public void setShouldQueries(List<QueryModel> shouldQueries) {
    this.shouldQueries = shouldQueries;
  }

  public void addShouldQuery(QueryModel shouldQuery) {
    if (this.shouldQueries == null) {
      this.shouldQueries = new ArrayList<>(); // Initialize the list if null
    }
    this.shouldQueries.add(shouldQuery);
  }

  public List<QueryModel> getMustNotQueries() {
    return mustNotQueries;
  }

  public void setMustNotQueries(List<QueryModel> mustNotQueries) {
    this.mustNotQueries = mustNotQueries;
  }

  public void addMustNotQuery(QueryModel mustNotQuery) {
    if (this.mustNotQueries == null) {
      this.mustNotQueries = new ArrayList<>(); // Initialize the list if null
    }
    this.mustNotQueries.add(mustNotQuery);
  }

  public List<QueryModel> getFilterQueries() {
    return filterQueries;
  }

  public void setFilterQueries(List<QueryModel> filterQueries) {
    this.filterQueries = filterQueries;
  }

  public void addFilterQuery(QueryModel filterQuery) {
    if (this.filterQueries == null) {
      this.filterQueries = new ArrayList<>(); // Initialize the list if null
    }
    this.filterQueries.add(filterQuery);
  }

  public BoolOperator getBoolOperator() {
    return boolOperator;
  }

  public void setBoolOperator(BoolOperator boolOperator) {
    this.boolOperator = boolOperator;
  }

  public List<String> getIncludeFields() {
    return includeFields;
  }

  public void setIncludeFields(List<String> includeFields) {
    this.includeFields = includeFields;
  }

  public List<String> getExcludeFields() {
    return excludeFields;
  }

  public void setExcludeFields(List<String> excludeFields) {
    this.excludeFields = excludeFields;
  }

  public String getLimit() {
    return limit;
  }

  public void setLimit(String limit) {
    this.limit = limit;
  }

  public String getOffset() {
    return offset;
  }

  public void setOffset(String offset) {
    this.offset = offset;
  }

  public Map<String, String> getSortFields() {
    return sortFields;
  }

  public void setSortFields(Map<String, String> sortFields) {
    this.sortFields = sortFields;
  }
  public String getScriptSource() {
    return scriptSource;
  }

  public void setScriptSource(String scriptSource) {
    this.scriptSource = scriptSource;
  }

  public String getScriptLanguage() {
    return scriptLanguage;
  }

  public void setScriptLanguage(String scriptLanguage) {
    this.scriptLanguage = scriptLanguage;
  }

  public Map<String, Object> getScriptParams() {
    return scriptParams;
  }

  public void setScriptParams(Map<String, Object> scriptParams) {
    this.scriptParams = scriptParams;
  }

  /**
   * Converts this QueryModel into an Elasticsearch Query object.
   *
   * @return Elasticsearch Query object.
   * @throws UnsupportedOperationException if the query type is not supported.
   */
  public Query toElasticsearchQuery() {
    LOGGER.debug("Converting QueryModel to Elasticsearch Query");

    if (this.queryType == null) {
      LOGGER.error("Query type is null for QueryModel: {}", this.toJson());
      throw new IllegalArgumentException("Query type cannot be null");
    }

    try {
      switch (this.queryType) {
        case MATCH_ALL:
          return MatchAllQuery.of(m -> m)._toQuery();
        case MATCH:
          return MatchQuery.of(
                          m ->
                                  m.field((String) queryParameters.get(FIELD))
                                          .query(queryParameters.get(VALUE).toString()))
                  ._toQuery();
        case TERM:
          return TermQuery.of(
                          t ->
                                  t.field((String) queryParameters.get(FIELD))
                                          .value(queryParameters.get(VALUE).toString()))
                  ._toQuery();
        case TERMS:
          // Ensure the value is a List<String>
          List<String> termsValues;
          if (queryParameters.get(VALUE) instanceof JsonArray) {
            termsValues = ((JsonArray) queryParameters.get(VALUE)).getList();
          } else {
            termsValues = List.of(queryParameters.get(VALUE).toString());
          }
          // Convert the list to FieldValue and wrap in TermsQueryField
          TermsQueryField termsQueryField =
                  new TermsQueryField.Builder()
                          .value(termsValues.stream().map(FieldValue::of).collect(Collectors.toList()))
                          .build();

          return TermsQuery.of(
                          t -> t.field((String) queryParameters.get(FIELD)).terms(termsQueryField))
                  ._toQuery();
        case BOOL:
          return BoolQuery.of(
                          b -> {
                            if (this.mustQueries != null) {
                              b.must(
                                      this.mustQueries.stream()
                                              .map(QueryModel::toElasticsearchQuery)
                                              .collect(Collectors.toList()));
                            }
                            if (this.shouldQueries != null) {
                              b.should(
                                      this.shouldQueries.stream()
                                              .map(QueryModel::toElasticsearchQuery)
                                              .collect(Collectors.toList()));
                            }
                            if (this.minimumShouldMatch != null) {
                              b.minimumShouldMatch(this.minimumShouldMatch);
                            }
                            if (this.mustNotQueries != null) {
                              b.mustNot(
                                      this.mustNotQueries.stream()
                                              .map(QueryModel::toElasticsearchQuery)
                                              .collect(Collectors.toList()));
                            }
                            if (this.filterQueries != null) {
                              b.filter(
                                      this.filterQueries.stream()
                                              .map(QueryModel::toElasticsearchQuery)
                                              .collect(Collectors.toList()));
                            }
                            return b;
                          })
                  ._toQuery();
        case WILDCARD:
          return WildcardQuery.of(
                          w ->
                                  w.field((String) queryParameters.get(FIELD))
                                          .value((String) queryParameters.get(VALUE)))
                  ._toQuery();
        case GEO_BOUNDING_BOX:
          return GeoBoundingBoxQuery.of(g -> g
                  .field((String) queryParameters.get(FIELD))
                  .boundingBox(bb -> bb
                          .tlbr(tlbr -> tlbr
                                  .topLeft(GeoLocation.of(gl -> gl.latlon(LatLonGeoLocation.of(latLon -> latLon
                                          .lat((Double) queryParameters.get("top_left_lat"))
                                          .lon((Double) queryParameters.get("top_left_lon"))))))
                                  .bottomRight(
                                          GeoLocation.of(gl -> gl.latlon(LatLonGeoLocation.of(latLon -> latLon
                                                  .lat((Double) queryParameters.get("bottom_right_lat"))
                                                  .lon((Double) queryParameters.get("bottom_right_lon"))))))))
          )._toQuery();
        case GEO_SHAPE:
          JsonObject geoJson = new JsonObject();
          geoJson.put(TYPE, queryParameters.get(TYPE).toString());
          geoJson.put(COORDINATES, queryParameters.get(COORDINATES));
          if (Objects.equals(queryParameters.get(TYPE).toString(), GEO_CIRCLE)) {
            geoJson.put("radius", queryParameters.get("radius"));
          }
          String relation = queryParameters.get("relation").toString();
          String formattedRelation =
                  relation.substring(0, 1).toUpperCase() + relation.substring(1).toLowerCase();

          GeoShapeFieldQuery geoShapeFieldQuery =
                  new GeoShapeFieldQuery.Builder()
                          .shape(JsonData.fromJson(geoJson.toString()))
                          .relation(GeoShapeRelation.valueOf(formattedRelation))
                          .build();
          return QueryBuilders.geoShape(
                  g -> g.field((String) queryParameters.get(GEO_PROPERTY)).shape(geoShapeFieldQuery));
        case TEXT:
          return QueryStringQuery.of(qs -> qs.query(queryParameters.get(Q_VALUE).toString()))
                  ._toQuery();
        case SCRIPT_SCORE:
          // Add the script_score query here
          JsonArray queryVector = (JsonArray) queryParameters.get("query_vector");
          List<Double> vectorList = queryVector.getList();
          String vectorString = new JsonArray(vectorList).encode();
          // Create a map for script parameters
          Map<String, JsonData> params = new HashMap<>();
          params.put("query_vector", JsonData.fromJson(vectorString));
          // Use MatchAllQuery if custom query is not provided
          QueryModel customQueryModel = null;
          if (queryParameters.get("custom_query") != null) {
            customQueryModel =
                    new QueryModel(JsonObject.mapFrom(queryParameters.get("custom_query")));
          }
          Query baseQuery = (customQueryModel != null)
                  ? customQueryModel.toElasticsearchQuery()
                  : MatchAllQuery.of(m -> m)._toQuery();
          return ScriptScoreQuery.of(ssq -> ssq
                  .query(baseQuery)
                  .script(s -> s.source(
                                  "doc['_word_vector'].size() == 0 "
                                          + "? 0 : "
                                          + "cosineSimilarity(params.query_vector, '_word_vector') + 1.0")
                          .lang("painless")
                          .params(params))
          )._toQuery();
        case QUERY_STRING:
          return QueryStringQuery.of(
                          qs -> {
                            qs.query(queryParameters.get("query").toString());
                            if (queryParameters.containsKey("default_field")) {
                              qs.defaultField(queryParameters.get("default_field").toString());
                            }
                            if (queryParameters.containsKey("fields")) {
                              List<String> fields = (List<String>) queryParameters.get("fields");
                              qs.fields(fields);
                            }
                            if (queryParameters.containsKey("analyzer")) {
                              qs.analyzer(queryParameters.get("analyzer").toString());
                            }
                            if (queryParameters.containsKey("default_operator")) {
                              String operator =
                                      queryParameters.get("default_operator").toString().toUpperCase();
                              qs.defaultOperator(Operator.valueOf(operator));
                            }
                            return qs;
                          })
                  ._toQuery();
        case RANGE:
          return RangeQuery.of(
                  r -> r.date(DateRangeQuery.of(
                          dr -> {
                            DateRangeQuery.Builder builder = new DateRangeQuery.Builder()
                                    .field(queryParameters.get(FIELD).toString());

                            Map<String, Consumer<String>> rangeConditions = Map.of(
                                    GREATER_THAN_EQ_OP, builder::gte,
                                    LESS_THAN_EQ_OP, builder::lte,
                                    GREATER_THAN_OP, builder::gt,
                                    LESS_THAN_OP, builder::lt
                            );

                            rangeConditions.forEach((key, setter) -> {
                              if (queryParameters.containsKey(key)) {
                                setter.accept(queryParameters.get(key).toString());
                              }
                            });

                            return builder;
                          })
                  ))._toQuery();

        default:
          throw new DxBadRequestException("Query type not supported: " + this.queryType);
      }
    } catch (Exception e) {
      LOGGER.error("Error while creating Elasticsearch Query for QueryModel: {}", this.toJson(), e);
      throw new DxInternalServerErrorException("Failed to convert QueryModel to Elasticsearch Query", e);
    }
  }

  /**
   * Converts this QueryModel into an Elasticsearch Aggregation object.
   *
   * @return A map of aggregation names to Elasticsearch Aggregation objects.
   */
  public Aggregation toElasticsearchAggregations() {
    // Validate the aggregation type and parameters
    if (this.aggregationType == null || this.aggregationParameters == null) {
      LOGGER.error(
              "Aggregation type or parameters missing. Aggregation cannot be created for: "
                      + this.toJson());
      throw new DxBadRequestException("Invalid aggregation configuration");
    }
    return AggregationFactory.createAggregation(this);
  }

  /**
   * Converts this QueryModel's source configuration into Elasticsearch SourceConfig object.
   *
   * @return Elasticsearch SourceConfig object.
   */
  public SourceConfig toSourceConfig() {
    if (includeFields != null && excludeFields != null) {
      return SourceConfig.of(s -> s.filter(f -> f.includes(includeFields).excludes(excludeFields)));
    } else if (includeFields != null) {
      return SourceConfig.of(s -> s.filter(f -> f.includes(includeFields)));
    } else if (excludeFields != null) {
      return SourceConfig.of(s -> s.filter(f -> f.excludes(excludeFields)));
    }
    return null; // Returns null if there are no field inclusion/exclusion rules
  }

  /**
   * Converts this QueryModel's sorting configuration into a list of Elasticsearch SortOptions
   * objects.
   *
   * @return List of Elasticsearch SortOptions objects.
   */
  public List<SortOptions> toSortOptions() {
    if (sortFields == null || sortFields.isEmpty()) {
      return null; // Returns null if there are no sorting rules
    }

    return sortFields.entrySet().stream()
            .map(entry ->
                    SortOptions.of(s ->
                            s.field(f ->
                                    f.field(entry.getKey())
                                            .order("asc".equalsIgnoreCase(entry.getValue())
                                                    ? SortOrder.Asc : SortOrder.Desc))))
            .collect(Collectors.toList());
  }

  /**
   * Converts this QueryModel's script configuration into an Elasticsearch Script object.
   *
   * @return Elasticsearch Script object, or null if no script configuration is available.
   */
  public Script toElasticsearchScript() {
    // Check if the QueryModel contains the necessary script-related properties
    if (scriptSource != null && !scriptSource.isEmpty()) {
      Script.Builder scriptBuilder = new Script.Builder();

      // Set the script source (e.g., Painless script code)
      scriptBuilder.source(scriptSource);

      // Set the language for the script (e.g., "painless")
      if (scriptLanguage != null && !scriptLanguage.isEmpty()) {
        scriptBuilder.lang(scriptLanguage);
      } else {
        scriptBuilder.lang("painless"); // Default to "painless" if no language is specified
      }
      Map<String, JsonData> params = scriptParams.entrySet().stream()
              .collect(Collectors.toMap(Map.Entry::getKey, e -> JsonData.of(e.getValue())));
      scriptBuilder.params(params);

      return scriptBuilder.build();
    }
    // Return null if there's no script source provided in the QueryModel
    return null;
  }

}