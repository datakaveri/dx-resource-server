package iudx.resource.server.encryption;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.exceptions.SodiumException;
import com.goterl.lazysodium.interfaces.Box;
import com.goterl.lazysodium.utils.Base64MessageEncoder;
import com.goterl.lazysodium.utils.Key;
import com.goterl.lazysodium.utils.KeyPair;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.encryption.service.EncryptionServiceImpl;
import iudx.resource.server.encryption.util.UrlBase64MessageEncoder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
public class EncryptionServiceImplTest {
    private static EncryptionServiceImpl encryptionService;
    private static LazySodiumJava lazySodiumJava;
    private static final Logger LOG = LoggerFactory.getLogger("EncryptionServiceImplTest.class");
    private static KeyPair keyPair;
    private static final String PUBLIC_KEY = "publicKey";
    private static final String ENCODED_KEY = "encodedKey";
    private static Key publicKey;
    private static Key privateKey;
    private static JsonObject jsonObject;
    private static Box.Lazy box;
    private static UrlBase64MessageEncoder urlBase64MessageEncoder;
    private static SodiumJava sodiumJava;
    @BeforeEach
    public void init(VertxTestContext vertxTestContext) {
        sodiumJava = new SodiumJava();
        urlBase64MessageEncoder = new UrlBase64MessageEncoder();
        lazySodiumJava = new LazySodiumJava(sodiumJava, urlBase64MessageEncoder);
        jsonObject = new JsonObject();
        box = (Box.Lazy) lazySodiumJava;
        encryptionService = new EncryptionServiceImpl(box);
        try {
            keyPair = box.cryptoBoxKeypair();
            publicKey = keyPair.getPublicKey();
            privateKey = keyPair.getSecretKey();
        } catch (SodiumException e) {
            LOG.error("Sodium Runtime exception: " + e);
        }
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test encodePublicKey method : Success")
    public void testEncodePublicKey(VertxTestContext vertxTestContext) {
        publicKey = keyPair.getPublicKey();
        jsonObject.put(PUBLIC_KEY, publicKey.getAsBytes());
        String encodedPublicKey = Base64.getUrlEncoder().encodeToString(publicKey.getAsBytes());
        encryptionService.encodePublicKey(jsonObject).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(encodedPublicKey, handler.result().getString(ENCODED_KEY));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause().getMessage());
            }
        });
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("publicKeyValues")
    @DisplayName("Test encodePublicKey method : With empty or invalid public Key")
    public void encodePublicKeyForInvalidPublicKey(JsonObject jsonObject, VertxTestContext vertxTestContext) {
        encryptionService.encodePublicKey(jsonObject).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("Test passed for empty or invalid public key");
            } else {
                assertEquals("public key is null or empty", handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    static Stream<Arguments> publicKeyValues() {
        return Stream.of(
                Arguments.of(new JsonObject().put(PUBLIC_KEY, null)),
                Arguments.of(new JsonObject().put(PUBLIC_KEY, "")),
                Arguments.of(new JsonObject().put("dummyKey", "1nSR-jsOD2xIDJvge1PNZrxwXdUChrv4C1cs2UJ1ew8="))
        );
    }

    static Stream<Arguments> encodedPublicKeyValues() {
        return Stream.of(
                Arguments.of(new JsonObject().put(ENCODED_KEY, null)),
                Arguments.of(new JsonObject().put(ENCODED_KEY, "")),
                Arguments.of(new JsonObject().put("dummyKey", "1nSR-jsOD2xIDJvge1PNZrxwXdUChrv4C1cs2UJ1ew8="))
        );
    }

    @ParameterizedTest
    @MethodSource("encodedPublicKeyValues")
    @DisplayName("Test decodePublicKey method : Failure")
    public void testDecodePublicKeyFailure(JsonObject jsonObject, VertxTestContext vertxTestContext) {
        assertNull(new EncryptionServiceImpl().decodePublicKey(jsonObject));
        vertxTestContext.completeNow();
    }

    static Stream<Arguments> invalidPublicKey() {
        return Stream.of(
                Arguments.of(new JsonObject().put(ENCODED_KEY, "I6W24wZTKcH0Xl6ykwD8eSLv8EZIhPU2WkwYzmZzP10=="),
                        "Exception while decoding public key: java.lang.IllegalArgumentException: Input byte array has incorrect ending byte at 44"),
                Arguments.of(new JsonObject().put(ENCODED_KEY, "I6W24wZTKcH0Xl6ykwD8eS+v8EZIhPU2WkwYzmZzP10"),
                        "Exception while decoding public key: java.lang.IllegalArgumentException: Illegal base64 character 2b"),
                Arguments.of(new JsonObject().put(ENCODED_KEY, "I6W24wZTKcH0Xl6ykwD8eS/v8EZIhPU2WkwYzmZzP10"),
                        "Exception while decoding public key: java.lang.IllegalArgumentException: Illegal base64 character 2f"),
                Arguments.of(new JsonObject().put(ENCODED_KEY, "I6W24wZTKcaaaaaa11111...........11111111111aaaaaaav8EZIhPU2WkwYzmZzP10"),
                        "Exception while decoding public key: java.lang.IllegalArgumentException: Illegal base64 character 2e")
        );
    }

    static Stream<Arguments> messages() {
        return Stream.of(
                Arguments.of(null, "message is null or empty"),
                Arguments.of("", "message is null or empty")
        );
    }

    @ParameterizedTest
    @DisplayName("Test encrypt method : With invalid message")
    @MethodSource("messages")
    public void testEncryptForInvalidMessage(String message, String expected, VertxTestContext vertxTestContext) {
        encryptionService.encrypt(message, new JsonObject()).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("test succeeded for invalid message");
            } else {
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @ParameterizedTest
    @MethodSource("invalidPublicKey")
    @DisplayName("Test encrypt method : with Invalid Public key")
    public void testEncryptForInvalidPublicKey(JsonObject jsonObject, String expected, VertxTestContext vertxTestContext) {
        String message = "Dummy string to be encrypted";
        encryptionService.encrypt(message, jsonObject).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("test succeeded for invalid message");
            } else {
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test encrypt method : with Sodium Exception")
    public void testEncryptFailure(VertxTestContext vertxTestContext) throws SodiumException {
        Box.Lazy box = mock(Box.Lazy.class);
        Base64MessageEncoder base64MessageEncoder = mock(Base64MessageEncoder.class);
        encryptionService = new EncryptionServiceImpl(box);
        String encodedKey = Base64.getUrlEncoder().encodeToString(publicKey.getAsBytes());
        jsonObject.put(ENCODED_KEY, encodedKey);
        when(box.cryptoBoxSealEasy(anyString(), any(Key.class))).thenThrow(SodiumException.class);
        encryptionService.encrypt("Dummy message", jsonObject).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause().getMessage());
            } else {
                assertEquals("Sodium exception: com.goterl.lazysodium.exceptions.SodiumException", handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test decrypt method : Success")
    public void testDecrypt(VertxTestContext vertxTestContext) {
        String message = "Dummy string to be encrypted";
        String encodedKey = Base64.getUrlEncoder().encodeToString(publicKey.getAsBytes());
        jsonObject.put("keyPair", keyPair);
        jsonObject.put(ENCODED_KEY, encodedKey);
        encryptionService.encrypt(message, jsonObject).onComplete(handler -> {
            if (handler.succeeded()) {
                assertNotNull(handler.result().getString("encodedCipherText"));
                String encodedCipherText = handler.result().getString("encodedCipherText");
                encryptionService.decrypt(encodedCipherText, jsonObject).onComplete(decryptHandler -> {
                    if (decryptHandler.succeeded()) {
                        assertEquals(message, decryptHandler.result().getString("message"));
                        vertxTestContext.completeNow();
                    } else {
                        vertxTestContext.failNow("Failure in decryption: " + decryptHandler.cause().getMessage());
                    }
                });
            } else {
                vertxTestContext.failNow(handler.cause().getMessage());
            }
        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Test decrypt method : with invalid message")
    public void testDecryptWithInvalidMessage(String encodedCipherText, VertxTestContext vertxTestContext) {
        encryptionService.decrypt(encodedCipherText, jsonObject).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("Success for null or empty message");
            } else {
                assertEquals("encoded cipher text is null or empty", handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    static Stream<Arguments> keyPairValue() {
        return Stream.of(
                Arguments.of(new JsonObject().put("keyPair", null)),
                Arguments.of(new JsonObject().put("dummyKeyPair", "abcd"))

        );
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("keyPairValue")
    @DisplayName("Test decrypt method : With absent keypair")
    public void testDecryptWithNoKeyPair(JsonObject jsonObject, VertxTestContext vertxTestContext) {
        encryptionService.decrypt("encodedCipherText", jsonObject).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow("Success for invalid keypair");
            } else {
                assertEquals("key pair is null", handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    static Stream<Arguments> encodedCipherText() {
        return Stream.of(
                Arguments.of("RjU4MTYwRTkxNzc2NkFFMzl==",
                        "IllegalArgumentException: java.lang.IllegalArgumentException: Input byte array has incorrect ending byte at 24"),
                Arguments.of("RjU4MTYwRTkxNzc2NkFFMzl",
                        "Exception : java.lang.NegativeArraySizeException: -31"),
                Arguments.of("encodedCipherText",
                        "IllegalArgumentException: java.lang.IllegalArgumentException: Last unit does not have enough valid bits"),
                Arguments.of("RjU4MTYwRTkxNzc2NkFFMzlFNjg0MURCN0NENTM2Q0EyNzUxODAxOTA4NzQ5MzgyRTAzRTMxRjlENkYzNDYxM0FFMUU4RUExQUZENzM1OTU2ODcwMDQxNjdCNkYyQ0RCNDNDOTA5MkM1ODE3RDExMUU0RkNBQjhGM0NFMURCQjg4RjE3OUYxNjI3NTk3NTU5OTJFMzk4OTg=",
                        "Sodium Exception"),
                Arguments.of("abcdefgh",
                        "Exception : java.lang.NegativeArraySizeException: -42"),
                Arguments.of("abcd",
                        "Exception : java.lang.NegativeArraySizeException: -45")

        );
    }


    @ParameterizedTest
    @MethodSource("encodedCipherText")
    @DisplayName("Test decrypt method : Failure")
    public void testDecryptFailure(String encodedCipherText, String expected, VertxTestContext vertxTestContext) {
        jsonObject.put("keyPair", keyPair);
        encryptionService.decrypt(encodedCipherText, jsonObject).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause().getMessage());
            } else {
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

}
