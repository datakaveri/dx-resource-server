package iudx.resource.server.metering.consumeddata;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.metering.consumeddata.util.QueryBuilder;
import iudx.resource.server.metering.model.ConsumedDataInfo;
import iudx.resource.server.metering.model.MeteringCountRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MeteringServiceImpl implements MeteringService {

  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);
  private final Vertx vertx;
  private final QueryBuilder queryBuilder = new QueryBuilder();
  private final PostgresService postgresService;


  public MeteringServiceImpl(
      Vertx vertxInstance, PostgresService postgresService) {
    this.vertx = vertxInstance;
    this.postgresService = postgresService;
  }


  private Future<JsonObject> executeQueryDatabaseOperation(String query) {
    Promise<JsonObject> promise = Promise.promise();
    postgresService.executeQuery(
        query,
        dbHandler -> {
          if (dbHandler.succeeded()) {
            promise.complete(dbHandler.result());
          } else {
            promise.fail(dbHandler.cause().getMessage());
          }
        });

    return promise.future();
  }

  @Override
  public Future<ConsumedDataInfo> getConsumedData(MeteringCountRequest meteringCountRequest) {
    Promise <ConsumedDataInfo> promise = Promise.promise();
    LOGGER.debug("request data: {}", meteringCountRequest);
    String query = queryBuilder.getConsumedDataQuery(meteringCountRequest);
    LOGGER.debug("getConsumedData query: {}", query);
    executeQueryDatabaseOperation(query)
            .onSuccess(dbResult->{
                JsonObject result = dbResult.getJsonArray("result").getJsonObject(0);
                ConsumedDataInfo consumedDataInfo = new ConsumedDataInfo();
                consumedDataInfo.setConsumedData(result.getLong("consumed_data"));
                consumedDataInfo.setApiCount(result.getLong("api_count"));
                promise.complete(consumedDataInfo);
            })
            .onFailure(
                    failureHandler -> {
                      LOGGER.error("getConsumedData failed : ", failureHandler);
                      promise.fail(failureHandler);
                    });
      return promise.future();
  }
}
