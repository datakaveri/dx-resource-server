package org.cdpg.dx.catalogue.client;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

public class CatalogueClientImpl implements CatalogueClient{
    private WebClient webClient;
    private String catHost;
    private int catPort;
    private String catBasePath;
    public CatalogueClientImpl(String catServerHost, Integer catServerPort, String dxCatalogueBasePath, WebClient webClient) {
        this.webClient = webClient;
        this.catHost = catServerHost;
        this.catPort = catServerPort;
        this.catBasePath = dxCatalogueBasePath;
    }

    @Override
    public Future<JsonObject> fetchCatalogueData(String id) {
        return null;
    }
}
