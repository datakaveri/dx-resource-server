// LatestServiceImpl.java
package org.cdpg.dx.rs.latest.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import iudx.resource.server.cache.service.CacheService;
import org.cdpg.dx.uniqueattribute.service.UniqueAttributeServiceImpl;
import org.cdpg.dx.database.redis.service.RedisService;
import org.cdpg.dx.database.redis.util.RedisArgs;
import org.cdpg.dx.database.redis.util.RedisCommandArgsBuilder;
import org.cdpg.dx.rs.latest.model.LatestData;
import org.cdpg.dx.rs.latest.util.RedisResponseParser;

public class LatestServiceImpl implements LatestService {
    private final RedisService redisService;
    private final CacheService cacheService;
    private final RedisCommandArgsBuilder redisCmdBuilder;
    private final RedisResponseParser responseParser;
    private final String tenantPrefix;
    private final UniqueAttributeServiceImpl uniqueAttributeService;
    public LatestServiceImpl(RedisService redisService,
                             CacheService cacheService,
                             RedisCommandArgsBuilder redisCmdBuilder,
                             RedisResponseParser responseParser,
                             String tenantPrefix,UniqueAttributeServiceImpl uniqueAttributeService) {
        this.redisService = redisService;
        this.cacheService = cacheService;
        this.redisCmdBuilder = redisCmdBuilder;
        this.responseParser = responseParser;
        this.tenantPrefix = tenantPrefix;
        this.uniqueAttributeService=uniqueAttributeService;
    }

    @Override
    public Future<LatestData> getLatestData(String rsId) {
        Promise<LatestData> promise = Promise.promise();

        isUniqueAttrRecordExist(rsId)
                .compose(uniqueAttrRecord -> getLatestValue(rsId,uniqueAttrRecord))
                .onSuccess(result -> promise.complete(new LatestData(result)))
                .onFailure(promise::fail);

        return promise.future();
    }

    private Future<JsonArray> getLatestValue(String id,boolean isUniqueAttrRecord) {
        Promise<JsonArray> promise = Promise.promise();

        RedisArgs args = redisCmdBuilder.buildRedisArgs(id, isUniqueAttrRecord, tenantPrefix);

        redisService.searchAsync(args.key(), args.path())
                .onSuccess(redisResult -> {
                    JsonArray parsed = responseParser.parseResponse(args.key(), redisResult.getResponseJson(), isUniqueAttrRecord);
                    promise.complete(parsed);
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    private Future<Boolean> isUniqueAttrRecordExist(String id) {
        Promise<Boolean> promise = Promise.promise();

        uniqueAttributeService.fetchUniqueAttributeInfo(id).onSuccess(successHandler->{
                        promise.complete(successHandler.containsKey("unique_attribute"));
                })
                .onFailure(err -> {
                    promise.fail("Unique attribute doesn't exist for id: " + id);
                });

        return promise.future();
    }
}
