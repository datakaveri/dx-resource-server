package org.cdpg.dx.rs.search.util;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.database.elastic.model.ElasticsearchResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResponseModel {
    private List<JsonObject> elasticsearchResponses;
    private Integer count;

    public ResponseModel(List<ElasticsearchResponse> elasticsearchResponses) {
        this.elasticsearchResponses = getJsonObjectList(Objects.requireNonNullElse(elasticsearchResponses, List.of()));
        this.count = -1; // Indicates it's a search and not a count request
    }

    private List<JsonObject> getJsonObjectList(List<ElasticsearchResponse> elasticsearchResponses) {
        List<JsonObject> jsonObjectList = new ArrayList<>();
        for (ElasticsearchResponse elasticsearchResponse : elasticsearchResponses) {
            jsonObjectList.add(elasticsearchResponse.getSource());
        }
        return jsonObjectList;
    }

    public ResponseModel(Integer count) {
        this.elasticsearchResponses = new ArrayList<>(); // Ensure it's mutable
        this.count = count;
    }

    public Object getResponse() {
        return count == -1 ? elasticsearchResponses : count;
    }

    public List<JsonObject> getElasticsearchResponses() {
        return elasticsearchResponses;
    }

    public Integer getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "ResponseModel{" +
                "elasticsearchResponses=" + elasticsearchResponses +
                ", count=" + count +
                '}';
    }
}
