package iudx.resource.server.dataLimitService.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import iudx.resource.server.dataLimitService.model.ConsumedDataInfo;
import iudx.resource.server.dataLimitService.model.RedisCountRequest;
import iudx.resource.server.dataLimitService.model.UserAuthLimitInfo;
import iudx.resource.server.dataLimitService.util.DataAccessLimitValidator;
import iudx.resource.server.dataLimitService.util.ValidateDataAccessResult;
import iudx.resource.server.database.redis.RedisService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class DataAccessLimitService {

    private static final Logger LOGGER = LogManager.getLogger(DataAccessLimitService.class);

    private final boolean isLimitEnabled;
    private final RedisService redisService;
    private final Vertx vertx;
    private final DataAccessLimitValidator dataAccessLimitValidator;


    public DataAccessLimitService(Vertx vertx, boolean isLimitEnabled,RedisService redisService) {
        this.isLimitEnabled = isLimitEnabled;
        this.vertx = vertx;
        this.dataAccessLimitValidator = new DataAccessLimitValidator();
        this.redisService = redisService;
    }

    public Future<ValidateDataAccessResult> validateDataAccess(UserAuthLimitInfo userAuthLimitInfo) {
        Promise<ValidateDataAccessResult> validateDataAccessResultPromise = Promise.promise();
        // If limits are not enabled or user is not a consumer, bypass limit check
        if (!isLimitEnabled || !isConsumer(userAuthLimitInfo)) {
            validateDataAccessResultPromise.complete(createUnlimitedAccessResult());
            return validateDataAccessResultPromise.future();
        }

        RedisCountRequest redisCountRequest = createRedisCountRequest(userAuthLimitInfo);
        // Compose blocks for future chaining
        return getConsumedInfo(redisCountRequest)
                .compose(consumedData -> validateLimits(userAuthLimitInfo, consumedData))
                .onSuccess(validateResult -> {
                    LOGGER.info(validateResult.isWithInLimit() ? "User access is allowed." : "Limit Exceeded");
                    validateDataAccessResultPromise.complete(validateResult);
                })
                .onFailure(failure -> {
                    LOGGER.error("Failed to validate data access: {}", failure.getMessage());
                    validateDataAccessResultPromise.fail(failure.getMessage());
                });
    }

    private boolean isConsumer(UserAuthLimitInfo userAuthLimitInfo) {
        return userAuthLimitInfo.getRole().getRole().equalsIgnoreCase("consumer");
    }

    private RedisCountRequest createRedisCountRequest(UserAuthLimitInfo userAuthLimitInfo) {
        RedisCountRequest redisCountRequest = new RedisCountRequest();
        redisCountRequest.setUserid(userAuthLimitInfo.getUserId());
        redisCountRequest.setResourceId(userAuthLimitInfo.getResourceId());
        redisCountRequest.setRedisKeys();
        LOGGER.trace("Redis count request: {}", redisCountRequest);
        return redisCountRequest;
    }

    private Future<ConsumedDataInfo> getConsumedInfo(RedisCountRequest redisCountRequest) {
        Promise<ConsumedDataInfo> promise = Promise.promise();

        // Prepare Redis keys
        List<String> keyList = new ArrayList<>();
        keyList.add(redisCountRequest.getApiCountKey());
        keyList.add(redisCountRequest.getTotalSizeKey());

        // Fetch data from Redis
        redisService.getFromRedis(keyList).onSuccess(responseModel -> {
            try {
                // Parse values from Redis result
                LOGGER.info("FJOF "+ responseModel.getValueFromKey(redisCountRequest.getApiCountKey()));
                long apiCount = parseLongOrDefault(responseModel.getValueFromKey(redisCountRequest.getApiCountKey()));
                long consumedData = parseLongOrDefault(responseModel.getValueFromKey(redisCountRequest.getTotalSizeKey()));
                LOGGER.info("apiCount: {}, consumedData: {}", apiCount, consumedData);

                // Populate ConsumedDataInfo and complete the promise
                ConsumedDataInfo consumedDataInfo = new ConsumedDataInfo();
                consumedDataInfo.setApiCount(apiCount);
                consumedDataInfo.setConsumedData(consumedData);
                promise.complete(consumedDataInfo);
            } catch (NumberFormatException e) {
                // Handle parsing errors and fail the promise
                LOGGER.error("Failed to parse Redis values: {}", e.getLocalizedMessage());
                promise.fail("Error parsing Redis values: " + e.getMessage());
            }
        }).onFailure(promise::fail);  // Handle Redis service failure

        return promise.future();
    }


    private Future<ValidateDataAccessResult> validateLimits(UserAuthLimitInfo userAuthLimitInfo, ConsumedDataInfo consumedData) {
        Promise<ValidateDataAccessResult> promise = Promise.promise();

        boolean isWithinLimits = dataAccessLimitValidator.isUsageWithinLimits(userAuthLimitInfo, consumedData);
        ValidateDataAccessResult result = new ValidateDataAccessResult();
        result.setConsumedDataInfo(consumedData);
        result.setWithInLimit(isWithinLimits);

        promise.complete(result);

        return promise.future();
    }

    private ValidateDataAccessResult createUnlimitedAccessResult() {
        ValidateDataAccessResult result = new ValidateDataAccessResult();
        result.setWithInLimit(true);
        return result;
    }

    private long parseLongOrDefault(Object value) throws NumberFormatException {
        LOGGER.info("VALUE "+value);
        if (value != null) {
            return Long.parseLong(value.toString()); // Let the exception propagate if parsing fails
        }
        throw new NumberFormatException("Value is null or cannot be parsed");
    }
}
