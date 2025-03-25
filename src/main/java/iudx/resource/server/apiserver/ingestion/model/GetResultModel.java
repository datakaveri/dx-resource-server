package iudx.resource.server.apiserver.ingestion.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.ResponseUrn;
import org.cdpg.dx.databroker.model.ExchangeSubscribersResponse;


public record GetResultModel(ExchangeSubscribersResponse exchangeSubscribersResponse) {
  public JsonObject constructSuccessResponse() {
    JsonObject iudxResponse = new JsonObject();
    iudxResponse.put("type", "urn:dx:rs:success");
    iudxResponse.put("title", "Success");
    JsonArray resultArray = new JsonArray();
    Object result = exchangeSubscribersResponse;
    if (result instanceof ExchangeSubscribersResponse) {
      JsonObject formattedResult = ((ExchangeSubscribersResponse) result).toJson();
      resultArray.add(formattedResult);
    } else if (result instanceof JsonObject) {
      resultArray.add(result);
    }
    iudxResponse.put("results", resultArray);
    return new JsonObject()
        .put("type", ResponseUrn.SUCCESS_URN.getUrn())
        .put("title", ResponseUrn.SUCCESS_URN.getMessage())
        .put("results", resultArray);
  }
}
