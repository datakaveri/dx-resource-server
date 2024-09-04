package iudx.resource.server.metering;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.AuthInfo;

@ProxyGen
@VertxGen
public interface MeteringServiceNew {

  @GenIgnore
  static MeteringService createProxy(Vertx vertx, String address) {
    return new MeteringServiceVertxEBProxy(vertx, address);
  }
  Future<Void> publishMeteringLogMessage(AuthInfo authInfo, long responseSize);
}
