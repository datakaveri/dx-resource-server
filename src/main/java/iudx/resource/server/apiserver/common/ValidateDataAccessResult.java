package iudx.resource.server.apiserver.common;

import iudx.resource.server.metering.model.ConsumedDataInfo;

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
