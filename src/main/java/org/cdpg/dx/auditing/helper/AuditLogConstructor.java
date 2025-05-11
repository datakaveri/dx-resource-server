package org.cdpg.dx.auditing.helper;

import static org.cdpg.dx.auditing.util.Constants.SERVER_ORIGIN;

import io.vertx.ext.web.RoutingContext;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auditing.model.AuditLog;
import org.cdpg.dx.auditing.model.RsAuditLog;
import org.cdpg.dx.common.models.JwtData;
import org.cdpg.dx.util.RoutingContextHelper;

public class AuditLogConstructor  {
  private static final Logger LOGGER = LogManager.getLogger(AuditLogConstructor.class);
  private final RoutingContext routingContext;
  private final Supplier<Long> epochSupplier =
          () -> LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
  private final Supplier<String> isoTimeSupplier =
          () -> LocalDateTime.now().atZone(ZoneOffset.UTC).toString();
  private final Supplier<String> primaryKeySupplier =
          () -> UUID.randomUUID().toString().replace("-", "");


  public AuditLogConstructor(RoutingContext routingContext) {
    this.routingContext = routingContext;
    generateRsAuditLog();
  }

  public void generateRsAuditLog() {
    Optional<JwtData> jwtData = RoutingContextHelper.getJwtData(routingContext);
    String api = RoutingContextHelper.getAuthInfo(routingContext).getString("api_endpoint");
    long responseSize = RoutingContextHelper.getResponseSize(routingContext);
    String id = RoutingContextHelper.getId(routingContext);

    AuditLog rsAuditLog = new RsAuditLog(primaryKeySupplier.get(), jwtData.get().sub(), id, api, responseSize, epochSupplier.get(), isoTimeSupplier.get(), getDelegatorId(jwtData.get()),SERVER_ORIGIN);
    List<AuditLog> rsAuditLogList = new ArrayList<>();
    rsAuditLogList.add(rsAuditLog);
    RoutingContextHelper.setAuditingLog(routingContext, rsAuditLogList);
  }

  private String getDelegatorId(JwtData jwtData) {
    if ("delegate".equalsIgnoreCase(jwtData.role()) && jwtData.drl() != null) {
      return jwtData.did();
    }
    return jwtData.sub();
  }
}
