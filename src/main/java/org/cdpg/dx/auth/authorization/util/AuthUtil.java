package org.cdpg.dx.auth.authorization.util;

import java.util.List;

public class AuthUtil {

  private static final List<String> OPEN_ENDPOINTS =
      List.of(
          "/temporal/entities",
          "/entities",
          "/entityOperations/query",
          "/temporal/entityOperations/query",
          "/async/status",
          "/consumer/audit",
          "/async/search",
          "/subscription",
          "/user/resetPassword",
          "/overview",
          "/summary");

  public static boolean isSkipResourceIdCheck(String endPoint, String method) {
    return endPoint.equalsIgnoreCase("/subscription")
            && (method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("DELETE"))
        || endPoint.equalsIgnoreCase("/user/resetPassword")
        || endPoint.equalsIgnoreCase("/consumer/audit")
        || endPoint.equalsIgnoreCase("/admin/revokeToken")
        || endPoint.equalsIgnoreCase("/admin/resourceattribute")
        || endPoint.equalsIgnoreCase("/provider/audit")
        || endPoint.equalsIgnoreCase("/async/status")
        || endPoint.equalsIgnoreCase("/ingestion")
        || endPoint.equalsIgnoreCase("/overview")
        || endPoint.equalsIgnoreCase("/summary");
  }

  public static boolean isOpenEndpoint(String endPoint) {
    for (String item : OPEN_ENDPOINTS) {
      if (endPoint.contains(item)) {
        return true;
      }
    }
    return false;
  }
}
