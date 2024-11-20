package iudx.resource.server.database.redis.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DataObject(generateConverter = true)
public class ResponseModel {
    private static final Logger LOGGER = LogManager.getLogger(ResponseModel.class);

    private Response response;
    private List<String> listOfKeys;
    private Map<String, Object> keyValueMap = new HashMap<>();

    // Constructor to create from JsonObject
    public ResponseModel(JsonObject request) {
        fromJson(request);
    }

    // Constructor to create from Response and List of Keys
    public ResponseModel(Response response, List<String> listOfKeys) {
        if (response == null || listOfKeys == null) {
            LOGGER.error("Response or List of Keys cannot be null");
            return;
        }
        this.response = response;
        this.listOfKeys = listOfKeys;
        createResponseMap();
    }

    // Custom method to convert ResponseModel to JsonObject
    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        // Convert listOfKeys to JsonArray
        if (listOfKeys != null) {
            json.put("listOfKeys", new JsonArray(listOfKeys));
        }

        // Convert keyValueMap to JsonObject
        if (keyValueMap != null) {
            JsonObject keyValueJson = new JsonObject();
            keyValueMap.forEach(keyValueJson::put);
            json.put("keyValueMap", keyValueJson);
        }

        return json;
    }

    // Custom method to create ResponseModel from JsonObject
    public void fromJson(JsonObject json) {
        // Convert listOfKeys from JsonArray
        if (json.containsKey("listOfKeys")) {
            listOfKeys = json.getJsonArray("listOfKeys").getList();
        }

        // Convert keyValueMap from JsonObject
        if (json.containsKey("keyValueMap")) {
            JsonObject keyValueJson = json.getJsonObject("keyValueMap");
            keyValueJson.forEach(entry -> keyValueMap.put(entry.getKey(), entry.getValue()));
        }
    }

    // Private method to map keys to response values
    private void createResponseMap() {
        if (response != null && listOfKeys != null) {
            for (int keyIndex = 0; keyIndex < listOfKeys.size(); keyIndex++) {
                if (keyIndex < response.size()) {
                    keyValueMap.put(listOfKeys.get(keyIndex), response.get(keyIndex).toString());
                } else {
                    LOGGER.warn("Response index out of bounds for key: " + listOfKeys.get(keyIndex));
                }
            }
        } else {
            LOGGER.error("Response or List of Keys is null when trying to createResponseMap");
        }
    }

    // Method to get a value from the key-value map
    public Object getValueFromKey(String key) {
        if (keyValueMap.containsKey(key)) {
            return keyValueMap.get(key);
        } else {
            LOGGER.warn("Key not found in keyValueMap: " + key);
            return null;
        }
    }
}
