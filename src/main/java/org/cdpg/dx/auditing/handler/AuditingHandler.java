package org.cdpg.dx.auditing.handler;

import static org.cdpg.dx.auditing.util.Constants.AUDITING_EXCHANGE;
import static org.cdpg.dx.auditing.util.Constants.ROUTING_KEY;
import static org.cdpg.dx.databroker.util.Constants.DATA_BROKER_SERVICE_ADDRESS;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auditing.model.AuditLog;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.util.RoutingContextHelper;

public class AuditingHandler {
  private static final Logger LOGGER = LogManager.getLogger(AuditingHandler.class);
  private static final List<Integer> STATUS_CODES_TO_AUDIT = List.of(200, 201, 204);
  private final DataBrokerService databrokerService;

  public AuditingHandler(Vertx vertx) {
    this.databrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
  }

  public void handleApiAudit(RoutingContext context) {
    context.addBodyEndHandler(
        v -> {
          try {
            if (!STATUS_CODES_TO_AUDIT.contains(context.response().getStatusCode())) {
              LOGGER.debug(
                  "Skipping audit for status code: {}", context.response().getStatusCode());
              return;
            }
            Optional<List<AuditLog>> auditLogData = RoutingContextHelper.getAuditingLog(context);
            if (auditLogData.isPresent()) {
              publishAuditLogs(auditLogData.get());
            } else {
              LOGGER.warn("No auditing log found in context");
            }

          } catch (Exception e) {
            LOGGER.error("Error: while publishing auditing log: " + e.getMessage());
            throw new RuntimeException(e);
          }
        });
    context.next();
  }

  public void publishAuditLogs(List<AuditLog> auditLogList) throws Exception {
    LOGGER.trace("AuditingHandler() started");

    auditLogList.forEach(
        log -> {
          LOGGER.debug("auditLogData : {}", log.toJson());
          databrokerService
              .publishMessage(log.toJson(), AUDITING_EXCHANGE, ROUTING_KEY)
              .onSuccess(success -> LOGGER.info("Auditing log published successfully"))
              .onFailure(
                  failure ->
                      LOGGER.error("Failed to publish auditing log: {}", failure.getMessage()));
        });
  }
}
