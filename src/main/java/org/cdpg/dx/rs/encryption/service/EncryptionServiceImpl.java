package org.cdpg.dx.encryption.service;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.exceptions.SodiumException;
import com.goterl.lazysodium.interfaces.Box;
import com.goterl.lazysodium.utils.Key;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.cdpg.dx.encryption.util.UrlBase64MessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

import static org.cdpg.dx.encryption.util.Constants.*;


public class EncryptionServiceImpl implements EncryptionService {
  private static final Logger LOG = LoggerFactory.getLogger(EncryptionServiceImpl.class);
  private Box.Lazy box;

  public EncryptionServiceImpl() {
    LazySodiumJava lazySodiumJava =
        new LazySodiumJava(new SodiumJava(), new UrlBase64MessageEncoder());
    this.box = (Box.Lazy) lazySodiumJava;
  }

  public EncryptionServiceImpl(Box.Lazy box) {
    this.box = box;
  }

  @Override
  public Future<String> encrypt(String message, String encodedPublicKey) {
    Promise<String> promise = Promise.promise();
    if (message == null || message.isEmpty()) {
      promise.fail("message is null or empty");
    }
    if (encodedPublicKey != null
        && encodedPublicKey != null
        && !encodedPublicKey.isEmpty()) {
      JsonObject result = new JsonObject();
      // decode public key
      try {
        Key key = decodePublicKey(encodedPublicKey);
        String cipherText = box.cryptoBoxSealEasy(message, key);

      } catch (IllegalArgumentException illegalArgumentException) {
        LOG.error("Exception while decoding the public key: " + illegalArgumentException);
        LOG.error("The public key should be in URL Safe base64 format");
        promise.fail("Exception while decoding public key: " + illegalArgumentException);
      } catch (SodiumException e) {
        LOG.error("Sodium Exception: " + e);
        promise.fail("Sodium exception: " + e);
      }
      promise.tryComplete(cipherText);
    } else {
      LOG.error("public key is empty or null");
      promise.tryFail("Public key is empty or null");
    }
    return promise.future();
  }

  public Key decodePublicKey(String encodedPublicKey) {
    if (encodedPublicKey == null || encodedPublicKey.isEmpty()) {
      return null;
    }
    String publicKey = encodedPublicKey;
    byte[] bytes = Base64.getUrlDecoder().decode(publicKey);
    return Key.fromBytes(bytes);
  }
}
