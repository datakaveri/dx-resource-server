package iudx.resource.server.apiserver.auditing.handler;

import static iudx.resource.server.apiserver.auditing.util.Constants.AUDITING_EXCHANGE;
import static iudx.resource.server.apiserver.auditing.util.Constants.ROUTING_KEY;
import static iudx.resource.server.databroker.util.Constants.DATA_BROKER_SERVICE_ADDRESS;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.RoutingContextHelper;
import iudx.resource.server.databroker.service.DataBrokerService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auditing.service.RsAuditLogData;

public class AuditingHandler {
  private static final Logger LOGGER = LogManager.getLogger(AuditingHandler.class);
  private static final List<Integer> STATUS_CODES_TO_AUDIT = List.of(200, 201, 204);
  private static final String SERVER_ORIGIN = "rs-server";

  private final DataBrokerService databrokerService;

  private final Supplier<Long> epochSupplier =
      () -> LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
  private final Supplier<String> isoTimeSupplier =
      () -> LocalDateTime.now().atZone(ZoneOffset.UTC).toString();
  private final Supplier<String> primaryKeySupplier =
      () -> UUID.randomUUID().toString().replace("-", "");

  public AuditingHandler(Vertx vertx) {
    this.databrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
  }

  public void handleApiAudit(RoutingContext context) {
    context.addBodyEndHandler(
        v -> {
          try {
            publishAuditLogs(context);
          } catch (Exception e) {
            LOGGER.error("Error: while publishing auditing log: " + e.getMessage());
            throw new RuntimeException(e);
          }
        });
    context.next();
  }

  public void publishAuditLogs(RoutingContext context) throws Exception {
    LOGGER.trace("AuditingHandler() started");
    if (!STATUS_CODES_TO_AUDIT.contains(context.response().getStatusCode())) {
      LOGGER.debug("Skipping audit for status code: {}", context.response().getStatusCode());
      return;
    }
    JwtData jwtData = RoutingContextHelper.getJwtData(context);
    RsAuditLogData auditLogData = createAuditLogData(jwtData, context);
    String api = RoutingContextHelper.getEndPoint(context);
    LOGGER.debug("auditLogData : " + auditLogData.toJson());

    // this is expected but i am not sure how it will work in the actual code??
    org.cdpg.dx.util.RoutingContextHelper.setAuditingLog(context, auditLogData.toJson());

    databrokerService
        .publishMessage(auditLogData.toJson(), AUDITING_EXCHANGE, ROUTING_KEY)
        .onSuccess(
            success ->
                LOGGER.info(
                    "Auditing log published successfully for user: {} endPoint: {}",
                    jwtData.getSub(),
                    api))
        .onFailure(
            failure -> LOGGER.error("Failed to publish auditing log: {}", failure.getMessage()));
  }

  private RsAuditLogData createAuditLogData(JwtData jwtData, RoutingContext context)
      throws Exception {

    RsAuditLogData auditLogData = new RsAuditLogData();
    auditLogData.setOrigin(SERVER_ORIGIN);
    auditLogData.setPrimaryKey(primaryKeySupplier.get());
    auditLogData.setEpochTime(epochSupplier.get());
    auditLogData.setIsoTime(isoTimeSupplier.get());
    auditLogData.setResponseSize(RoutingContextHelper.getResponseSize(context));
    auditLogData.setApi(RoutingContextHelper.getEndPoint(context));
    auditLogData.setId(RoutingContextHelper.getId(context));
    auditLogData.setUserid(jwtData.getSub());
    auditLogData.setDelegatorId(getDelegatorId(jwtData));
    return auditLogData;
  }

  private String getDelegatorId(JwtData jwtData) {
    if ("delegate".equalsIgnoreCase(jwtData.getRole()) && jwtData.getDrl() != null) {
      return jwtData.getDid();
    }
    return jwtData.getSub();
  }
}
