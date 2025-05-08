// IudxRedisCommandArgsBuilder.java (Implementation)
package org.cdpg.dx.rs.latest.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.cdpg.dx.database.redis.util.RedisCommandArgsBuilder;
import org.cdpg.dx.database.redis.util.RedisArgs;

public class LatestRedisCommandArgsBuilder implements RedisCommandArgsBuilder {
    @Override
    public RedisArgs buildRedisArgs(String id, boolean isUniqueAttributeExist, String tenantPrefix) {
        String processedKey = processKey(id, tenantPrefix);
        String path = buildPath(id, isUniqueAttributeExist);
        
        return new RedisArgs(processedKey, path);
    }

    private String processKey(String id, String tenantPrefix) {
        return tenantPrefix.equals("none") ? id : tenantPrefix + ":" + id;
    }

    private String buildPath(String id, boolean isUniqueAttributeExist) {
        if (isUniqueAttributeExist) {
            return ".";
        }
        String shaId = id + "/_d";
        return "._" + DigestUtils.sha1Hex(shaId);
    }
}
