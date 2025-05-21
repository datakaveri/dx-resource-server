package org.cdpg.dx.rs.search.factory;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.auditing.handler.AuditingHandler;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.database.elastic.service.ElasticsearchService;
import org.cdpg.dx.rs.authorization.handler.ResourcePolicyAuthorizationHandler;
import org.cdpg.dx.rs.search.controller.SearchController;
import org.cdpg.dx.rs.search.service.SearchApiService;
import org.cdpg.dx.rs.search.service.SearchApiServiceImpl;

public class SearchControllerFactory {

  public static SearchController create(
      ElasticsearchService elasticsearchService,
      CatalogueService catalogueService,
      ClientRevocationValidationHandler clientRevocationValidationHandler,
      ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler,
      AuditingHandler auditingHandler,
      JsonObject config) {
    String tenantPrefix = config.getString("tenantPrefix");
    String timeLimit = config.getString("timeLimit");
    SearchApiService searchApiService =
        new SearchApiServiceImpl(elasticsearchService, catalogueService, tenantPrefix, timeLimit);

    return new SearchController(
        searchApiService,
        clientRevocationValidationHandler,
        resourcePolicyAuthorizationHandler,
        auditingHandler);
  }
}
