package org.cdpg.dx.databroker.util;

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

  public static String encodeValue(String value) {
    return (value == null) ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
