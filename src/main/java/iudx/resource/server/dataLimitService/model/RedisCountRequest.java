package iudx.resource.server.dataLimitService.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.dataLimitService.util.UniqueKeyUtil;

public class RedisCountRequest {

    private String userid;
    private String resourceId;
    private String apiCountKey;
    private String totalSizeKey;

    // Convert to JsonObject (needed for DataObject)
    public JsonObject toJson() {
        return new JsonObject()
                .put("userid", userid)
                .put("resourceId", resourceId)
                .put("apiCountKey", apiCountKey)
                .put("totalSizeKey", totalSizeKey);
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setRedisKeys()
    {
        this.apiCountKey = UniqueKeyUtil.generateUniqueKey(userid, resourceId, "apiCount");
        this.totalSizeKey = UniqueKeyUtil.generateUniqueKey(userid, resourceId, "totalSize");

    }
    public String getApiCountKey() {
        return apiCountKey;
    }

    public String getTotalSizeKey() {
        return totalSizeKey;
    }
}
