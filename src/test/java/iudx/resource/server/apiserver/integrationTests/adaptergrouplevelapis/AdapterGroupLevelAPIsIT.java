package iudx.resource.server.apiserver.integrationTests.adaptergrouplevelapis;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;


import static iudx.resource.server.authenticator.TokensForITs.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdapterGroupLevelAPIsIT {
    String adapter_id_GL="935f2045-f5c6-4c76-b14a-c29a88589bf3";
    String nonExisting_adapter_id_GL="935f2045-f5c6-4c76-b14a-c29a88589bf3-123";
    String invalidToken="public_1";

    //Registering Adapter (Group Level)
    @Test
    @Order(1)
    @DisplayName("201 (created successfully) - Register adapter (GL)")
    public void createRegisterAdaptersTest(){
        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add(adapter_id_GL));
        given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " +providerToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/ingestion")
                .then()
                .statusCode(201)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .body("results", notNullValue())
                .body("results[0].id", notNullValue())
                .body("results[0].username", notNullValue())
                .body("results[0].apiKey", notNullValue());
    }
    @Test
    @Order(2)
    @DisplayName("409 (already exist) - Register adapter")
    public void createAlreadyExistingRegisterAdaptersTest(){
        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add(adapter_id_GL));
        given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + providerToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/ingestion")
                .then()
                .statusCode(409)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:resourceAlreadyExist"))
                .body("title", equalTo("Conflict"));
    }
    @Test
    @Order(3)
    @DisplayName("401(not authorized) register adapter")
    public void createRegisterAdaptersWithInvalidTokenTest(){

        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add(adapter_id_GL));
        given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + invalidToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/ingestion")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"));
    }

    //Retrieving Adapter (Group Level)
    @Test
    @Order(4)
    @DisplayName("200 (success) - Get adapter details (GL)")
    public void getRegisterAdaptersTest(){
        given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + providerToken)
                .pathParam("adapter_id_GL", adapter_id_GL)
                .when()
                .get("/ingestion/{adapter_id_GL}")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @Order(5)
    @DisplayName("404 (not found) - Get adapter details")
    public void getNonExistingRegisterAdaptersTest(){
        given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + providerToken)
                .pathParam("nonExisting_adapter_id_GL", nonExisting_adapter_id_GL)
                .when()
                .get("/ingestion/{nonExisting_adapter_id_GL}")
                .then()
                .statusCode(404)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:general"))
                .body("title", equalTo("Not Found"))
                .extract().response();
    }
    @Test
    @Order(6)
    @DisplayName("401 (not authorized) - Get adapter details")
    public void getRegisterAdaptersWithInvalidTokenTest(){
        String adapter_id_GL="935f2045-f5c6-4c76-b14a-c29a88589bf3";
        given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + invalidToken)
                .pathParam("adapter_id_GL", adapter_id_GL)
                .when()
                .get("/ingestion/{adapter_id_GL}")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"))
                .extract().response();
    }

    //Deleting Adapter (Group Level)
    @Test
    @Order(7)
    @DisplayName("200 (success) - Delete adapter (GL)")
    public void deleteRegisterAdaptersTest(){
        given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + providerToken)
                .pathParam("adapter_id_GL", adapter_id_GL)
                .when()
                .delete("/ingestion/{adapter_id_GL}")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @Order(8)
    @DisplayName("404 (not found) - Delete adapter details")
    public void deleteNonExistingRegisterAdaptersTest(){
        given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + providerToken)
                .pathParam("invalid_adapter_id_GL", nonExisting_adapter_id_GL)
                .when()
                .delete("/ingestion/{invalid_adapter_id_GL}")
                .then()
                .statusCode(404)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:general"))
                .body("title", equalTo("Not Found"))
                .extract().response();
    }
    @Test
    @Order(9)
    @DisplayName("401 (not authorized) - Delete adapter details")
    public void deleteRegisterAdaptersWithInvalidTokenTest(){
        given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + invalidToken)
                .pathParam("adapter_id_GL", adapter_id_GL)
                .when()
                .delete("/ingestion/{adapter_id_GL}")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"))
                .extract().response();
    }
    @AfterEach
    public void tearDown() {
        // Introduce a delay
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}