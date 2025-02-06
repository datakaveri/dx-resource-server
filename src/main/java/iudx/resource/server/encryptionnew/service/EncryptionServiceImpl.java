package iudx.resource.server.encryptionnew.service;

import static iudx.resource.server.encryption.util.Constants.*;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.exceptions.SodiumException;
import com.goterl.lazysodium.interfaces.Box;
import com.goterl.lazysodium.utils.Key;
import com.goterl.lazysodium.utils.KeyPair;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import iudx.resource.server.encryption.util.UrlBase64MessageEncoder;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public Future<JsonObject> encrypt(String message, JsonObject encodedPublicKey) {
    Promise<JsonObject> promise = Promise.promise();
    if (message == null || message.isEmpty()) {
      promise.fail("message is null or empty");
    }
    if (encodedPublicKey != null
        && encodedPublicKey.getString(ENCODED_KEY) != null
        && !encodedPublicKey.getString(ENCODED_KEY).isEmpty()) {
      JsonObject result = new JsonObject();
      // decode public key
      try {
        Key key = decodePublicKey(encodedPublicKey);
        String cipherText = box.cryptoBoxSealEasy(message, key);
        result.put(ENCODED_CIPHER_TEXT, cipherText);
      } catch (IllegalArgumentException illegalArgumentException) {
        LOG.error("Exception while decoding the public key: " + illegalArgumentException);
        LOG.error("The public key should be in URL Safe base64 format");
        promise.fail("Exception while decoding public key: " + illegalArgumentException);
      } catch (SodiumException e) {
        LOG.error("Sodium Exception: " + e);
        promise.fail("Sodium exception: " + e);
      }
      promise.tryComplete(result);
    } else {
      LOG.error("public key is empty or null");
      promise.tryFail("Public key is empty or null");
    }
    return promise.future();
  }

  @Override
  public Future<JsonObject> decrypt(String encodedCipherText, JsonObject keyPair) {
    if (encodedCipherText == null || encodedCipherText.isEmpty()) {
      return Future.failedFuture("encoded cipher text is null or empty");
    }
    if (keyPair == null || keyPair.getValue(KEYPAIR) == null) {
      return Future.failedFuture("key pair is null");
    }
    JsonObject result = new JsonObject();
    try {
      KeyPair keys = (KeyPair) keyPair.getValue(KEYPAIR);
      String message = box.cryptoBoxSealOpenEasy(encodedCipherText, keys);
      result.put("message", message);
    } catch (IllegalArgumentException exception) {
      LOG.error("IllegalArgumentException: " + exception);
      return Future.failedFuture("IllegalArgumentException: " + exception);
    } catch (SodiumException e) {
      LOG.error("Sodium Exception: " + e);
      return Future.failedFuture("Sodium Exception");
    } catch (Exception e) {
      LOG.error("Exception: " + e);
      return Future.failedFuture("Exception : " + e);
    }
    return Future.succeededFuture(result);
  }

  @Override
  public Future<JsonObject> encodePublicKey(JsonObject publicKey) {
    if (publicKey != null
        && publicKey.getString(PUBLIC_KEY) != null
        && !publicKey.getString(PUBLIC_KEY).isEmpty()) {
      byte[] bytes = publicKey.getBinary(PUBLIC_KEY);
      String encodedKey = Base64.getUrlEncoder().encodeToString(bytes);
      JsonObject result = new JsonObject();
      result.put(ENCODED_KEY, encodedKey);
      return Future.succeededFuture(result);
    } else {
      return Future.failedFuture("public key is null or empty");
    }
  }

  public Key decodePublicKey(JsonObject encodedPublicKey) {
    if (encodedPublicKey == null
        || encodedPublicKey.getString(ENCODED_KEY) == null
        || encodedPublicKey.getString(ENCODED_KEY).isEmpty()) {
      return null;
    }
    String publicKey = encodedPublicKey.getString(ENCODED_KEY);
    byte[] bytes = Base64.getUrlDecoder().decode(publicKey);
    return Key.fromBytes(bytes);
  }
}
