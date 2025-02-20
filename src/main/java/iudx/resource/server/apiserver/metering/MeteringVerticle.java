package iudx.resource.server.apiserver.metering;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.resource.server.apiserver.metering.service.MeteringService;
import iudx.resource.server.apiserver.metering.service.MeteringServiceImpl;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.apiserver.metering.util.Constant.METERING_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.*;
import static iudx.resource.server.databroker.util.Constants.DATA_BROKER_SERVICE_ADDRESS;

public class MeteringVerticle extends AbstractVerticle{
    private static final Logger LOGGER = LogManager.getLogger(MeteringVerticle.class);
    private ServiceBinder binder;
    private MessageConsumer<JsonObject> consumer;
    private MeteringService meteringService;
    private PostgresService postgresService;
    private CacheService cacheService;
    private DataBrokerService dataBrokerService;
    @Override
    public void start() throws Exception {
        binder = new ServiceBinder(vertx);
        postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
        this.cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
        this.dataBrokerService = DataBrokerService.createProxy(vertx, DATA_BROKER_SERVICE_ADDRESS);
       // meteringService = new MeteringServiceImpl(/*vertx, postgresService, cacheService*/dataBrokerService,cacheService);
        consumer =
                binder.setAddress(METERING_SERVICE_ADDRESS).register(MeteringService.class, meteringService);
        LOGGER.info("Metering Verticle Started");
    }

    @Override
    public void stop() throws Exception {
        binder.unregister(consumer);
    }
}
