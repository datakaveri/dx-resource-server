package iudx.resource.server.authenticator.authorization;

import static iudx.resource.server.apiserver.util.Constants.RESET_PWD;
import static iudx.resource.server.authenticator.authorization.Method.DELETE;
import static iudx.resource.server.authenticator.authorization.Method.GET;
import static iudx.resource.server.authenticator.authorization.Method.PATCH;
import static iudx.resource.server.authenticator.authorization.Method.POST;
import static iudx.resource.server.authenticator.authorization.Method.PUT;
import static iudx.resource.server.common.ResponseUrn.*;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsumerAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ConsumerAuthStrategy.class);
  static Map<String, List<AuthorizationRequest>> consumerAuthorizationRules = new HashMap<>();
  private static volatile ConsumerAuthStrategy instance;
  private final boolean isLimitsEnabled;

  private ConsumerAuthStrategy(boolean isLimitsAllowed, Api api) {
    this.isLimitsEnabled = isLimitsAllowed;
    buildPermissions(api);
  }

  public static ConsumerAuthStrategy getInstance(boolean isLimitsAllowed, Api api) {
    if (instance == null) {
      synchronized (ConsumerAuthStrategy.class) {
        if (instance == null) {
          instance = new ConsumerAuthStrategy(isLimitsAllowed, api);
        }
      }
    }
    return instance;
  }

  private void buildPermissions(Api api) {

    // api access list/rules
    List<AuthorizationRequest> apiAccessList = new ArrayList<>();
    apiAccessList.add(new AuthorizationRequest(GET, api.getEntitiesUrl()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getTemporalUrl()));
    apiAccessList.add(new AuthorizationRequest(POST, api.getPostEntitiesQueryPath()));
    apiAccessList.add(new AuthorizationRequest(POST, api.getPostTemporalQueryPath()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getIudxConsumerAuditUrl()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getMonthlyOverview()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getSummaryPath()));
    consumerAuthorizationRules.put(IudxAccess.API.getAccess(), apiAccessList);

    // subscriptions access list/rules
    List<AuthorizationRequest> subsAccessList = new ArrayList<>();
    subsAccessList.add(new AuthorizationRequest(GET, api.getSubscriptionUrl()));
    subsAccessList.add(new AuthorizationRequest(POST, api.getSubscriptionUrl()));
    subsAccessList.add(new AuthorizationRequest(DELETE, api.getSubscriptionUrl()));
    subsAccessList.add(new AuthorizationRequest(PUT, api.getSubscriptionUrl()));
    subsAccessList.add(new AuthorizationRequest(PATCH, api.getSubscriptionUrl()));
    consumerAuthorizationRules.put(IudxAccess.SUBSCRIPTION.getAccess(), subsAccessList);

    // management
    List<AuthorizationRequest> mgmtAccessList = new ArrayList<>();
    mgmtAccessList.add(new AuthorizationRequest(POST, RESET_PWD));
    consumerAuthorizationRules.put(IudxAccess.MANAGEMENT.getAccess(), mgmtAccessList);

    // async access list
    List<AuthorizationRequest> asyncAccessList = new ArrayList<>();
    asyncAccessList.add(new AuthorizationRequest(GET, api.getIudxAsyncSearchApi()));
    asyncAccessList.add(new AuthorizationRequest(GET, api.getIudxAsyncStatusApi()));
    consumerAuthorizationRules.put(IudxAccess.ASYNC.getAccess(), asyncAccessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    JsonObject access =
        jwtData.getCons() != null ? jwtData.getCons().getJsonObject("access") : null;

    if (access == null) {
      return false;
    }
    String endpoint = authRequest.getApi();
    Method method = authRequest.getMethod();
    LOGGER.info("authorization request for : " + endpoint + " with method : " + method.name());
    LOGGER.info("allowed access : " + access);
    boolean result = false;
    if (!result && access.containsKey(IudxAccess.API.getAccess())) {
      result = consumerAuthorizationRules.get(IudxAccess.API.getAccess()).contains(authRequest);
    }
    if (!result && access.containsKey(IudxAccess.SUBSCRIPTION.getAccess())) {
      result =
          consumerAuthorizationRules.get(IudxAccess.SUBSCRIPTION.getAccess()).contains(authRequest);
    }
    if (!result) {
      result =
          consumerAuthorizationRules.get(IudxAccess.MANAGEMENT.getAccess()).contains(authRequest);
    }
    if (!result) {
      result = consumerAuthorizationRules.get(IudxAccess.ASYNC.getAccess()).contains(authRequest);
    }
    return result;
  }

  @Override
  public boolean isAuthorized(
      AuthorizationRequest authRequest, JwtData jwtData, JsonObject quotaConsumed) {
    JsonObject access =
        jwtData.getCons() != null ? jwtData.getCons().getJsonObject("access") : null;

    if (access == null) {
      return false;
    }
    String endpoint = authRequest.getApi();
    Method method = authRequest.getMethod();
    LOGGER.info("authorization request for : " + endpoint + " with method : " + method.name());
    LOGGER.info("allowed access : " + access);
    boolean result = false;
    if (!result && access.containsKey(IudxAccess.API.getAccess())) {
      result =
          consumerAuthorizationRules.get(IudxAccess.API.getAccess()).contains(authRequest)
              && isUsageWithinLimits(access.getJsonObject("api"), quotaConsumed, "api");
    }
    if (!result && access.containsKey(IudxAccess.SUBSCRIPTION.getAccess())) {
      result =
          consumerAuthorizationRules.get(IudxAccess.SUBSCRIPTION.getAccess()).contains(authRequest)
              && isUsageWithinLimits(access.getJsonObject("sub"), quotaConsumed, "sub");
    }
    if (!result) {
      result =
          consumerAuthorizationRules.get(IudxAccess.MANAGEMENT.getAccess()).contains(authRequest);
    }
    if (!result) {
      result =
          consumerAuthorizationRules.get(IudxAccess.ASYNC.getAccess()).contains(authRequest)
              && isUsageWithinLimits(access.getJsonObject("async"), quotaConsumed, "async");
    }
    return result;
  }

  private boolean isUsageWithinLimits(JsonObject access, JsonObject quotaConsumed, String type) {
    LOGGER.info("access: {} type: {} ", access, type);
    boolean isUsageWithinLimits = false;
    LOGGER.info("quotaConsumed: {} ", quotaConsumed);
    int allowedLimit = access.getInteger("limit");

    int consumedData = quotaConsumed.getInteger("consumed_data");
    int apiCount = quotaConsumed.getInteger("api_count");

    if (type.equalsIgnoreCase("api")) {
      if (apiCount < allowedLimit) {
        isUsageWithinLimits = true;
      } else {
        Response response =
            new Response.Builder()
                .withUrn(ResponseUrn.LIMIT_EXCEED_URN.getUrn())
                .withStatus(429)
                .withTitle("Too Many Requests")
                .withDetail(LIMIT_EXCEED_URN.getMessage())
                .build();
        throw new RuntimeException(response.toString());
      }
    }
    if (type.equalsIgnoreCase("sub") || type.equalsIgnoreCase("async")) {
      if (consumedData < allowedLimit) {
        isUsageWithinLimits = true;
      } else {
        Response response =
            new Response.Builder()
                .withUrn(ResponseUrn.LIMIT_EXCEED_URN.getUrn())
                .withStatus(429)
                .withTitle("Too Many Requests")
                .withDetail(LIMIT_EXCEED_URN.getMessage())
                .build();
        throw new RuntimeException(response.toString());
      }
    }
    LOGGER.info("usage limits {} defined limits", isUsageWithinLimits ? "within" : "exceeds");
    return isUsageWithinLimits;
  }
}
