package org.cdpg.dx.rs.ingestion.factory;

import org.cdpg.dx.auditing.handler.AuditingHandler;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.rs.authorization.handler.ResourcePolicyAuthorizationHandler;
import org.cdpg.dx.rs.ingestion.controller.IngestionAdaptorController;
import org.cdpg.dx.rs.ingestion.dao.IngestionDAO;
import org.cdpg.dx.rs.ingestion.dao.impl.IngestionDAOImpl;
import org.cdpg.dx.rs.ingestion.service.IngestionService;
import org.cdpg.dx.rs.ingestion.service.IngestionServiceImpl;
import org.cdpg.dx.validations.provider.ProviderValidationHandler;

public class IngestionControllerFactory {

  public static IngestionAdaptorController create(
      PostgresService postgresService,
      CatalogueService catalogueService,
      DataBrokerService dataBroker,
      ClientRevocationValidationHandler clientRevocationValidationHandler,
      ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler,
      ProviderValidationHandler providerValidationHandler,
      AuditingHandler auditingHandler) {
    IngestionDAO ingestionDAO = new IngestionDAOImpl(postgresService);
    IngestionService ingestionService =
        new IngestionServiceImpl(catalogueService, dataBroker, ingestionDAO);

    return new IngestionAdaptorController(
        ingestionService,
        clientRevocationValidationHandler,
        resourcePolicyAuthorizationHandler,
        providerValidationHandler,
        auditingHandler);
  }
}
