package iudx.resource.server.dataLimitService.util;

import iudx.resource.server.dataLimitService.model.ConsumedDataInfo;

public class ValidateDataAccessResult {
    private boolean withInLimit;
    ConsumedDataInfo consumedDataInfo;

    public boolean isWithInLimit() {
        return withInLimit;
    }

    public void setWithInLimit(boolean withInLimit) {
        this.withInLimit = withInLimit;
    }

    public ConsumedDataInfo getConsumedDataInfo() {
        return consumedDataInfo;
    }

    public void setConsumedDataInfo(ConsumedDataInfo consumedDataInfo) {
        this.consumedDataInfo = consumedDataInfo;
    }
}
