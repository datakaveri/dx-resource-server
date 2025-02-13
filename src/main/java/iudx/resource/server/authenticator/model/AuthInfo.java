package iudx.resource.server.authenticator.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.util.IudxRole;
import lombok.*;

@DataObject(generateConverter = true, publicConverter = false)
public class AuthInfo {
  private String userId;
  private String resourceId;
  private String providerId;
  private String api;
  private String resourceGroup;
  private String did;
  private String drl;
  private IudxRole role;
  private String endPoint;
  private JsonObject access;
  private JsonArray attributes;
  private String accessPolicy;

  /*public AuthInfo(JsonObject json) {
    AuthInfoConverter.fromJson(json, this);
  }*/

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public String getProviderId() {
    return providerId;
  }

  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  public String getApi() {
    return api;
  }

  public void setApi(String api) {
    this.api = api;
  }

  public String getResourceGroup() {
    return resourceGroup;
  }

  public void setResourceGroup(String resourceGroup) {
    this.resourceGroup = resourceGroup;
  }

  public String getDid() {
    return did;
  }

  public void setDid(String did) {
    this.did = did;
  }

  public String getDrl() {
    return drl;
  }

  public void setDrl(String drl) {
    this.drl = drl;
  }

  public IudxRole getRole() {
    return role;
  }

  public void setRole(IudxRole role) {
    this.role = role;
  }

  public String getEndPoint() {
    return endPoint;
  }

  public void setEndPoint(String endPoint) {
    this.endPoint = endPoint;
  }

  public JsonObject getAccess() {
    return access;
  }

  public void setAccess(JsonObject access) {
    this.access = access;
  }

  public JsonArray getAttributes() {
    return attributes;
  }

  public void setAttributes(JsonArray attributes) {
    this.attributes = attributes;
  }

  public String getAccessPolicy() {
    return accessPolicy;
  }

  public void setAccessPolicy(String accessPolicy) {
    this.accessPolicy = accessPolicy;
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put("userid", userId)
        .put("resourceId", resourceId)
        .put("providerId", providerId)
        .put("api", api)
        .put("resourceGroup", resourceGroup)
        .put("did", did)
        .put("drl", drl)
        .put("role", role != null ? role.toString() : null)
        .put("attributes", attributes)
        .put("accessPolicy", accessPolicy)
        .put("access", access);
  }

  @Override
  public String toString() {
    return toJson().toString();
  }
}
