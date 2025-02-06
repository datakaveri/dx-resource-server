package iudx.resource.server.encryption.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;


@ProxyGen
@VertxGen
public interface EncryptionService {

  /* A factory method to create a proxy */
  @GenIgnore
  static EncryptionService createProxy(Vertx vertx, String address) {
    return new EncryptionServiceVertxEBProxy(vertx, address);
  }
  /* service operations */

  /**
   * encrypts the message using the public key
   *
   * <p>Encodes the message in base64 format
   *
   * @param message : resource to be encrypted
   * @param encodedPublicKey : URL base64 public key in JsonObject
   * @return cipherText : encrypted and encoded cipher text for the client
   */
  Future<JsonObject> encrypt(String message, JsonObject encodedPublicKey);

  /**
   * Decodes the encodedCipherText from base64 to String
   *
   * <p>Decrypts using libsodium sealed box by supplying keypair and cipherText
   *
   * @param keyPair : libsodium keypair
   * @param encodedCipherText : encoded cipher text to be decoded and decrypted
   * @return message
   */
  Future<JsonObject> decrypt(String encodedCipherText, JsonObject keyPair);

  /**
   * Encodes the public key in URL safe base64 format
   *
   * @param publicKey : public key in a json object
   * @return JsonObject : encoded key as a JSON Object future
   */
  Future<JsonObject> encodePublicKey(JsonObject publicKey);

  //    Future<JsonObject> decodePublicKey(JsonObject encodedPublicKey);

}
