package iudx.resource.server.apiserver.util;

import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.authenticator.model.AuthInfo;
import iudx.resource.server.common.ResultInfo;

public class ContextHelper {

  private static final String AUTH_INFO_KEY = "authInfo";
  private static final String RESULT_INFO_INFO_KEY = "authInfo";

  public static void putAuthInfo(RoutingContext context, AuthInfo authInfo) {
    context.data().put(AUTH_INFO_KEY, authInfo);
  }

  public static AuthInfo getAuthInfo(RoutingContext context) {
    Object value = context.data().get(AUTH_INFO_KEY);
    if (value instanceof AuthInfo) {
      return (AuthInfo) value;
    }
    throw new IllegalStateException(
        "AuthInfo is missing or is of the wrong type in the RoutingContext.");
  }

  public static void putResultInfo(RoutingContext context, ResultInfo resultInfo) {
    context.data().put(RESULT_INFO_INFO_KEY, resultInfo);
  }

  public static ResultInfo getResultInfo(RoutingContext context) {
    Object value = context.data().get(RESULT_INFO_INFO_KEY);
    if (value instanceof ResultInfo) {
      return (ResultInfo) value;
    }
    throw new IllegalStateException(
        "AuthInfo is missing or is of the wrong type in the RoutingContext.");
  }
}
