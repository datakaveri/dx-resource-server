package org.cdpg.dx.rs.subscription.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.databroker.util.Vhosts;

public class HelperDataBrokerMethod {
    DataBrokerService dataBrokerService;
    public HelperDataBrokerMethod(DataBrokerService dataBrokerService){
        this.dataBrokerService = dataBrokerService;
    }

    public Future<Void> createExchange(){
        Promise<Void> promise = Promise.promise();
        dataBrokerService.registerExchange("fd47486b-3497-4248-ac1e-082e4d37a66c","8b95ab80-2aaf-4636-a65e-7f2563d0d371", Vhosts.IUDX_PROD)
                .onSuccess(success -> promise.complete()).onFailure(promise::fail);
        return promise.future();
    }

}
