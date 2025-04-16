package org.cdpg.dx.database.postgres.models;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;
import java.util.List;

@DataObject
@JsonGen(publicConverter = false)
public class SimpleCondition implements ConditionComponent{
  private String condition;
  private List<Object> queryParams;
    public SimpleCondition(JsonObject jsonObject){
      /* Converts JsonObject to User class object or dataObject conversion [Deserialization] */
      SimpleConditionConverter.fromJson(jsonObject, this);
    }
  public SimpleCondition(String condition, List<Object> queryParams){
      super();
      this.condition = condition;
      this.queryParams = queryParams;
  }

  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  public void setQueryParams(List<Object> queryParams) {
    this.queryParams = queryParams;
  }

  /**
   * Converts Data object or User class object to json object [Serialization]
   *
   * @return JsonObject
   */
  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    SimpleConditionConverter.toJson(this, jsonObject);
    return jsonObject;
  }
  @Override
  public String toSQL() {
    return condition; // Example: "id = ?"
  }


  @Override
  public List<Object> getQueryParams() {
    return queryParams;
  }

}
