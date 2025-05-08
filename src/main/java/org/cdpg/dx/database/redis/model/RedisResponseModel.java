package org.cdpg.dx.database.redis.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * ResponseModel - A wrapper class for handling Redis responses in a structured format.
 */
@DataObject(generateConverter = true)
public class RedisResponseModel {
    private static final Logger LOGGER = LogManager.getLogger(RedisResponseModel.class);

    private JsonObject json;

    /**
     * Build from a raw JSON.GET Response.
     */
    public RedisResponseModel(Response response) {
        if (response == null) {
            LOGGER.warn("RedisResponseModel received null Response");
            this.json = new JsonObject();
        } else {
            try {
                // Extract the bulk string payload
                String payload = response.toBuffer().toString();
                if (payload == null || payload.isEmpty()) {
                    LOGGER.warn("RedisResponseModel received empty payload from Redis for response: {}", response);
                    this.json = new JsonObject();
                } else {
                    this.json = new JsonObject(payload);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse Redis JSON.GET response [{}]", response, e);
                this.json = new JsonObject();
            }
        }
    }

    /**
     * Build directly from a JsonObject (e.g. for codegen or tests).
     */
    public RedisResponseModel(JsonObject json) {
        this.json = (json != null ? json.copy() : new JsonObject());
    }

    /**
     * @return the parsed JSON (never null)
     */
    public JsonObject toJson() {
        return json;
    }


}
