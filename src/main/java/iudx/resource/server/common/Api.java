package iudx.resource.server.common;

import static iudx.resource.server.apiserver.async.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.*;

public class Api {

  private static volatile Api apiInstance;
  private final String dxApiBasePath;
  private StringBuilder entitiesUrl;
  private StringBuilder temporalUrl;
  private StringBuilder subscriptionUrl;
  private StringBuilder postTemporalQueryPath;
  private StringBuilder postEntitiesQueryPath;
  private StringBuilder iudxAsyncSearchApi;
  private StringBuilder iudxAsyncStatusApi;
  private StringBuilder iudxConsumerAuditUrl;
  private StringBuilder iudxProviderAuditUrl;
  private StringBuilder iudxManagementAdapterUrl;
  private StringBuilder ingestionPath;
  private StringBuilder resetPassword;
  private StringBuilder asyncPath;
  private StringBuilder monthlyOverview;
  private StringBuilder summaryPath;
  private StringBuilder ingestionPathEntities;

  private Api(String dxApiBasePath) {
    this.dxApiBasePath = dxApiBasePath;
    buildPaths();
  }

  public static Api getInstance(String dxApiBasePath) {
    if (apiInstance == null) {
      synchronized (Api.class) {
        if (apiInstance == null) {
          apiInstance = new Api(dxApiBasePath);
        }
      }
    }
    return apiInstance;
  }

  public String getIngestionPathEntities() {
    return ingestionPathEntities.toString();
  }

  private void buildPaths() {
    entitiesUrl = new StringBuilder(dxApiBasePath).append(NGSILD_ENTITIES_URL);
    temporalUrl = new StringBuilder(dxApiBasePath).append(NGSILD_TEMPORAL_URL);
    subscriptionUrl = new StringBuilder(dxApiBasePath).append(NGSILD_SUBSCRIPTION_URL);
    postTemporalQueryPath =
        new StringBuilder(dxApiBasePath).append(NGSILD_POST_TEMPORAL_QUERY_PATH);
    postEntitiesQueryPath =
        new StringBuilder(dxApiBasePath).append(NGSILD_POST_ENTITIES_QUERY_PATH);
    iudxAsyncSearchApi = new StringBuilder(dxApiBasePath).append(IUDX_ASYNC_SEARCH_API);
    iudxConsumerAuditUrl = new StringBuilder(dxApiBasePath).append(IUDX_CONSUMER_AUDIT_URL);
    iudxProviderAuditUrl = new StringBuilder(dxApiBasePath).append(IUDX_PROVIDER_AUDIT_URL);
    iudxManagementAdapterUrl = new StringBuilder(dxApiBasePath).append(IUDX_MANAGEMENT_ADAPTER_URL);
    ingestionPath = new StringBuilder(dxApiBasePath).append(INGESTION_PATH);
    asyncPath = new StringBuilder(dxApiBasePath).append(ASYNC);
    iudxAsyncStatusApi = new StringBuilder(dxApiBasePath).append(ASYNC + STATUS);
    resetPassword = new StringBuilder(dxApiBasePath).append(RESET_PWD);
    monthlyOverview = new StringBuilder(dxApiBasePath).append(MONTHLY_OVERVIEW);
    summaryPath = new StringBuilder(dxApiBasePath).append(SUMMARY_ENDPOINT);
    ingestionPathEntities = new StringBuilder(dxApiBasePath).append(INGESTION_PATH_ENTITIES);
  }

  public String getEntitiesUrl() {
    return entitiesUrl.toString();
  }

  public String getTemporalUrl() {
    return temporalUrl.toString();
  }

  public String getSubscriptionUrl() {
    return subscriptionUrl.toString();
  }

  public String getPostTemporalQueryPath() {
    return postTemporalQueryPath.toString();
  }

  public String getPostEntitiesQueryPath() {
    return postEntitiesQueryPath.toString();
  }

  public String getIudxAsyncSearchApi() {
    return iudxAsyncSearchApi.toString();
  }

  public String getIudxAsyncStatusApi() {
    return iudxAsyncStatusApi.toString();
  }

  public String getIudxConsumerAuditUrl() {
    return iudxConsumerAuditUrl.toString();
  }

  public String getIudxProviderAuditUrl() {
    return iudxProviderAuditUrl.toString();
  }

  public String getIudxManagementAdapterUrl() {
    return iudxManagementAdapterUrl.toString();
  }

  public String getIngestionPath() {
    return ingestionPath.toString();
  }

  public String getAsyncPath() {
    return asyncPath.toString();
  }

  public String getManagementBasePath() {
    return resetPassword.toString();
  }

  public String getMonthlyOverview() {
    return monthlyOverview.toString();
  }

  public String getSummaryPath() {
    return summaryPath.toString();
  }
}
