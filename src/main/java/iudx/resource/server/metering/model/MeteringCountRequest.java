package iudx.resource.server.metering.model;

public class MeteringCountRequest {
  private String startTime;
  private String endTime;
  private String userid;
  private String accessType;
  private String resourceId;

  @Override
  public String toString() {
    return "MeteringCountRequest{"
        + "startTime='"
        + startTime
        + '\''
        + ", endTime='"
        + endTime
        + '\''
        + ", userid='"
        + userid
        + '\''
        + ", accessType='"
        + accessType
        + '\''
        + ", resourceId='"
        + resourceId
        + '\''
        + '}';
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public String getUserid() {
    return userid;
  }

  public void setUserid(String userid) {
    this.userid = userid;
  }

  public String getAccessType() {
    return accessType;
  }

  public void setAccessType(String accessType) {
    this.accessType = accessType;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }
}
