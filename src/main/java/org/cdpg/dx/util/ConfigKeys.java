package org.cdpg.dx.util;

import java.util.Set;

public class ConfigKeys {
    public static final String AUTH_HOST = "authHost";
    public static final String AUTH_PORT = "authPort";
    public static final String DX_AUTH_BASE_PATH = "dxAuthBasePath";
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";

    // Set of required keys (Dynamically used for validation)
    public static final Set<String> REQUIRED_KEYS = Set.of(
            AUTH_HOST, AUTH_PORT, DX_AUTH_BASE_PATH, CLIENT_ID, CLIENT_SECRET
    );
}
