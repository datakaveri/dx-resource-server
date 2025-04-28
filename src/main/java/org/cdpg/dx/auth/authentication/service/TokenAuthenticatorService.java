package org.cdpg.dx.auth.authentication.service;

import io.vertx.core.Future;
import org.cdpg.dx.common.models.JwtData;

public interface TokenAuthenticatorService {

  Future<JwtData> authenticate(String token);
}
