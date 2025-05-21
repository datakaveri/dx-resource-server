package org.cdpg.dx.rs.latest.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.database.redis.service.RedisService;
import org.cdpg.dx.database.redis.util.RedisArgs;
import org.cdpg.dx.rs.latest.model.LatestData;
import org.cdpg.dx.rs.latest.util.LatestRedisCommandArgsBuilder;
import org.cdpg.dx.uniqueattribute.service.UniqueAttributeService;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default implementation of LatestService that retrieves the latest snapshot or single record
 * for a given resource ID from Redis, considering unique attribute grouping if applicable.
 */
public class LatestServiceImpl implements LatestService {
    private static final Logger LOGGER = LogManager.getLogger(LatestServiceImpl.class);
    private final RedisService redisService;
    private final LatestRedisCommandArgsBuilder argsBuilder;
    private final UniqueAttributeService uniqueAttrService;
    private final String tenantPrefix;

    public LatestServiceImpl(
            RedisService redisService,
            String tenantPrefix,
            UniqueAttributeService uniqueAttrService
    ) {
        this.redisService = Objects.requireNonNull(redisService, "redisService must not be null");
        this.tenantPrefix = Objects.requireNonNull(tenantPrefix, "tenantPrefix must not be null");
        this.uniqueAttrService = Objects.requireNonNull(uniqueAttrService, "uniqueAttrService must not be null");
        this.argsBuilder = new LatestRedisCommandArgsBuilder();
    }

  @Override
  public Future<LatestData> getLatestData(String rsId) {
    Objects.requireNonNull(rsId, "Resource ID must not be null");

    // Determine if this resource uses unique attribute grouping
    return isUniqueAttribute(rsId)
        .recover(
            err -> {
              // Treat lookup failure as absence of unique attribute
              LOGGER.debug(
                  "Unique attribute check failed for ID={}, defaulting to false", rsId, err);
              return Future.succeededFuture(false);
            })
        .compose(useGrouping -> fetchLatestValues(rsId, useGrouping))
        .map(LatestData::new);
  }

  private Future<Boolean> isUniqueAttribute(String id) {
    LOGGER.trace("Checking unique attribute existence for ID={}", id);
    return uniqueAttrService
        .fetchUniqueAttributeInfo(id)
        .map(info -> info.containsKey("unique_attribute"));
  }

  private Future<JsonArray> fetchLatestValues(String id, boolean groupSnapshot) {
    Objects.requireNonNull(id, "Resource ID must not be null");

    RedisArgs args = argsBuilder.buildRedisArgs(id, groupSnapshot, tenantPrefix);
    LOGGER.trace("Searching Redis with key={}, path={}", args.key(), args.path());

    return redisService
        .searchAsync(args.key(), args.path())
        .map(redisResult -> parseResponse(args.key(), redisResult.toJson(), groupSnapshot));
  }

  /**
   * Parses the raw Redis JSON result into a JsonArray suitable for LatestData.
   *
   * @param key the Redis key used for grouping removal
   * @param result the raw JsonObject returned by Redis
   * @param grouped whether the query was grouped on unique attribute
   * @return a JsonArray of results
   */
  private JsonArray parseResponse(String key, JsonObject result, boolean grouped) {
    if (grouped) {
      result.remove(key);
      return new JsonArray(
          result.stream().map(java.util.Map.Entry::getValue).collect(Collectors.toList()));
    }
    return new JsonArray().add(result);
  }
}
