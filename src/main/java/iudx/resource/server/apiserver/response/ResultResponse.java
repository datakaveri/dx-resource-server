package iudx.resource.server.apiserver.response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.util.Constants;

import static iudx.resource.server.apiserver.util.Constants.*;

public class ResultResponse {
    private String type;
    private String title;
    private String detail;
    private JsonArray results;

    public ResultResponse(String type, String title, String detail, JsonArray results) {
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.results = results;
    }

    public ResultResponse(String type, String title, String detail) {
        this.type = type;
        this.title = title;
        this.detail = detail;
    }

    public ResultResponse(String type, String title,JsonArray results) {
        this.type = type;
        this.title = title;
        this.results = results;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public JsonArray getResults() {
        return results;
    }

    public void setResults(JsonArray results) {
        this.results = results;
    }

    public JsonObject toJsonWithDetails() {
        JsonObject json = new JsonObject();
        json.put(JSON_TYPE, this.type);
        json.put(JSON_TITLE, this.title);
        json.put(JSON_DETAIL, this.detail);
        return json;
    }

    public JsonObject toJsonWithResult() {
        JsonObject json = new JsonObject();
        json.put(JSON_TYPE, this.type);
        json.put(JSON_TITLE, this.title);
        json.put("results", new JsonArray().add(this.results));
        return json;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put(JSON_TYPE, this.type);
        json.put(JSON_TITLE, this.title);
        json.put(JSON_DETAIL, this.detail);
        json.put("results", new JsonArray().add(this.results));
        return json;
    }
    @Override
    public String toString() {
        return "ResultResponse{" +
                "type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", detail='" + detail + '\'' +
                ", results=" + results +
                '}';
    }
}
