package iudx.resource.server.apiserver.integrationTests.subscriptionapis;
import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.*;

import static iudx.resource.server.authenticator.TokensForITs.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StreamingSubscriptionAPIsIT {
    String appName = "Subscriptions Test 15";
    String inValidAppName="RS-integration-test-alias-RL";
    String entityId ="83c2e5c2-3574-4e11-9530-2b1fbdfce832";
    String invalidEntity = "8b95ab80-2aaf-4636-a65e-7f2563d0d370";
    String invalidToken="public_1";
    JsonArray entitiesArray = new JsonArray()
            .add("83c2e5c2-3574-4e11-9530-2b1fbdfce832")
            .add("b58da193-23d9-43eb-b98a-a103d4b6103c");
    private static String subscriptionID;


    // Creating Streaming Subscription
    @Test
    @Order(1)
    @DisplayName("201 (created) - create a streaming subscription RL")
    public void createSubscriptionTest() {
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add(entityId));

        Response response = given()
                .header("Content-Type", "application/json")
                .header("options", "streaming")
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/subscription");

        response.then()
                .statusCode(201)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("success"))
                .body("results[0].id", containsString('/' + appName));

        // Extract the subscriptionID directly
        subscriptionID = response.jsonPath().getString("results[0].id");
    }
    @Test
    @Order(2)
    @DisplayName("400 (bad query) - create a streaming subscription RL")
    public void createSubscriptionWithInvalidPayloadTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("types", "subscription")
                .put("entities", new JsonArray().add(entityId));
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/subscription")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidPayloadFormat"))
                .body("title", equalTo("Bad Request"));
    }
    @Test
    @Order(3)
    @DisplayName("401 (not authorized) create a streaming subscription")
    public void createSubscriptionWithInvalidTokenTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add(entityId));
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + invalidToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/subscription")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"));
    }
    @Test
    @Order(4)
    @DisplayName("409 (Already exist) create a streaming subscription")
    public void createAlreadyExistingSubscriptionTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add(entityId));
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/subscription")
                .then()
                .statusCode(409)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:general"))
                .body("title", equalTo("Conflict"));
    }

    // Appending Streaming Subscription
    @Test
    @Order(5)
    @DisplayName("201 (created) - Append streaming subscription")
    public void appendStreamingSubscriptionTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", entitiesArray);
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .patch("/subscription/"+subscriptionID)
                .then()
                .statusCode(201);
        //.log().body();
    }
    @Test
    @Order(6)
    @DisplayName("400 (invalid name) - Append streaming subscription")
    public void appendStreamingSubscriptionWithInvalidNameTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", inValidAppName)
                .put("type", "subscription")
                .put("entities",entitiesArray);
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .patch("/subscription/"+subscriptionID)
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidParamameter"))
                .body("title", equalTo("Bad Request"));
    }
    @Test
    @Order(7)
    @DisplayName("401 (not authorized) append streaming subscription")
    public void appendStreamingSubscriptionWithInvalidTokenTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", entitiesArray);
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + invalidToken)
                .body(requestBody.encodePrettily())
                .when()
                .patch("/subscription/"+subscriptionID)
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"));
    }
    @Test
    @Order(8)
    @DisplayName("404 (not found) - Append streaming subscription")
    public void appendNonExistingStreamingSubscriptionTest() {
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add(invalidEntity));
        given()
                .header("Content-Type", "application/json")
                .header("options", "streaming")
                .header("Authorization", "Bearer " + openResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .patch("/subscription/"+subscriptionID)
                .then()
                //.log().body()
                .statusCode(404)
                .body("type", equalTo("urn:dx:rs:resourceNotFound"))
                .body("title", equalTo("Not Found"));
    }

    //Updating subscription
    @Test
    @Order(9)
    @DisplayName("201 (created) - Update streaming subscription")
    public void updateStreamingSubscriptionTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add(entityId));
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/subscription/"+subscriptionID)
                .then()
                .statusCode(201)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("success"));
    }
    @Test
    @Order(10)
    @DisplayName("400 (invalid name) - Update streaming subscription")
    public void updateStreamingSubscriptionWithInvalidNameTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", inValidAppName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add(entityId));
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/subscription/"+subscriptionID)
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidParamameter"))
                .body("title", equalTo("Bad Request"));
    }
    @Test
    @Order(11)
    @DisplayName("401 (not authorized) Update streaming subscription")
    public void updateStreamingSubscriptionWithInvalidTokenTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add(entityId));
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + invalidToken)
                .body(requestBody.encodePrettily())
                .when()
                .patch("/subscription/"+subscriptionID)
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"));
    }
    @Test
    @Order(12)
    @DisplayName("404 (Not Found) - Update streaming subscription")
    public void updateNonExistingStreamingSubscriptionTest() {
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add(invalidEntity));
        given()
                .header("Content-Type", "application/json")
                .header("options", "streaming")
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .patch("/subscription/"+subscriptionID)
                .then()
                //.log().body()
                .statusCode(404)
                .body("type", equalTo("urn:dx:rs:resourceNotFound"))
                .body("title", equalTo("Not Found"));
    }

    //Retrieving Streaming Subscription
    @Test
    @Order(13)
    @DisplayName("200 (success) - Get streaming subscription")
    public void getStreamingSubscriptionTest(){
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + secureResourceToken)
                .when()
                .get("/subscription/"+subscriptionID)
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("success"));
    }
    @Test
    @Order(14)
    @DisplayName("401 (not authorized) - Get streaming subscription")
    public void getStreamingSubscriptionWithInvalidTokenTest(){
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + invalidToken)
                .when()
                .get("/subscription/"+subscriptionID)
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"));
    }
    @Test
    @Order(15)
    @DisplayName("404 (not found) - Get streaming subscription")
    public void getNonExistingStreamingSubscriptionTest() {
        String nonExistingId = "fd47486b-3497-4248-ac1e-082e4d37a66c/Subscriptions RS Test-1234invalidId";
        given()
                .header("Content-Type", "application/json")
                .header("options", "streaming")
                .header("Authorization", "Bearer " + delegateToken)
                .when()
                .get("/subscription/"+nonExistingId)
                .then()
                //.log().body()
                .statusCode(404)

                .body("type", equalTo("urn:dx:rs:resourceNotFound"))
                .body("title", equalTo("Not Found"));
    }

    // Deleting Streaming Subscription

    @Test
    @Order(16)
    @DisplayName("200 (success) - Delete a subscription RL")
    public void deleteStreamingSubscriptionTest(){
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + secureResourceToken)
                .when()
                .delete("/subscription/"+subscriptionID)
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("success"));
    }
    @Test
    @Order(17)
    @DisplayName("401 (not authorized) - Delete a subscription")
    public void deleteStreamingSubscriptionWithInvalidTokenTest(){
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("Authorization", "Bearer " + invalidToken)
                .when()
                .delete("/subscription/"+subscriptionID)
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"));
    }
    @Test
    @Order(18)
    @DisplayName("404 (not found) - Delete a subscription")
    public void deleteNonExistingStreamingSubscriptionTest() {
        String nonExistingId = "fd47486b-3497-4248-ac1e-082e4d37a66c/Subscriptions RS Test-1234invalidId";
        given()
                .header("Content-Type", "application/json")
                .header("options", "streaming")
                .header("Authorization", "Bearer " + delegateToken)
                .when()
                .delete("/subscription/"+nonExistingId)
                .then()
                //.log().body()
                .statusCode(404)
                .body("type", equalTo("urn:dx:rs:resourceNotFound"))
                .body("title", equalTo("Not Found"));
    }

}