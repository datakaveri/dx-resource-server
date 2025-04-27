package org.cdpg.dx.rs.apiserver;

import static org.cdpg.dx.common.util.ProxyAddressConstants.*;

import io.vertx.core.Vertx;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.catalogue.service.CatalogueService;
import org.cdpg.dx.database.elastic.service.ElasticsearchService;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.rs.subscription.SubsCriptionController;

public class ControllerFactory {
  private static final Logger LOGGER = LogManager.getLogger(ControllerFactory.class);
  private final Vertx vertx;
  private PostgresService pgService;
  private ElasticsearchService esService;
  private DataBrokerService brokerService;
  private CatalogueService catService;
  private boolean isTimeLimitEnabled;
  private String dxApiBasePath;

  public ControllerFactory(boolean isTimeLimitEnabled, String dxApiBasePath, Vertx vertx) {
    this.isTimeLimitEnabled = isTimeLimitEnabled;
    this.dxApiBasePath = dxApiBasePath;
    this.vertx = vertx;
    CreateProxies(vertx);
  }

  public List<ApiController> createControllers() {
    return List.of(new SubsCriptionController(vertx, pgService, brokerService, catService));
  }

  private void CreateProxies(Vertx vertx) {
    pgService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    esService = ElasticsearchService.createProxy(vertx, ELASTIC_SERVICE_ADDRESS);
    brokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
    catService = CatalogueService.createProxy(vertx, CATALOGUE_SERVICE_ADDRESS);
  }
}
