package org.cdpg.dx.databroker.util;

import static iudx.resource.server.databroker.util.Constants.*;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Util {
  private static final Logger LOGGER = LogManager.getLogger(Util.class);
  public static Supplier<String> randomPassword =
      () -> {
        UUID uid = UUID.randomUUID();
        byte[] pwdBytes =
            ByteBuffer.wrap(new byte[16])
                .putLong(uid.getMostSignificantBits())
                .putLong(uid.getLeastSignificantBits())
                .array();
        return Base64.getUrlEncoder().encodeToString(pwdBytes).substring(0, 22);
      };
  public static BinaryOperator<JsonArray> bindingMergeOperator =
      (key1, key2) -> {
        JsonArray mergedArray = new JsonArray();
        mergedArray.clear().addAll(key1).addAll(key2);
        return mergedArray;
      };

  public static String encodeValue(String value) {
    return (value == null) ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
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

  public static JsonObject getResponseJson(String type, String title, String detail) {
    JsonObject json = new JsonObject();
    json.put(TYPE, type);
    json.put(TITLE, title);
    json.put(DETAIL, detail);
    return json;
  }
}
