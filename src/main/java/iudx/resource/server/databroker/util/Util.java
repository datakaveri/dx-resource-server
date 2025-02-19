package iudx.resource.server.databroker.util;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static iudx.resource.server.databroker.util.Constants.*;


public class Util {
    private static final Logger LOGGER = LogManager.getLogger(Util.class);
    public static String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
    public static JsonObject getResponseJson(
            String type, int statusCode, String title, String detail) {
        JsonObject json = new JsonObject();
        json.put(TYPE, type);
        json.put(STATUS, statusCode);
        json.put(TITLE, title);
        json.put(DETAIL, detail);
        return json;
    }
    public static JsonObject getResponseJson(int type, String title, String detail) {
        JsonObject json = new JsonObject();
        json.put(TYPE, type);
        json.put(TITLE, title);
        json.put(DETAIL, detail);
        return json;
    }

    public static JsonObject getResponseJson(String type, String title, String detail){
        JsonObject json = new JsonObject();
        json.put(TYPE, type);
        json.put(TITLE, title);
        json.put(DETAIL, detail);
        return json;
    }
}
