package iudx.resource.server.cache;

import static iudx.resource.server.cache.util.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.resource.server.database.postgres.util.Constants.PG_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.cache.service.CacheServiceImpl;
import iudx.resource.server.cache.service.type.CatalogueCacheImpl;
import iudx.resource.server.cache.service.type.RevokedClientCache;
import iudx.resource.server.cache.service.type.UniqueAttributeCache;
import iudx.resource.server.database.postgres.service.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CacheVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(CacheVerticle.class);
  static WebClient catWebClient;
  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;
  private CacheService cacheService;
  private PostgresService postgresService;
  private CatalogueCacheImpl catalogueCache;
  private RevokedClientCache revokedClientCache;
  private UniqueAttributeCache uniqueAttributeCache;

  @Override
  public void start() throws Exception {
    postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true);
    catWebClient = WebClient.create(vertx, options);
    catalogueCache =
        new CatalogueCacheImpl(
            vertx,
            config().getString("catServerHost"),
            config().getInteger("catServerPort"),
            config().getString("dxCatalogueBasePath"),
            catWebClient);

    revokedClientCache = new RevokedClientCache(vertx, postgresService);
    uniqueAttributeCache = new UniqueAttributeCache(vertx, postgresService);

    cacheService = new CacheServiceImpl(catalogueCache, revokedClientCache, uniqueAttributeCache);
    binder = new ServiceBinder(vertx);
    consumer = binder.setAddress(CACHE_SERVICE_ADDRESS).register(CacheService.class, cacheService);

    LOGGER.info("Cache Verticle deployed.");
  }

  @Override
  public void stop() throws Exception {
    binder.unregister(consumer);
  }
}
