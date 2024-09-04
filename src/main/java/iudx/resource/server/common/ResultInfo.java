package iudx.resource.server.common;

public class ResultInfo {
  private int StatusCode;
  private long responseSize;

  public long getResponseSize() {
    return responseSize;
  }

  public void setResponseSize(long responseSize) {
    this.responseSize = responseSize;
  }

  public int getStatusCode() {
    return StatusCode;
  }

  public void setStatusCode(int statusCode) {
    StatusCode = statusCode;
  }
}
