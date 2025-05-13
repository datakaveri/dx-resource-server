package org.cdpg.dx.rs.latest.service;

import io.vertx.core.Future;
import org.cdpg.dx.rs.latest.model.LatestData;

public interface LatestService {

    Future<LatestData> getLatestData(String rsId);
}
