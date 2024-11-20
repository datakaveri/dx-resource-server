//package iudx.resource.server.apiserver.handlers;
//
//import io.vertx.core.Handler;
//import io.vertx.core.http.HttpServerResponse;
//import io.vertx.ext.web.RoutingContext;
//import iudx.resource.server.apiserver.util.ContextHelper;
//import iudx.resource.server.authenticator.model.AuthInfo;
//import iudx.resource.server.common.ResultInfo;
//import iudx.resource.server.metering.MeteringServiceNew;
//import java.util.List;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//public class MeteringHandler implements Handler<RoutingContext> {
//
//  private static final Logger LOGGER = LogManager.getLogger(MeteringHandler.class);
//  final List<Integer> STATUS_CODES_TO_AUDIT = List.of(200, 201);
//  private final MeteringServiceNew meteringServiceNew;
//
//  public MeteringHandler(MeteringServiceNew meteringServiceNew) {
//    this.meteringServiceNew = meteringServiceNew;
//  }
//
//  @Override
//  public void handle(RoutingContext context) {
//    AuthInfo authInfo = ContextHelper.getAuthInfo(context);
//    ResultInfo resultInfo = ContextHelper.getResultInfo(context);
//    long responseSize = resultInfo.getResponseSize();
//    LOGGER.info("response status:: {}", resultInfo.getStatusCode());
//
//    if (STATUS_CODES_TO_AUDIT.contains(resultInfo.getStatusCode())) {
//
//      meteringServiceNew
//          .publishMeteringLogMessage(authInfo, responseSize)
//          .onSuccess(success -> LOGGER.info("Metering log published successfully"))
//          .onFailure(
//              failure -> LOGGER.error("Failed to publish metering log: {}", failure.getMessage()));
//    }
//  }
//}
