package iudx.resource.server.apiserver.metering.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.List;

import iudx.resource.server.apiserver.metering.service.MeteringService;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.ResultInfo;
import iudx.resource.server.common.RoutingContextHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(MeteringHandler.class);
  final List<Integer> STATUS_CODES_TO_AUDIT = List.of(200, 201);
  private final MeteringService meteringService;

  public MeteringHandler(MeteringService meteringService) {
    this.meteringService = meteringService;
  }

  @Override
  public void handle(RoutingContext context) {
    JwtData jwtData = RoutingContextHelper.getJwtData(context);
    String endPoint = RoutingContextHelper.getEndPoint(context);

    /*ResultInfo resultInfo = RoutingContextHelper.getResultInfo(context);*/
    /*long responseSize = resultInfo.getResponseSize();*/
    /*LOGGER.info("response status:: {}", resultInfo.getStatusCode());*/

    /*if (STATUS_CODES_TO_AUDIT.contains(resultInfo.getStatusCode())) {*/

      meteringService
          .publishMeteringLogMessage(jwtData, 0, endPoint)
          .onSuccess(success -> LOGGER.info("Metering log published successfully"))
          .onFailure(
              failure -> LOGGER.error("Failed to publish metering log: {}", failure.getMessage()));
    /*}*/
  }
}
