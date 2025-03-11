package iudx.resource.server.apiserver.auditing.model;

import io.vertx.core.json.JsonObject;

public class MonthlyOverviewResponse {
  private String month;
  private String year;
  private long count;

  // Default constructor
  public MonthlyOverviewResponse() {}

  // Constructor from JsonObject
  public MonthlyOverviewResponse(JsonObject json) {
    this.month = json.getString("month");
    this.year = json.getString("year");
    this.count = json.getLong("counts", 0L);
  }

  // Copy constructor
  public MonthlyOverviewResponse(MonthlyOverviewResponse other) {
    this.month = other.month;
    this.year = other.year;
    this.count = other.count;
  }

  // Convert object to JsonObject
  public JsonObject toJson() {
    return new JsonObject()
            .put("month", month)
            .put("year", year)
            .put("count", count);
  }

  // Getters and Setters
  public String getMonth() { return month; }
  public void setMonth(String month) { this.month = month; }

  public String getYear() { return year; }
  public void setYear(String year) { this.year = year; }

  public long getCount() { return count; }
  public void setCount(long count) { this.count = count; }
}
