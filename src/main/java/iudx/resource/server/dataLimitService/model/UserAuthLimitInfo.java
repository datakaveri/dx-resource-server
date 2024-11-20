package iudx.resource.server.dataLimitService.model;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.authorization.IudxRole;
import iudx.resource.server.authenticator.model.AuthInfo;

public class UserAuthLimitInfo {
    public AuthInfo authInfo;
    private String userId;
    private String resourceId;
    private IudxRole role;
    private JsonObject access;
    private String accessPolicy;
    private String endPoint;

    public String getEndPoint() {
        return endPoint;
    }

    public UserAuthLimitInfo(AuthInfo  authInfo){
        this.authInfo=authInfo;
        setUserAuthInfo();
    }

    public String getUserId() {
        return userId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public IudxRole getRole() {
        return role;
    }

    public JsonObject getAccess() {
        return access;
    }

    public String getAccessPolicy() {
        return accessPolicy;
    }


    public void setUserAuthInfo(){
        this.accessPolicy = authInfo.getAccessPolicy();
        this.access = authInfo.getAccess();
        this.role = authInfo.getRole();
        this.resourceId = authInfo.getResourceId();
        this.userId = authInfo.getUserid();
        this.endPoint=authInfo.getEndPoint();
    }


}
