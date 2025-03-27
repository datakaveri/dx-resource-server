package org.cdpg.dx.database.redis.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.ResponseType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ResponseModel - A wrapper class for handling Redis responses in a structured format.
 */
@DataObject(generateConverter = true)
public class RedisResponseModel {
    private static final Logger LOGGER = LogManager.getLogger(RedisResponseModel.class);
    private JsonObject responseJson;

    public RedisResponseModel() {
        this.responseJson = new JsonObject();
    }

    public RedisResponseModel(JsonObject json) {
        ResponseModelConverter.fromJson(json, this);
    }

    public RedisResponseModel(Response response) {
        this.responseJson = parseResponse(response);
    }

    public JsonObject getResponseJson() {
        return this.responseJson;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ResponseModelConverter.toJson(this, json);
        return json;
    }

    /**
     * Parses the Redis Response object into a structured JsonObject.
     *
     * @param response The Response object received from Redis
     * @return JsonObject representation of the response
     */
    private JsonObject parseResponse(Response response) {
        JsonObject json = new JsonObject();
        if (response == null) {
            return json;
        }

        try {
            ResponseType type = response.type();
            json.put("type", type.name());

            switch (type) {
                case SIMPLE:
                case BULK:
                    json.put("value", response.toString());
                    break;
                case ERROR:
                    json.put("error", response.toString());
                    break;
                default:
                    json.put("unknown", response.toString());
            }

        } catch (Exception e) {
            LOGGER.error("Error parsing Redis response: {}", e.getMessage(), e);
            json.put("error", "Failed to parse response");
        }

        return json;
    }
}
