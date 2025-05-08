package org.cdpg.dx.database.redis.util;

public interface RedisCommandArgsBuilder {
    RedisArgs buildRedisArgs(String id, boolean isUniqueAttributeExist, String tenantPrefix);
}
