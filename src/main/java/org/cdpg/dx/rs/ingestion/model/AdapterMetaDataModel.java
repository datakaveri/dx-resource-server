package org.cdpg.dx.rs.ingestion.model;

import io.vertx.core.json.JsonObject;
import org.cdpg.dx.databroker.model.RegisterExchangeModel;

public record AdapterMetaDataModel(RegisterExchangeModel registerExchange, JsonObject catalogueResult, String routingKey) {}
