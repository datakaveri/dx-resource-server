package iudx.resource.server.apiserver.common;

import iudx.resource.server.authenticator.model.AuthInfo;
import iudx.resource.server.metering.model.ConsumedDataInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataAccessLimitValidator {
  private static final Logger LOGGER = LogManager.getLogger(DataAccessLimitValidator.class);

  public static boolean isUsageWithinLimits(
      AuthInfo authInfo, ConsumedDataInfo consumedDataInfo, long currentSize, boolean isAudited) {
    LOGGER.trace("isAudited: {}, currentSize: {}", isAudited, currentSize);

    if (isAudited) {
      return true;
    }

    long updatedConsumedData = consumedDataInfo.getConsumedData() + currentSize;
    consumedDataInfo.setConsumedData(updatedConsumedData);

    return isUsageWithinLimits(authInfo, consumedDataInfo);
  }

  public static boolean isUsageWithinLimits(AuthInfo authInfo, ConsumedDataInfo quotaConsumed) {
    if (!isLimitEnabled(authInfo)) {
      return true;
    }

    String accessType = authInfo.getAccessPolicy();
    long allowedLimit = authInfo.getAccess().getJsonObject(accessType).getLong("limit");
    LOGGER.debug("Access type: {}, Allowed limit: {}", accessType, allowedLimit);

    boolean isWithinLimits = false;

    if ("api".equalsIgnoreCase(accessType)) {
      isWithinLimits = quotaConsumed.getApiCount() <= allowedLimit;
    } else if ("async".equalsIgnoreCase(accessType)) {
      isWithinLimits = quotaConsumed.getConsumedData() <= allowedLimit;
    } else if ("sub".equalsIgnoreCase(accessType)) {
      isWithinLimits = true;
    }

    LOGGER.info("Usage {} defined limits", isWithinLimits ? "within" : "exceeds");
    return isWithinLimits;
  }

  private static boolean isLimitEnabled(AuthInfo authInfo) {
    return "CONSUMER".equalsIgnoreCase(authInfo.getRole().getRole())
        && !"OPEN".equalsIgnoreCase(authInfo.getAccessPolicy());
  }
}
