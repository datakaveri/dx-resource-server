package org.cdpg.dx.rs.latest.model;

import io.vertx.core.json.JsonArray;

public class LatestData {


    JsonArray jsonArray;

    public LatestData(JsonArray jsonArray){
        this.jsonArray=jsonArray;
    }

    public JsonArray getLatestData() {
        return jsonArray;
    }

}
