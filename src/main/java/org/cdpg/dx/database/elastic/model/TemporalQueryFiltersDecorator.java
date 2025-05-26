package org.cdpg.dx.database.elastic.model;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.cdpg.dx.database.elastic.util.QueryType;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cdpg.dx.database.elastic.util.Constants.*;

public class TemporalQueryFiltersDecorator implements ElasticsearchQueryDecorator {
    private final int defaultDateLimit;
    private Map<QueryType, List<QueryModel>> queryFilters;
    private JsonObject requestQuery;

    public TemporalQueryFiltersDecorator(
            Map<QueryType, List<QueryModel>> queryFilters, JsonObject requestQuery, int defaultDateLimit) {
        this.queryFilters = queryFilters;
        this.requestQuery = requestQuery;
        this.defaultDateLimit = defaultDateLimit;
    }

    @Override
    public Map<QueryType, List<QueryModel>> add() {
        String queryRequestTimeRelation = requestQuery.getString(REQ_TIMEREL);
        String queryRequestStartTime = requestQuery.getString(TIME_KEY);
        String queryRequestEndTime = requestQuery.getString(END_TIME);

        ZonedDateTime startDateTime = getZonedDateTime(queryRequestStartTime);
        ZonedDateTime endDateTime =
                (queryRequestEndTime != null) ? getZonedDateTime(queryRequestEndTime) : null;

        if (DURING.equalsIgnoreCase(queryRequestTimeRelation)
                || BETWEEN.equalsIgnoreCase(queryRequestTimeRelation)) {
            validateTemporalPeriod(startDateTime, endDateTime);
        } else if (BEFORE.equalsIgnoreCase(queryRequestTimeRelation)) {
            queryRequestStartTime = startDateTime.minusDays(defaultDateLimit).toString();
            queryRequestEndTime = startDateTime.toString();
        } else if (AFTER.equalsIgnoreCase(queryRequestTimeRelation)) {
            queryRequestStartTime = startDateTime.toString();
            queryRequestEndTime = getEndDateForAfterQuery(startDateTime);
        } else {
            throw new DxBadRequestException("exception while parsing date/time");
        }

        final String startTime = queryRequestStartTime;
        final String endTime = queryRequestEndTime;

        QueryModel temporalQuery = new QueryModel(QueryType.RANGE);
        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put(FIELD, "observationDateTime");
        queryParams.put(LESS_THAN_EQ_OP, endTime);
        queryParams.put(GREATER_THAN_EQ_OP, startTime);
        temporalQuery.setQueryParameters(queryParams);

        List<QueryModel> queryList = queryFilters.get(QueryType.FILTER);
        queryList.add(temporalQuery);
        return queryFilters;
    }

    public void addDefaultTemporalFilters(Map<QueryType, List<QueryModel>> queryLists,
                                          JsonObject query) {
        String[] timeLimitConfig = query.getString(TIME_LIMIT).split(",");
        String deploymentType = timeLimitConfig[0];
        String dateToUseForDevDeployment = timeLimitConfig[1];
        if (PROD_INSTANCE.equalsIgnoreCase(deploymentType)) {
            addDefaultForProduction(queryLists);
        } else if (TEST_INSTANCE.equalsIgnoreCase(deploymentType)) {
            addDefaultForDev(queryLists, dateToUseForDevDeployment);
        } else {
            throw new DxBadRequestException("invalid timeLimit config passed");
        }
    }

    private void addDefaultForDev(
            Map<QueryType, List<QueryModel>> queryLists, String dateToUseForDevDeployment) {
        ZonedDateTime endTime = getZonedDateTime(dateToUseForDevDeployment);
        ZonedDateTime startTime = endTime.minusDays(defaultDateLimit);
        // LOGGER.debug("startTim :{}, endTime : {} [default days :
        // {}]",startTime,endTime,defaultDateLimit);
        QueryModel temporalQuery = new QueryModel(QueryType.RANGE);
        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put(FIELD, "observationDateTime");
        queryParams.put(LESS_THAN_EQ_OP, endTime.toString());
        queryParams.put(GREATER_THAN_EQ_OP, startTime.toString());
        temporalQuery.setQueryParameters(queryParams);

        List<QueryModel> queryList = queryLists.get(QueryType.FILTER);
        queryList.add(temporalQuery);
    }

    private void addDefaultForProduction(Map<QueryType, List<QueryModel>> queryLists) {
        OffsetDateTime currentDateTime = OffsetDateTime.now().minusDays(defaultDateLimit);
        QueryModel temporalQuery = new QueryModel(QueryType.RANGE);
        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put(FIELD, "observationDateTime");
        queryParams.put(GREATER_THAN_EQ_OP, currentDateTime.toString());
        temporalQuery.setQueryParameters(queryParams);

        List<QueryModel> queryList = queryLists.get(QueryType.FILTER);
        queryList.add(temporalQuery);
    }

    private String getEndDateForAfterQuery(ZonedDateTime startDateTime) {
        ZonedDateTime endDateTime;
        endDateTime = startDateTime.plusDays(defaultDateLimit);
        ZonedDateTime now = ZonedDateTime.now();
        long difference = endDateTime.compareTo(now);
        if (difference > 0) {
            return now.toString();
        } else {
            return endDateTime.toString();
        }
    }

    private void validateTemporalPeriod(ZonedDateTime startDateTime, ZonedDateTime endDateTime) {
        if (endDateTime == null) {
            throw new DxBadRequestException("No endDate[required mandatory field] provided for query");
        }

        if (startDateTime.isAfter(endDateTime)) {
            throw new DxBadRequestException("end date is before start date");
        }
    }

    private ZonedDateTime getZonedDateTime(String time) {
        try {
            return ZonedDateTime.parse(time);
        } catch (DateTimeParseException e) {
            throw new DxBadRequestException("exception while parsing date/time");
        }
    }
}