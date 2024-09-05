package iudx.resource.server.metering.consumeddata;

import io.vertx.core.Future;
import iudx.resource.server.metering.model.ConsumedDataInfo;
import iudx.resource.server.metering.model.MeteringCountRequest;

public interface MeteringService {

  Future<ConsumedDataInfo> getConsumedData(MeteringCountRequest meteringCountRequest);
}
