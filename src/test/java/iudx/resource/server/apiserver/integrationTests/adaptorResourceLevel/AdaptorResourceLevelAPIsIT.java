package iudx.resource.server.apiserver.integrationTests.adaptorResourceLevel;

import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static iudx.resource.server.authenticator.TokensForITs.*;
import static org.hamcrest.Matchers.equalTo;

/**
 * Integration tests for the Adaptor Resource level APIs in the Resource Server.
 * These tests cover scenarios such as registering adaptors through POST methods,
 * retrieving adaptors, and deleting adaptors at the resource level.
 * */
@ExtendWith(RestAssuredConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdaptorResourceLevelAPIsIT {
    private static final Logger LOGGER = LogManager.getLogger(AdaptorResourceLevelAPIsIT.class);
    String adapter_id_RL= "935f2045-f5c6-4c76-b14a-c29a88589bf3";
    String adapter_id_RL2 = "695e222b-3fae-4325-8db0-3e29d01c4fc0";

    @Test
    @Order(1)
    @DisplayName("testing adaptor resource level  - 201 (Created Successfully) Register Adaptor")
    void PostIngestionRegisterAdaptor() {
        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add(adapter_id_RL));
        Response response = given()
                .header("token", providerToken)
                .body(requestBody.toString())
                .contentType("application/json")
                .when()
                .post("/ingestion")
                .then()
                .statusCode(201)
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @Order(2)
    @DisplayName("testing adaptor resource level  - 409 (Already exist) Register Adaptor")
    void PostIngestionRegisterAdaptor409() {
        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add(adapter_id_RL));
        Response response = given()
                .header("token", providerToken)
                .body(requestBody.toString())
                .contentType("application/json")
                .when()
                .post("/ingestion")
                .then()
                .statusCode(409)
                .body("title", equalTo("Conflict"))
                .extract()
                .response();
    }

    @Test
    @Order(3)
    @DisplayName("testing adaptor resource level  - 401 (Not Authorized) Register Adaptor")
    void PostIngestionRegisterAdaptorUnAuth() {
        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add(adapter_id_RL));
        Response response = given()
                .header("token", "public_1")
                .body(requestBody.toString())
                .contentType("application/json")
                .when()
                .post("/ingestion")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @Order(4)
    @DisplayName("testing adaptor resource level  - 200 (Success) Get adaptor details")
    void GetIngestionAdaptor() {
        Response response = given()
                .header("token", providerToken)
                .contentType("application/json")
                .pathParam("adapter_id_RL", adapter_id_RL)
                .when()
                .get("/ingestion/{adapter_id_RL}")
                .then()
                .statusCode(200)
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @Order(5)
    @DisplayName("testing adaptor resource level  - 404 (Not Found) Get adaptor details")
    void GetIngestionAdaptorNotFound() {
        String adapter_id_RL= "123";
        Response response = given()
                .header("token", providerToken)
                .contentType("application/json")
                .pathParam("adapter_id_RL", adapter_id_RL)
                .when()
                .get("/ingestion/{adapter_id_RL}")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .body("type", equalTo("urn:dx:rs:general"))
                .extract()
                .response();
    }

    @Test
    @Order(6)
    @DisplayName("testing adaptor resource level  - 401 (Not Authorized) Get adaptor details")
    void GetIngestionAdaptorUnAuth() {
        Response response = given()
                .header("token", "public_1")
                .contentType("application/json")
                .pathParam("adapter_id_RL", adapter_id_RL)
                .when()
                .get("/ingestion/{adapter_id_RL}")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @Order(7)
    @DisplayName("testing adaptor resource level  - 401 (Not Authorized) Delete adaptor")
    void DeleteIngestionAdaptorUnAuth() {
        Response response = given()
                .header("token", "public_1")
                .contentType("application/json")
                .pathParam("adapter_id_RL", adapter_id_RL)
                .when()
                .delete("/ingestion/{adapter_id_RL}")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @Order(8)
    @DisplayName("testing adaptor resource level  - 200 (Success) Delete adaptor")
    void DeleteIngestionAdaptor() {
        Response response = given()
                .header("token", providerToken)
                .contentType("application/json")
                .pathParam("adapter_id_RL", adapter_id_RL)
                .when()
                .delete("/ingestion/{adapter_id_RL}")
                .then()
                .statusCode(200)
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @Order(9)
    @DisplayName("testing adaptor resource level  - 404 (Not Found) Delete adaptor")
    void DeleteIngestionAdaptorNotFound() {
        Response response = given()
                .header("token", providerToken)
                .contentType("application/json")
                .pathParam("adapter_id_RL", adapter_id_RL)
                .when()
                .delete("/ingestion/{adapter_id_RL}")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .extract()
                .response();
    }

    @Test
    @Order(10)
    @DisplayName("testing adaptor resource level  - 200 (Created Successfully) Ingestion Entities Adaptor")
    void PostIngestionEntitiesAdaptor() {
        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add(adapter_id_RL2));
        Response response = given()
                .header("token", adaptorToken)
                .body(new JsonArray().add(requestBody).toString())
                .contentType("application/json")
                .when()
                .post("/ingestion/entities")
                .then()
                .statusCode(200)
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @Order(11)
    @DisplayName("testing adaptor resource level  - 401 (Unauthorized) Ingestion Entities Adaptor")
    void PostIngestionEntitiesAdaptorUnAuthorized() {
        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add(adapter_id_RL2));
        Response response = given()
                .header("token", secureResourceToken)
                .body(new JsonArray().add(requestBody).toString())
                .contentType("application/json")
                .when()
                .post("/ingestion/entities")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @Order(12)
    @DisplayName("testing adaptor resource level  - 404 (Unauthorized) Ingestion Entities Adaptor")
    void PostIngestionEntitiesAdaptorNotFound() {
        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add("adapter_id_RL2"));
        Response response = given()
                .header("token", secureResourceToken)
                .body(new JsonArray().add(requestBody).toString())
                .contentType("application/json")
                .when()
                .post("/ingestion/entities")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .extract()
                .response();
    }
    @Test
    @Order(12)
    @DisplayName("testing adaptor resource level  - 400 (Bad Request) Ingestion Entities Adaptor")
    void PostIngestionEntitiesAdaptorBadRequest() {
        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add("adapter_id_RL2"));
        JsonObject requestBody2 = new JsonObject()
                .put("entities", new JsonArray().add(adapter_id_RL2));
        Response response = given()
                .header("token", adaptorToken)
                .body(new JsonArray().add(requestBody).add(requestBody2).toString())
                .contentType("application/json")
                .when()
                .post("/ingestion/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }
}
