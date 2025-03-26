package org.cdpg.dx.catalogue.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.catalogue.client.CatalogueClient;


public class CatalogueServiceImpl implements CatalogueService{
    private CatalogueClient catalogueClient;
    public CatalogueServiceImpl(CatalogueClient catalogueClient){
        this.catalogueClient = catalogueClient;
    }

    @Override
    public Future<JsonObject> fetchCatalogueInfo(String id) {
        return null;
    }
}
