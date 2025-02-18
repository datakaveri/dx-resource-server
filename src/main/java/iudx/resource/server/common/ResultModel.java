package iudx.resource.server.common;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;

public class ResultModel {
    private static final Logger LOGGER = LogManager.getLogger(ResultModel.class);
    String type;

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public int getStatusCode() {
        return statusCode;
    }

    String title;
    String detail;
    int statusCode;
    JsonObject resultJson;
    HttpServerResponse httpServerResponse;

   public ResultModel(String failureMessage, HttpServerResponse response){
        try{
            this.httpServerResponse = response;
            resultJson =  new JsonObject(failureMessage);
            this.statusCode = resultJson.getInteger("status");
            this.type =  resultJson.getString("type");
             this.title = resultJson.getString("title");
             this.detail = resultJson.getString("detail");
        }catch(DecodeException de){
            LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
            response
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(400)
                    .end(ResponseUrn.BACKING_SERVICE_FORMAT_URN.getUrn());
        }
    }

    public String getResponse(){
       return httpServerResponse.putHeader(CONTENT_TYPE, APPLICATION_JSON)
               .setStatusCode(statusCode)
               .end(toJson().toString()).toString();
    }

    public JsonObject toJson(){
       return new JsonObject().put("type", type).put("title", title).put("detail",detail);
    }

}
