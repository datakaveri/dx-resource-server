package iudx.resource.server.database.latest;

import static iudx.resource.server.database.archives.Constants.DEFAULT_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.junit5.VertxExtension;

@ExtendWith({VertxExtension.class})
public class RedisCommandArgsBuilderTest {

  static RedisCommandArgsBuilder redisCmdArgsBuilder;

  @BeforeAll
  static void init() {
    redisCmdArgsBuilder = new RedisCommandArgsBuilder();
  }


  @Test
  public void resourceLevelArgs() {
    String id = "asdas/asdasdas/adsasdasd/asdasda/asdasdas";
    String tenantPrefix = "iudx";
    RedisArgs redisArgs = redisCmdArgsBuilder.getRedisCommandArgs(id, true, tenantPrefix);

    String idKey = id;
    String namespace = tenantPrefix.concat(":");
    String key = namespace.concat(idKey);
    assertEquals(key, redisArgs.getKey());
    assertEquals(".", redisArgs.getPath());
  }

  @Test
  public void groupLevelArgs() {
    String id = "asdas/asdasdas/adsasdasd/asdasda";
    String tenantPrefix = "iudx";
    RedisArgs redisArgs = redisCmdArgsBuilder.getRedisCommandArgs(id, false, tenantPrefix);

    String idKey = id;
    String namespace = tenantPrefix.concat(":");
    String key = namespace.concat(idKey);
    StringBuilder shaId = new StringBuilder(id).append("/").append(DEFAULT_ATTRIBUTE);
    String sha = DigestUtils.sha1Hex(shaId.toString());

    assertEquals(key, redisArgs.getKey());
    assertEquals("._" + sha, redisArgs.getPath());
  }

}
