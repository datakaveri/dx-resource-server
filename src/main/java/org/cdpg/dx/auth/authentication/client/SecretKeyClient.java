package org.cdpg.dx.auth.authentication.client;

import io.vertx.core.Future;

/** Client interface to fetch secret key from the authentication server. */
public interface SecretKeyClient {
  Future<String> fetchCertKey();
}
