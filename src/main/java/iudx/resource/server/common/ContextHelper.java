package iudx.resource.server.common;

import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.authenticator.model.AuthInfo;

public class ContextHelper {
    private static final String AUTH_INFO_KEY = "authInfo";
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
}
