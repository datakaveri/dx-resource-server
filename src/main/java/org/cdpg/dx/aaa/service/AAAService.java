package org.cdpg.dx.aaa.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.cdpg.dx.aaa.models.UserInfo;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.common.models.User;

@VertxGen
@ProxyGen
public interface AAAService {
  @GenIgnore
  static AAAService createProxy(Vertx vertx, String address) {
    return new AAAServiceVertxEBProxy(vertx, address);
  }

 // Future<User> fetchUserInfo(UserInfo userInfo);

  Future<String> getPublicOrCertKey();
}
