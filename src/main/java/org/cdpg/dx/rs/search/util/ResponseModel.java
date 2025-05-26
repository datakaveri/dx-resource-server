package org.cdpg.dx.rs.search.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.database.elastic.model.ElasticsearchResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.cdpg.dx.database.elastic.util.Constants.TOTAL_HITS;

public class ResponseModel {
    JsonObject response;
    private List<JsonObject> elasticsearchResponses;
    private Integer count;
    private int limit;
    private int offset;
    public ResponseModel(List<ElasticsearchResponse> elasticsearchResponses,int limit,int offset) {
        this.response=new JsonObject();
        this.response.put("results",elasticsearchResponses);
        this.limit=limit;
        this.offset=offset;
        this.elasticsearchResponses = getJsonObjectList(Objects.requireNonNullElse(elasticsearchResponses, List.of()));
        this.count = -1; // Indicates it's a search and not a count request
        setResponseJson();
    }

    private List<JsonObject> getJsonObjectList(List<ElasticsearchResponse> elasticsearchResponses) {
        List<JsonObject> jsonObjectList = new ArrayList<>();
        for (ElasticsearchResponse elasticsearchResponse : elasticsearchResponses) {
            jsonObjectList.add(elasticsearchResponse.getSource());
        }
        response.put("offset", offset);
        response.put("limit", limit);
        response.put("totalHits", elasticsearchResponses.size());

        return jsonObjectList;
    }

    public ResponseModel(Integer count) {
        this.elasticsearchResponses = new ArrayList<>(); // Ensure it's mutable
        this.count = count;
        setResponseJson();
    }

    public JsonObject getResponse() {
        return count == -1 ? response : response.put("results", new JsonArray().add(new JsonObject().put("totalHits", count)));
    }

    public List<JsonObject> getElasticsearchResponses() {
        return elasticsearchResponses;
    }

    public Integer getCount() {
        return count;
    }

    public void setResponseJson(){
        if(count!=-1){
            response=new JsonObject().put("totalHits",count);
        }
        else response = new JsonObject().put("results",elasticsearchResponses).put("limit",limit).put("offset",offset);
    }

    public void setFromParam(int from) {
        response.put("offset", from);
    }

    public void setSizeParam(int size) {
        response.put("limit", size);
    }
    @Override
    public String toString() {
        return "ResponseModel{" +
                "elasticsearchResponses=" + elasticsearchResponses +
                ", count=" + count +
                '}';
    }
}
