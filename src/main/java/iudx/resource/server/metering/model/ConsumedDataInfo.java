package iudx.resource.server.metering.model;

public class ConsumedDataInfo {
    long consumedData;
    long apiCount;

    public long getConsumedData() {
        return consumedData;
    }

    public void setConsumedData(long consumedData) {
        this.consumedData = consumedData;
    }

    public long getApiCount() {
        return apiCount;
    }

    public void setApiCount(long apiCount) {
        this.apiCount = apiCount;
    }
}
