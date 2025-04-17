package org.cdpg.dx.rs.search.util.dtoUtil;
import io.vertx.core.json.JsonObject;


public class TemporalQ {
    private String timerel;
    private String time;
    private String endtime;
    private String timeProperty;

    public TemporalQ() {}

    public TemporalQ(JsonObject temporalQJson) {
        this.timerel = temporalQJson.getString("timerel");
        this.time = temporalQJson.getString("time");
        this.endtime = temporalQJson.getString("endtime");
        this.timeProperty = temporalQJson.getString("timeProperty");
    }

    // Getters and setters
    public String getTimerel() {
        return timerel;
    }

    public void setTimerel(String timerel) {
        this.timerel = timerel;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getEndtime() {
        return endtime;
    }

    public void setEndtime(String endtime) {
        this.endtime = endtime;
    }

    public String getTimeProperty() {
        return timeProperty;
    }

    public void setTimeProperty(String timeProperty) {
        this.timeProperty = timeProperty;
    }

    /**
     * Serializes this TemporalQ instance into a JsonObject.
     *
     * @return a JsonObject representing this TemporalQ
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("timerel", timerel)
                .put("time", time)
                .put("endtime", endtime)
                .put("timeProperty", timeProperty);
        return json;
    }

}