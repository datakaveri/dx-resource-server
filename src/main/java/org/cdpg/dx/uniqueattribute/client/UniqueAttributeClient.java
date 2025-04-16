package org.cdpg.dx.uniqueattribute.client;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import java.util.List;

public interface UniqueAttributeClient {
    Future<List<JsonObject>> fetchUniqueAttribute();
}
