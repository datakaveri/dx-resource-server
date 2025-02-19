package iudx.resource.server.apiserver.metering.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.metering.model.MeteringLog;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.cache.service.CacheService;
import iudx.resource.server.common.Response;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import iudx.resource.server.databroker.service.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static iudx.resource.server.apiserver.metering.util.Constant.*;

import static iudx.resource.server.cache.util.CacheType.CATALOGUE_CACHE;

public class MeteringServiceImpl implements MeteringService {
    @Override
    public Future<Void> publishMeteringLogMessage(JwtData jwtData, long responseSize, String endPoint) {
        return null;
    }
   /* private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
    public DataBrokerService rmqService;
    private CacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    Supplier<Long> epochSupplier = () -> LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    Supplier<String> isoTimeSupplier =
            () -> ZonedDateTime.now(ZoneId.of(ZoneId.SHORT_IDS.get("IST"))).toString();
    Supplier<String> primaryKeySuppler = () -> UUID.randomUUID().toString().replace("-", "");
    private MeteringLog meteringLog;
    String resourceGroup;
    String providerId;

    public MeteringServiceImpl(DataBrokerService dataBrokerService, CacheService cacheService) {
        this.rmqService = dataBrokerService;
        this.cacheService = cacheService;
    }

    @Override
    public Future<Void> publishMeteringLogMessage(JwtData jwtData, long responseSize, String endPoint) {
        Promise<Void> promise = Promise.promise();
        meteringLog  = createMeteringLog(jwtData, responseSize, endPoint);
        LOGGER.trace("JWT DATA: " + jwtData);
        LOGGER.debug("write message =  {}", meteringLog.toString());
        rmqService.publishMessage(
                meteringLog.toJson(),
                EXCHANGE_NAME,
                ROUTING_KEY).onComplete(
                rmqHandler -> {
                    if (rmqHandler.succeeded()) {
                        LOGGER.info("inserted into rmq");
                        promise.complete();
                    } else {
                        LOGGER.error(rmqHandler.cause());
                        try {
                            Response resp =
                                    objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
                            LOGGER.debug("response from rmq " + resp);
                            promise.fail(resp.toString());
                        } catch (JsonProcessingException e) {
                            LOGGER.error("Failure message not in format [type,title,detail]");
                            promise.fail(e.getMessage());
                        }
                    }
                });
        return promise.future();
    }

    private MeteringLog createMeteringLog(JwtData jwtData, long responseSize, String endPoint) {
        String delegatorId = getDelegatorId(jwtData);
        String event = getEvent(jwtData);
        *//*resourceGroup = getCacheResult(jwtData.getIid().split(":")[1]);*//*
        JsonObject cacheJson =
                new JsonObject().put("key", jwtData.getIid().split(":")[1]).put("type", CATALOGUE_CACHE);

        return meteringLog =
                new MeteringLog.Builder()
                        .forUserId(jwtData.getSub())
                        .forResourceId(jwtData.getIid().split(":")[1])
                        .forResourceGroup(*//*jwtData.getIid()*//*"")
                        .forApi(endPoint)
                        .forEvent(event)
                        .forType("RESOURCE")
                        .withPrimaryKey(primaryKeySuppler.get())
                        .withProviderId("authInfo.getProviderId()")
                        .withDelegatorId(delegatorId)
                        .withResponseSize(responseSize)
                        .atEpoch(epochSupplier.get())
                        .atIsoTime(isoTimeSupplier.get())
                        .forOrigin(ORIGIN_SERVER)
                        .build();
    }


    private String getDelegatorId(JwtData jwtData) {
        String delegatorId;
        if (jwtData.getRole().equalsIgnoreCase("delegate") && jwtData.getDrl() != null) {
            delegatorId = jwtData.getDid();
        } else {
            delegatorId = jwtData.getSub();
        }
        return delegatorId;
    }

    private String getEvent(JwtData jwtData) {
        String event = null;
        *//*if (authInfo.getEndPoint().contains("/ngsi-ld/v1/subscription")) {
            event = "subscriptions";
        }*//*
        return event;
    }*/

}
