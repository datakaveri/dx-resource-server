package org.cdpg.dx.rs.apiserver;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.*;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auditing.handler.AuditingHandler;
import org.cdpg.dx.auth.authorization.handler.ClientRevocationValidationHandler;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.database.elastic.service.ElasticsearchService;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.database.redis.service.RedisService;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.revoked.service.RevokedService;
import org.cdpg.dx.rs.authorization.handler.ResourcePolicyAuthorizationHandler;
import org.cdpg.dx.rs.ingestion.controller.IngestionAdaptorController;
import org.cdpg.dx.rs.ingestion.factory.IngestionControllerFactory;
import org.cdpg.dx.rs.latest.controller.LatestController;
import org.cdpg.dx.rs.latest.factory.LatestControllerFactory;
import org.cdpg.dx.rs.search.controller.SearchController;
import org.cdpg.dx.rs.search.factory.SearchControllerFactory;
import org.cdpg.dx.rs.subscription.controller.SubscriptionController;
import org.cdpg.dx.rs.subscription.factory.SubscriptionControllerFactory;
import org.cdpg.dx.rs.usermanagement.controller.UserManagementController;
import org.cdpg.dx.rs.usermanagement.factory.UserManagementControllerFactory;
import org.cdpg.dx.uniqueattribute.service.UniqueAttributeService;
import org.cdpg.dx.validations.provider.ProviderValidationHandler;

public class ControllerFactory {
  private static final Logger LOGGER = LogManager.getLogger(ControllerFactory.class);

  private ControllerFactory() {}

  public static List<ApiController> createControllers(Vertx vertx, JsonObject config) {
    // Service proxies
    final PostgresService pgService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    final ElasticsearchService esService =
        ElasticsearchService.createProxy(vertx, ELASTIC_SERVICE_ADDRESS);
    final DataBrokerService brokerService =
        DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
    final CatalogueService catService =
        CatalogueService.createProxy(vertx, CATALOGUE_SERVICE_ADDRESS);
    final RevokedService revokedService =
        RevokedService.createProxy(vertx, REVOKED_SERVICE_ADDRESS);
    final RedisService redisService = RedisService.createProxy(vertx, REDIS_SERVICE_ADDRESS);
    final UniqueAttributeService uniqueAttrService =
        UniqueAttributeService.createProxy(vertx, UNIQUE_ATTRIBUTE_SERVICE_ADDRESS);

    // Handlers
    final ClientRevocationValidationHandler revocationHandler =
        new ClientRevocationValidationHandler(revokedService);
    final ResourcePolicyAuthorizationHandler policyAuthHandler =
        new ResourcePolicyAuthorizationHandler(catService);
    final ProviderValidationHandler providerValidationHandler =
        new ProviderValidationHandler(catService);
    final AuditingHandler auditingHandler = new AuditingHandler(vertx);

    String tenantPrefix = config.getString("tenantPrefix");

    // Controllers
    final IngestionAdaptorController ingestionController =
        IngestionControllerFactory.create(
            pgService,
            catService,
            brokerService,
            revocationHandler,
            policyAuthHandler,
            providerValidationHandler,
            auditingHandler);

    final SubscriptionController subscriptionController =
        SubscriptionControllerFactory.create(
            pgService,
            catService,
            brokerService,
            revocationHandler,
            policyAuthHandler,
            auditingHandler);

    UserManagementController userManagementController =
        UserManagementControllerFactory.create(brokerService, revocationHandler);

    LatestController latestController =
        LatestControllerFactory.create(
            redisService,
            uniqueAttrService,
            revocationHandler,
            policyAuthHandler,
            auditingHandler,
            tenantPrefix);

    SearchController searchController =
        SearchControllerFactory.create(
            esService, catService, revocationHandler, policyAuthHandler, auditingHandler, config);

    return List.of(
        ingestionController,
        subscriptionController,
        userManagementController,
        latestController,
        searchController);
  }
}
