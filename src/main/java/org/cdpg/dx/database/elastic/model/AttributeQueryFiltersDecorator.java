package org.cdpg.dx.database.elastic.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.common.ResponseUrn;
import org.cdpg.dx.database.elastic.exception.EsQueryException;
import org.cdpg.dx.database.elastic.util.QueryType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cdpg.dx.database.elastic.util.Constants.*;

public class AttributeQueryFiltersDecorator implements ElasticsearchQueryDecorator {

    private Map<QueryType, List<QueryModel>> queryFilters;
    private JsonObject requestQuery;

    public AttributeQueryFiltersDecorator(
            Map<QueryType, List<QueryModel>> queryFilters, JsonObject requestQuery) {
        this.queryFilters = queryFilters;
        this.requestQuery = requestQuery;
    }

    @Override
    public Map<QueryType, List<QueryModel>> add() {
        JsonArray attrQuery;

        if (!requestQuery.containsKey(ATTRIBUTE_QUERY_KEY)) {
            return queryFilters;
        }

        attrQuery = requestQuery.getJsonArray(ATTRIBUTE_QUERY_KEY);
        for (Object obj : attrQuery) {
            JsonObject attrObj = (JsonObject) obj;
            QueryModel attrRangeQuery = new QueryModel(QueryType.RANGE);
            Map<String, Object> attrRangeParams = new HashMap<String, Object>();
            try {
                String attribute = attrObj.getString(ATTRIBUTE_KEY);
                String operator = attrObj.getString(OPERATOR);
                String attributeValue = attrObj.getString(VALUE);

                attrRangeParams.put(FIELD, attribute);

                if (GREATER_THAN_OP.equalsIgnoreCase(operator)) {

                    attrRangeParams.put(GREATER_THAN_OP, attributeValue);

                    List<QueryModel> queryList = queryFilters.get(QueryType.FILTER);
                    attrRangeQuery.setQueryParameters(attrRangeParams);
                    queryList.add(attrRangeQuery);

                } else if (LESS_THAN_OP.equalsIgnoreCase(operator)) {

                    attrRangeParams.put(LESS_THAN_OP, attributeValue);

                    List<QueryModel> queryList = queryFilters.get(QueryType.FILTER);
                    attrRangeQuery.setQueryParameters(attrRangeParams);
                    queryList.add(attrRangeQuery);

                } else if (GREATER_THAN_EQ_OP.equalsIgnoreCase(operator)) {

                    attrRangeParams.put(GREATER_THAN_EQ_OP, attributeValue);

                    List<QueryModel> queryList = queryFilters.get(QueryType.FILTER);
                    attrRangeQuery.setQueryParameters(attrRangeParams);
                    queryList.add(attrRangeQuery);

                } else if (LESS_THAN_EQ_OP.equalsIgnoreCase(operator)) {

                    attrRangeParams.put(LESS_THAN_EQ_OP, attributeValue);

                    List<QueryModel> queryList = queryFilters.get(QueryType.FILTER);
                    attrRangeQuery.setQueryParameters(attrRangeParams);
                    queryList.add(attrRangeQuery);

                } else if (EQUAL_OP.equalsIgnoreCase(operator)) {

                    QueryModel termQuery = new QueryModel(QueryType.TERM);
                    Map<String, Object> termParams = new HashMap<>();
                    termParams.put(FIELD, attribute);
                    termParams.put(VALUE, attributeValue);

                    List<QueryModel> queryList = queryFilters.get(QueryType.FILTER);
                    termQuery.setQueryParameters(termParams);
                    queryList.add(termQuery);

                } else if (BETWEEN_OP.equalsIgnoreCase(operator)) {
                    String gteField = attrObj.getString(VALUE_LOWER);
                    String lteField = attrObj.getString(VALUE_UPPER);

                    attrRangeParams.put(GREATER_THAN_EQ_OP, gteField);
                    attrRangeParams.put(LESS_THAN_EQ_OP, lteField);

                    List<QueryModel> queryList = queryFilters.get(QueryType.FILTER);
                    attrRangeQuery.setQueryParameters(attrRangeParams);
                    queryList.add(attrRangeQuery);

                } else if (NOT_EQUAL_OP.equalsIgnoreCase(operator)) {

                    QueryModel termQuery = new QueryModel(QueryType.TERM);
                    Map<String, Object> termParams = new HashMap<>();
                    termParams.put(FIELD, attribute);
                    termParams.put(VALUE, attributeValue);

                    List<QueryModel> queryList = queryFilters.get(QueryType.MUST_NOT);
                    termQuery.setQueryParameters(termParams);
                    queryList.add(termQuery);

                } else {
                    throw new EsQueryException(
                            ResponseUrn.INVALID_ATTR_PARAM_URN, "invalid attribute operator");
                }
            } catch (EsQueryException e) {
                throw e;
            } catch (Exception e) {
                throw new EsQueryException(
                        ResponseUrn.INVALID_ATTR_PARAM_URN, "exception occured at decoding attributes");
            }
        }
        return queryFilters;
    }
}