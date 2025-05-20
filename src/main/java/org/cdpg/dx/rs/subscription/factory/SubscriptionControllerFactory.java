package org.cdpg.dx.rs.subscription.factory;

import org.cdpg.dx.auditing.handler.AuditingHandler;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.rs.authorization.handler.ResourcePolicyAuthorizationHandler;
import org.cdpg.dx.rs.subscription.controller.SubscriptionController;
import org.cdpg.dx.rs.subscription.dao.SubscriptionServiceDAO;
import org.cdpg.dx.rs.subscription.dao.impl.SubscriptionServiceDAOImpl;
import org.cdpg.dx.rs.subscription.service.SubscriptionService;
import org.cdpg.dx.rs.subscription.service.SubscriptionServiceImpl;

public class SubscriptionControllerFactory {
  public static SubscriptionController create(
      PostgresService postgresService,
      CatalogueService catalogueService,
      DataBrokerService dataBroker,
      ClientRevocationValidationHandler clientRevocationValidationHandler,
      ResourcePolicyAuthorizationHandler resourcePolicyAuthorizationHandler,
      AuditingHandler auditingHandler) {
    SubscriptionServiceDAO subscriptionServiceDAO = new SubscriptionServiceDAOImpl(postgresService);
    SubscriptionService subscriptionService =
        new SubscriptionServiceImpl(subscriptionServiceDAO, dataBroker, catalogueService);

    return new SubscriptionController(
        subscriptionService,
        auditingHandler,
        clientRevocationValidationHandler,
        resourcePolicyAuthorizationHandler);
  }
}
