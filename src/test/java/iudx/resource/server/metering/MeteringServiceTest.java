package iudx.resource.server.metering;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import iudx.resource.server.cache.cachelmpl.CacheType;
import iudx.resource.server.configuration.Configuration;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import iudx.resource.server.databroker.RabbitClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.authenticator.Constants.ROLE;
import static iudx.resource.server.database.archives.Constants.ID;
import static iudx.resource.server.metering.util.Constants.API;
import static iudx.resource.server.metering.util.Constants.IID;
import static iudx.resource.server.metering.util.Constants.USER_ID;
import static iudx.resource.server.metering.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class MeteringServiceTest {

    private static final Logger LOGGER = LogManager.getLogger(MeteringServiceTest.class);
    public static String userId;
    public static String id;
    private static Vertx vertxObj;
    private static MeteringServiceImpl meteringService;
    private static Configuration config;
    private static PostgresService postgresService;

    @BeforeAll
    @DisplayName("Deploying Verticle")
    static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
        vertxObj = vertx;
        config = new Configuration();

        userId = UUID.randomUUID().toString();
        id = "89a36273d77dac4cf38114fca1bbe64392547f86";
        vertxTestContext.completeNow();
    }

    private JsonObject readConsumerRequest() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        jsonObject.put(RESOURCE_ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        jsonObject.put(START_TIME, "2022-05-29T05:30:00+05:30[Asia/Kolkata]");
        jsonObject.put(END_TIME, "2022-06-04T02:00:00+05:30[Asia/Kolkata]");
        jsonObject.put(TIME_RELATION, DURING);
        jsonObject.put(API, "/ngsi-ld/v1/subscription");
        jsonObject.put(ENDPOINT, "/ngsi-ld/v1/consumer/audit");

        return jsonObject;
    }

    private JsonObject readProviderRequest() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        jsonObject.put(RESOURCE_ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        jsonObject.put(START_TIME, "2022-05-29T05:30:00+05:30[Asia/Kolkata]");
        jsonObject.put(END_TIME, "2022-06-04T02:00:00+05:30[Asia/Kolkata]");
        jsonObject.put(TIME_RELATION, DURING);
        jsonObject.put(API, "/ngsi-ld/v1/subscription");
        jsonObject.put(PROVIDER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias");
        jsonObject.put(CONSUMER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        jsonObject.put(IID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        jsonObject.put(ENDPOINT, "/ngsi-ld/v1/provider/audit");
        jsonObject.put(ROLE,"provider");
        return jsonObject;
    }

    private JsonObject read() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put(START_TIME, "2022-06-20T00:00:00Z");
        jsonObject.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        jsonObject.put(END_TIME, "2022-06-21T16:00:00Z");
        jsonObject.put(TIME_RELATION, "between");
        jsonObject.put(API, "/ngsi-ld/v1/subscription");
        jsonObject.put(PROVIDER_ID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86");
        jsonObject.put(ENDPOINT, "/ngsi-ld/v1/provider/audit");
        jsonObject.put(IID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055");
        return jsonObject;
    }

    // @AfterAll
    // public void finish(VertxTestContext testContext) {
    // logger.info("finishing");
    // vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
    // }

    @Test
    @DisplayName("Testing read query with invalid time interval")
    void readFromInvalidTimeInterval(VertxTestContext testContext) {
        JsonObject request = readConsumerRequest();
        postgresService = mock(PostgresService.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);
        request.put(START_TIME, "2021-11-01T05:30:00+05:30[Asia/Kolkata]");
        request.put(END_TIME, "2021-11-01T02:00:00+05:30[Asia/Kolkata]");
        meteringService.executeReadQuery(
                request,
                testContext.failing(
                        response ->
                                testContext.verify(
                                        () -> {
                                            assertEquals(
                                                    INVALID_DATE_DIFFERENCE,
                                                    new JsonObject(response.getMessage()).getString(DETAIL));
                                            testContext.completeNow();
                                        })));
    }

    @Test
    @DisplayName("Testing read query with given Time Interval")
    void readFromValidTimeInterval(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json, responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(39);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        JsonObject request = readConsumerRequest();

        meteringService.executeReadQuery(
                request,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            LOGGER.info(response);
                                            assertEquals(SUCCESS, response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    @DisplayName("Testing read query for missing userId")
    void readForMissingUserId(VertxTestContext vertxTestContext) {
        JsonObject request = readConsumerRequest();
        request.remove(USER_ID);
        postgresService = mock(PostgresService.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        meteringService.executeReadQuery(
                request,
                vertxTestContext.failing(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(
                                                    USERID_NOT_FOUND,
                                                    new JsonObject(response.getMessage()).getString(DETAIL));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing read query for missing time Relation")
    void readForMissingTimeRel(VertxTestContext vertxTestContext) {
        JsonObject request = readConsumerRequest();
        request.remove(TIME_RELATION);
        postgresService = mock(PostgresService.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);
        meteringService.executeReadQuery(
                request,
                vertxTestContext.failing(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(
                                                    TIME_RELATION_NOT_FOUND,
                                                    new JsonObject(response.getMessage()).getString(DETAIL));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing read query for missing time")
    void readForMissingTime(VertxTestContext vertxTestContext) {
        JsonObject request = readConsumerRequest();
        request.remove(START_TIME);
        postgresService = mock(PostgresService.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);
        meteringService.executeReadQuery(
                request,
                vertxTestContext.failing(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(
                                                    TIME_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    @DisplayName("Testing read query with invalid start/end time")
    void readForInvalidStartTime(VertxTestContext vertxTestContext) {
        JsonObject request = readConsumerRequest();
        request.put(START_TIME, "2021-009-18T00:30:00+05:30[Asia/Kolkata]");
        postgresService = mock(PostgresService.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);
        meteringService.executeReadQuery(
                request,
                vertxTestContext.failing(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(
                                                    INVALID_DATE_TIME,
                                                    new JsonObject(response.getMessage()).getString(DETAIL));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing read query for given time.")
    void readForGivenTime(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json, responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(3);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        JsonObject jsonObject = read();
        jsonObject.remove(RESOURCE_ID);
        jsonObject.remove(API);
        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing read query for given time and id.")
    void readForGivenTimeAndId(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json, responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(60);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        JsonObject jsonObject = readConsumerRequest();
        jsonObject.remove(API);
        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing read query for given time and api.")
    void readForGivenTimeAndApi(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json, responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(300);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());
        JsonObject jsonObject = readConsumerRequest();
        jsonObject.remove(RESOURCE_ID);

        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing read query for given time,api and resourceId.")
    void readForGivenTimeApiAndID(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json, responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(34);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());
        JsonObject jsonObject = readConsumerRequest();

        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing count query for given time,api and id.")
    void countForGivenTimeAndApiAndID(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json, responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(23);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());
        JsonObject jsonObject = readConsumerRequest();
        jsonObject.put("options", "count");

        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertTrue(
                                                    response.getJsonArray(RESULTS).getJsonObject(0).containsKey(TOTAL));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing Write Query Failure")
    void writeDataFailure(VertxTestContext vertxTestContext) {
        JsonObject request = new JsonObject();
        ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        long time = zst.toInstant().toEpochMilli();
        String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
        request.put(EPOCH_TIME, time);
        request.put(ISO_TIME, isoTime);
        request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        request.put(ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        request.put(API, "/ngsi-ld/v1/subscription");
        request.put(RESPONSE_SIZE, 12);
        postgresService = mock(PostgresService.class);
        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        DataBrokerService dataBrokerService = mock(DataBrokerService.class);

        RabbitClient webClient = mock(RabbitClient.class);
        RabbitMQClient rabbitMQClient = mock(RabbitMQClient.class);
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);

        Future future = mock(Future.class);

        meteringService.insertMeteringValuesInRmq(
                request, handler -> {
                    if (handler.failed()) {
                        vertxTestContext.completeNow();
                    } else {
                        vertxTestContext.failNow("Failed");
                    }
                });
    }

    @Test
    @DisplayName("Testing Write Query Successful")
    void writeDataSuccessful(VertxTestContext vertxTestContext) {
        JsonObject request = new JsonObject();
        ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        long time = zst.toInstant().toEpochMilli();
        String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
        request.put(EPOCH_TIME, time);
        request.put(ISO_TIME, isoTime);
        request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        request.put(ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        request.put(API, "/ngsi-ld/v1/subscription");
        request.put(RESPONSE_SIZE, 12);
        postgresService = mock(PostgresService.class);
        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        MeteringServiceImpl.rmqService = mock(DataBrokerService.class);

        when(asyncResult.succeeded()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
                return null;
            }
        }).when(MeteringServiceImpl.rmqService).publishMessage(any(), anyString(), anyString(), any());

        meteringService.insertMeteringValuesInRmq(
                request, handler -> {
                    if (handler.succeeded()) {
                        vertxTestContext.completeNow();
                    } else {
                        vertxTestContext.failNow("Failed");
                    }
                });
    }


    @Test
    @DisplayName("Testing read query with missing providerId.")
    void readForMissingProviderId(VertxTestContext vertxTestContext) {
        postgresService = mock(PostgresService.class);
        meteringService = new MeteringServiceImpl( vertxObj, postgresService);
        JsonObject request = readProviderRequest();
        request.remove(PROVIDER_ID);
        meteringService.executeReadQuery(
                request,
                vertxTestContext.failing(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(
                                                    INVALID_PROVIDER_REQUIRED,
                                                    new JsonObject(response.getMessage()).getString(DETAIL));
                                            vertxTestContext.completeNow();
                                        })));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Testing read query with invalid providerId.")
    void readForInvalidProviderId(VertxTestContext vertxTestContext) {
        JsonObject request = readProviderRequest();
        request.put(PROVIDER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-tsst-alias");

        meteringService.executeReadQuery(
                request,
                vertxTestContext.failing(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(
                                                    INVALID_PROVIDER_ID,
                                                    new JsonObject(response.getMessage()).getString(DETAIL));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing read query for given time,api and id.")
    void readForGivenTimeApiIdConsumerProviderID(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json, responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(300);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());
        JsonObject jsonObject = readProviderRequest();

        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing read query for given time,api and id.")
    void readForGivenTimeApiIdConsumerProviderIDZero(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json, responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(0);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());
        JsonObject jsonObject = readProviderRequest();

        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString("title"));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing count query for given time,api and id.")
    void countForGivenTimeApiIdConsumerProviderID(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json, responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(10);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());
        JsonObject jsonObject = readProviderRequest();
        jsonObject.put("options", "count");

        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString("title"));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing read query for given time,api and providerId.")
    void readForGivenTimeApiAndProviderID(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json, responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(200);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        JsonObject jsonObject = readProviderRequest();
        jsonObject.remove(RESOURCE_ID);
        jsonObject.remove(CONSUMER_ID);

        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            LOGGER.debug("RESPONSE" + response);
                                            assertEquals(SUCCESS, response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("Testing count query for given time.")
    void readCountForGivenTime(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(300);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());
        JsonObject jsonObject = read();
        jsonObject.put("options", "count");
        jsonObject.remove(API);
        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString("title"));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    @DisplayName("Testing count query for given time.")
    void readCountForGivenTimeForZero(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(0);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());
        JsonObject jsonObject = read();
        jsonObject.put("options", "count");
        jsonObject.remove(API);
        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString("title"));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    public void testOverallMethodAdmin(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = new JsonObject().put("role", "admin");
        JsonObject expected = new JsonObject().put(SUCCESS, "count return");

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(expected);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        meteringService.monthlyOverview(
                json,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {

                                            System.out.println(response.toString());
                                            assertEquals("count return", response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    public void testOverallMethodConsumer(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = new JsonObject().put("role", "Consumer");
        json.put(IID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        json.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        JsonObject expected = new JsonObject().put(SUCCESS, "count return");

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(expected);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        meteringService.monthlyOverview(
                json,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {

                                            System.out.println(response.toString());
                                            assertEquals("count return", response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    public void testOverallMethodProvider(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = new JsonObject().put("role", "Provider");
        json.put(IID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        json.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");

        JsonObject expected = new JsonObject().put(SUCCESS, "count return");

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(expected);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        meteringService.monthlyOverview(
                json,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {

                                            System.out.println(response.toString());
                                            assertEquals("count return", response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    public void testOverallMethodAdminWithETST(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = new JsonObject().put("role", "admin");
        json.put(STARTT, "2022-11-20T00:00:00Z");
        json.put(ENDT, "2022-12-20T00:00:00Z");
        JsonObject expected = new JsonObject().put(SUCCESS, "count return");

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(expected);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        meteringService.monthlyOverview(
                json,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {

                                            System.out.println(response.toString());
                                            assertEquals("count return", response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    public void testOverallMethodConsumerWithSTET(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = new JsonObject().put("role", "Consumer");
        json.put(IID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        json.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        json.put(STARTT, "2022-11-20T00:00:00Z");
        json.put(ENDT, "2022-12-20T00:00:00Z");
        JsonObject expected = new JsonObject().put(SUCCESS, "count return");

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(expected);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        meteringService.monthlyOverview(
                json,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {

                                            System.out.println(response.toString());
                                            assertEquals("count return", response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    public void testOverallMethodProviderWithSTET(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = new JsonObject().put("role", "Provider");
        json.put(IID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        json.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        json.put(STARTT, "2022-11-20T00:00:00Z");
        json.put(ENDT, "2022-12-20T00:00:00Z");

        JsonObject expected = new JsonObject().put(SUCCESS, "count return");

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(expected);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        meteringService.monthlyOverview(
                json,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {

                                            System.out.println(response.toString());
                                            assertEquals("count return", response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    public void testForSummaryApi(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = new JsonObject().put("role", "consumer");
        json.put(IID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        json.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");

        JsonObject jsonObject = new JsonObject().put("resourceid", "datakaveri.org/0808eb81ea0e5773187ae06110f55915a55f5c05/rs.iudx.io/integration-test-rsg-one")
                .put("count", 5);
        JsonArray jsonArray = new JsonArray().add(jsonObject);

        JsonObject postgresJson = new JsonObject().put("type", "urn:dx:rs:success").put("title", "Success")
                .put("result", jsonArray);

        JsonObject cacheInteraction = new JsonObject();
        cacheInteraction.put("type", CacheType.CATALOGUE_CACHE);
        cacheInteraction.put("key", jsonArray.getJsonObject(0).getString("resourceid"));

        JsonObject outputFormat = new JsonObject()
                .put("resourceid", "datakaveri.org/0808eb81ea0e5773187ae06110f55915a55f5c05/rs.iudx.io/integration-test-rsg-one");
        JsonArray outputArray = new JsonArray().add(outputFormat);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        MeteringServiceImpl spyMeteringService = Mockito.spy(meteringService);


        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(postgresJson);
        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        doAnswer(Answer -> Future.succeededFuture(outputArray))
                .when(spyMeteringService).cacheCall(any());

        spyMeteringService.summaryOverview(
                json,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(response.getString("type"), "urn:dx:dm:Success");
                                            assertEquals(response.getString("title"), "Success");
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    public void testForSummaryApiWithSTET(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = new JsonObject().put("role", "consumer");
        json.put(IID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        json.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        json.put(STARTT, "2022-11-20T00:00:00Z");
        json.put(ENDT, "2022-12-20T00:00:00Z");

        JsonObject jsonObject = new JsonObject().put("resourceid", "datakaveri.org/0808eb81ea0e5773187ae06110f55915a55f5c05/rs.iudx.io/integration-test-rsg-one")
                .put("count", 5);
        JsonArray jsonArray = new JsonArray().add(jsonObject);

        JsonObject postgresJson = new JsonObject().put("type", "urn:dx:rs:success").put("title", "Success")
                .put("result", jsonArray);

        JsonObject cacheInteraction = new JsonObject();
        cacheInteraction.put("type", CacheType.CATALOGUE_CACHE);
        cacheInteraction.put("key", jsonArray.getJsonObject(0).getString("resourceid"));

        JsonObject outputFormat = new JsonObject()
                .put("resourceid", "datakaveri.org/0808eb81ea0e5773187ae06110f55915a55f5c05/rs.iudx.io/integration-test-rsg-one");
        JsonArray outputArray = new JsonArray().add(outputFormat);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        MeteringServiceImpl spyMeteringService = Mockito.spy(meteringService);


        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(postgresJson);
        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        doAnswer(Answer -> Future.succeededFuture(outputArray))
                .when(spyMeteringService).cacheCall(any());

        spyMeteringService.summaryOverview(
                json,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(response.getString("type"), "urn:dx:dm:Success");
                                            assertEquals(response.getString("title"), "Success");
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("summary api where count is zero")
    public void testForSummaryApiZeroValues(VertxTestContext vertxTestContext) {

        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.size()).thenReturn(0);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());
        JsonObject jsonObject = readProviderRequest();

        meteringService.summaryOverview(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString("title"));
                                            vertxTestContext.completeNow();
                                        })));

    }

    @Test
    @DisplayName("summary api for bad request")
    public void testForSummaryApiFail(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        Future future = mock(Future.class);
        Throwable throwable = mock(Throwable.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = new JsonObject().put("role", "consumer");
        json.put(IID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        json.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        json.put(STARTT, "2022-11-20T00:00:00Z");

        lenient().when(future.succeeded()).thenReturn(false);
        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        meteringService.summaryOverview(json, handler -> {
            if (handler.failed()) {
                assertEquals(handler.cause().getMessage(), "Bad Request");
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow("failed");
            }
        });

    }

    @Test
    @DisplayName("Overview api for bad request")
    public void testForOverviewApiFail(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        Future future = mock(Future.class);
        Throwable throwable = mock(Throwable.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = new JsonObject().put("role", "admin");
        json.put(IID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
        json.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
        json.put(STARTT, "2022-11-20T00:00:00Z");

        lenient().when(future.succeeded()).thenReturn(false);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        meteringService.monthlyOverview(json, handler -> {
            if (handler.failed()) {
                assertEquals(handler.cause().getMessage(), "Bad Request");
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow("failed");
            }
        });
    }


    @Test
    @DisplayName("Testing read query with given Time Interval")
    void readFromValidTimeIntervalWithLimitAndOffSet(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json, responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(39);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        JsonObject request = readConsumerRequest();
        request.put("limit", "110").put("offset", "0");

        meteringService.executeReadQuery(
                request,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            LOGGER.info(response);
                                            assertEquals(SUCCESS, response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));
    }

    @Test
    @DisplayName("Testing read query with given Time Interval")
    void readFromValidTimeIntervalWithLimitAndOffSetProvider(VertxTestContext vertxTestContext) {
        JsonObject responseJson = new JsonObject().put(SUCCESS, "Success");
        AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json = mock(JsonObject.class);
        JsonArray jsonArray = mock(JsonArray.class);

        meteringService = new MeteringServiceImpl( vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json, responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(39);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());

        JsonObject request = read();
        request.put("limit", "110").put("offset", "0");

        meteringService.executeReadQuery(
                request,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            LOGGER.info(response);
                                            assertEquals(SUCCESS, response.getString(SUCCESS));
                                            vertxTestContext.completeNow();
                                        })));
    }

}
