package iudx.resource.server.apiserver.auditing.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.auditing.model.AuditLogSearchRequest;
import iudx.resource.server.apiserver.auditing.model.MonthlyOverviewResponse;
import iudx.resource.server.apiserver.auditing.model.OverviewRequest;

import java.util.List;


public interface AuditLogService {

  Future<JsonObject> executeAuditingSearchQuery(AuditLogSearchRequest searchRequest);

   Future<List<MonthlyOverviewResponse>> monthlyOverview(OverviewRequest overviewRequest);

  Future<JsonObject> summaryOverview(OverviewRequest overviewRequest);
}
