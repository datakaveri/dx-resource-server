package org.cdpg.dx.rs.apiserver;

import static org.cdpg.dx.common.config.ServiceProxyAddressConstants.*;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.List;

import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.database.elastic.service.ElasticsearchService;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.database.redis.service.RedisService;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.revoked.service.RevokedService;
import org.cdpg.dx.rs.admin.controller.AdminController;
import org.cdpg.dx.rs.admin.dao.RevokedTokenServiceDAO;
import org.cdpg.dx.rs.admin.dao.UniqueAttributeServiceDAO;
import org.cdpg.dx.rs.admin.dao.impl.RevokedTokenServiceDAOImpl;
import org.cdpg.dx.rs.admin.dao.impl.UniqueAttributeServiceDAOImpl;
import org.cdpg.dx.rs.ingestion.controller.IngestionAdaptorController;
import org.cdpg.dx.rs.ingestion.dao.IngestionDAO;
import org.cdpg.dx.rs.ingestion.dao.impl.IngestionDAOImpl;
import org.cdpg.dx.rs.latest.controller.LatestController;
import org.cdpg.dx.rs.search.controller.SearchController;
import org.cdpg.dx.rs.subscription.controller.SubscriptionController;
import org.cdpg.dx.rs.subscription.dao.SubscriptionServiceDAO;
import org.cdpg.dx.rs.subscription.dao.impl.SubscriptionServiceDAOImpl;
import org.cdpg.dx.rs.usermanagement.controller.UserManagementController;
import org.cdpg.dx.uniqueattribute.service.UniqueAttributeService;

public class ControllerFactory {
  private final Vertx vertx;
  private String tenantPrefix;
  private String timeLimit;
  private PostgresService pgService;
  private ElasticsearchService esService;
  private DataBrokerService brokerService;
  private CatalogueService catService;
  private RevokedService revokedService;
  private RedisService redisService;
  private UniqueAttributeService uniqueAttributeService;
  private SubscriptionServiceDAO subscriptionServiceDAO;
  private UniqueAttributeServiceDAO uniqueAttributeServiceDAO;
  private RevokedTokenServiceDAO revokedTokenServiceDAO;
  private IngestionDAO ingestionDAO;

    public ControllerFactory(
            JsonObject config, Vertx vertx) {
        this.vertx = vertx;
        createProxies(vertx);
        initialization(config);
    }
  public List<ApiController> createControllers() {
    return List.of(
        new SubscriptionController(
            vertx, subscriptionServiceDAO, brokerService, catService, revokedService),
        new SearchController(esService, catService, tenantPrefix, timeLimit, revokedService),
        new LatestController(
            redisService, tenantPrefix, uniqueAttributeService, revokedService, catService),
        new UserManagementController(brokerService, revokedService),
        new AdminController(
            vertx,
            revokedTokenServiceDAO,
            uniqueAttributeServiceDAO,
            brokerService,
            revokedService),
        new IngestionAdaptorController(
            vertx, brokerService, revokedService, catService, ingestionDAO));
  }

  private void createProxies(Vertx vertx) {
    pgService = PostgresService.createProxy(vertx, POSTGRES_SERVICE_ADDRESS);
    esService = ElasticsearchService.createProxy(vertx, ELASTIC_SERVICE_ADDRESS);
    brokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
    catService = CatalogueService.createProxy(vertx, CATALOGUE_SERVICE_ADDRESS);
    revokedService = RevokedService.createProxy(vertx, REVOKED_SERVICE_ADDRESS);
    redisService = RedisService.createProxy(vertx, REDIS_SERVICE_ADDRESS);
    uniqueAttributeService =
        UniqueAttributeService.createProxy(vertx, UNIQUE_ATTRIBUTE_SERVICE_ADDRESS);
  }

  private void initialization(JsonObject config) {
      this.tenantPrefix =
              config.containsKey("tenantPrefix") ? config.getString("tenantPrefix") : "none";
      this.timeLimit = config.containsKey("timeLimit") ? config.getString("timeLimit") : "none";
    subscriptionServiceDAO = new SubscriptionServiceDAOImpl(pgService);
    ingestionDAO = new IngestionDAOImpl(pgService);
    uniqueAttributeServiceDAO = new UniqueAttributeServiceDAOImpl(pgService);
    revokedTokenServiceDAO = new RevokedTokenServiceDAOImpl(pgService);
  }
}
