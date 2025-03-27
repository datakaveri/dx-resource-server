package org.cdpg.dx.encryption.service;

import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public interface EncryptionService {

    /* A factory method to create a proxy */
//    @GenIgnore
//    static EncryptionService createProxy(Vertx vertx, String address) {
//        return new EncryptionServiceVertxEBProxy(vertx, address);
//    }
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
    Future<JsonObject> encrypt(String message, String encodedPublicKey);

}
