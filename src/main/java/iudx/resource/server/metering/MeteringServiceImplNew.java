package iudx.resource.server.metering;

import static iudx.resource.server.metering.util.Constants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.*;
import iudx.resource.server.authenticator.model.AuthInfo;
import iudx.resource.server.common.Response;
import iudx.resource.server.databroker.DataBrokerService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import iudx.resource.server.metering.util.MeteringLogBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringServiceImplNew implements MeteringServiceNew {
  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImplNew.class);

  public static DataBrokerService rmqService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  Supplier<Long> epochSupplier = () -> LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
  Supplier<String> isoTimeSupplier =
      () -> ZonedDateTime.now(ZoneId.of(ZoneId.SHORT_IDS.get("IST"))).toString();
  Supplier<String> primaryKeySuppler = () -> UUID.randomUUID().toString().replace("-", "");
  private MeteringLogBuilder meteringLog;

  public MeteringServiceImplNew(DataBrokerService dataBrokerService) {
    this.rmqService = dataBrokerService;
  }

  @Override
  public Future<Void> publishMeteringLogMessage(AuthInfo authInfo, long responseSize) {
    Promise<Void> promise = Promise.promise();
    MeteringLogBuilder meteringLogMessage = createMeteringLog(authInfo, responseSize);
    rmqService.publishMessage(
        meteringLogMessage.toJson(),
        EXCHANGE_NAME,
        ROUTING_KEY,
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

  private MeteringLogBuilder createMeteringLog(AuthInfo authInfo, long responseSize) {
    String delegatorId = getDelegatorId(authInfo);
    String event = geEvent(authInfo);
    return meteringLog =
        new MeteringLogBuilder.Builder()
            .forUserId(authInfo.getUserid())
            .forResourceId(authInfo.getResourceId())
            .forResourceGroup(authInfo.getResourceGroup())
            .forApi(authInfo.getApi())
            .forEvent(event)
            .forType("RESOURCE")
            .withPrimaryKey(primaryKeySuppler.get())
            .withProviderId(authInfo.getProviderId())
            .withDelegatorId(delegatorId)
            .withResponseSize(responseSize)
            .atEpoch(epochSupplier.get())
            .atIsoTime(isoTimeSupplier.get())
            .forOrigin(ORIGIN_SERVER)
            .build();
  }

  private String getDelegatorId(AuthInfo authInfo) {
    String delegatorId;
    if (authInfo.getRole().getRole().equalsIgnoreCase("delegate") && authInfo.getDrl() != null) {
      delegatorId = authInfo.getDid();
    } else {
      delegatorId = authInfo.getUserid();
    }
    return delegatorId;
  }

  private String geEvent(AuthInfo authInfo) {
    String event = null;
    if (authInfo.getEndPoint().contains("/ngsi-ld/v1/subscription")) {
      event = "subscriptions";
    }
    return event;
  }
}
