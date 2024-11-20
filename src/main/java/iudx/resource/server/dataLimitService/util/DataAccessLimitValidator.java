package iudx.resource.server.dataLimitService.util;


import iudx.resource.server.dataLimitService.model.ConsumedDataInfo;
import iudx.resource.server.dataLimitService.model.UserAuthLimitInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataAccessLimitValidator {
  private static final Logger LOGGER = LogManager.getLogger(DataAccessLimitValidator.class);

  public boolean isUsageWithinLimits(UserAuthLimitInfo userAuthLimitInfo, ConsumedDataInfo quotaConsumed) {

    if (!isLimitEnabled(userAuthLimitInfo)) {
      return true;
    }

    String accessType = userAuthLimitInfo.getAccessPolicy();


    long allowedLimit = userAuthLimitInfo.getAccess().getJsonObject(accessType).getLong("limit");

    LOGGER.info("Access type: {}, Allowed limit: {}", accessType, allowedLimit);

    boolean isWithinLimits;

    switch (accessType.toLowerCase()) {
      case "api":
        isWithinLimits = quotaConsumed.getApiCount() < allowedLimit;
        break;
      case "async":
        isWithinLimits = quotaConsumed.getConsumedData() < allowedLimit;
        break;
      case "sub":
        isWithinLimits = true;
        break;
      default:
        isWithinLimits = false; // Handle unexpected accessType cases if needed
        break;
    }

    LOGGER.info("Usage {} defined limits", isWithinLimits ? "within" : "exceeds");
    return isWithinLimits;
  }

  private static boolean isLimitEnabled(UserAuthLimitInfo userAuthLimitInfo) {
    return "CONSUMER".equalsIgnoreCase(userAuthLimitInfo.getRole().getRole())
        && !"OPEN".equalsIgnoreCase(userAuthLimitInfo.getAccessPolicy());
  }
}
