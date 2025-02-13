package iudx.resource.server.encryption.handler;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.encryption.service.EncryptionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EncryptionHandler implements Handler<RoutingContext> {
    //TODO: Need To work on it
    private static final Logger LOGGER = LogManager.getLogger(EncryptionHandler.class);
    boolean isEncryptionRequired;
    private EncryptionService encryptionService;
    private JsonObject jsonObject;

    public EncryptionHandler(EncryptionService encryptionService, boolean isEncryptionRequired,JsonObject jsonObject) {
        this.encryptionService = encryptionService;
        this.isEncryptionRequired = isEncryptionRequired;
        this.jsonObject = jsonObject;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.trace("EncryptionHandler started");

        if (isEncryptionRequired) {
            Future.future(encryption -> encryptionDone());
            LOGGER.info("");
            routingContext.next();

        } else {
            routingContext.next();
        }
    }

    private Future<JsonObject> encryptionDone() {
        //encryptionService.encrypt();
        return null;
    }
}
