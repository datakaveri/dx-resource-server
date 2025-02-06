package iudx.resource.server.encryption;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.resource.server.encryption.EncryptionService;
import iudx.resource.server.encryption.EncryptionServiceImpl;

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
    iudx.resource.server.encryption.EncryptionService encryptionService = new EncryptionServiceImpl();
    new ServiceBinder(vertx)
        .setAddress(ENCRYPTION_SERVICE_ADDRESS)
        .register(EncryptionService.class, encryptionService);
  }
}
