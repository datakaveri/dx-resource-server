package iudx.resource.server.encryption;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.resource.server.encryption.service.EncryptionService;
import iudx.resource.server.encryption.service.EncryptionServiceImpl;

/**
 *
 *
 * <h2>EncryptionVerticle</h2>
 *
 * <p>EncryptionVerticle exposes the Encryption Service over Vert.x service proxy {@link
 * EncryptionVerticle}
 */
public class EncryptionVerticle extends AbstractVerticle {

  private static final String ENCRYPTION_SERVICE_ADDRESS = "iudx.rs.encryption.service";

  @Override
  public void start() {
    EncryptionService encryptionService = new EncryptionServiceImpl();
    new ServiceBinder(vertx)
        .setAddress(ENCRYPTION_SERVICE_ADDRESS)
        .register(EncryptionService.class, encryptionService);
  }

  @Override
  public void stop() throws Exception {}
}
