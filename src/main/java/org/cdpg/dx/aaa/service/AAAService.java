package org.cdpg.dx.aaa.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

@VertxGen
@ProxyGen
public interface AAAService {
  @GenIgnore
  static AAAService createProxy(Vertx vertx, String address) {
    return new AAAServiceVertxEBProxy(vertx, address);
  }

  // todo : since vertx don't support java record,we need to use JsonObject or some other dataobject
  // Future<User> fetchUserInfo(UserInfo userInfo);

  Future<String> getPublicOrCertKey();
}
