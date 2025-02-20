package iudx.resource.server.apiserver.integrationTests.complexCountPostQueries;

import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.resource.server.authenticator.TokensForITs.openResourceToken;
import static iudx.resource.server.authenticator.TokensForITs.secureResourceToken;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;


/**
 * Integration tests for the complex count post queries related to Geo, Temporal and Complex operations
 * in the resource server. These tests cover scenarios for success, empty responses, invalid parameters,
 * not found, and unauthorized access. The tests use RestAssured for making HTTP requests and validating responses.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class ComplexCountPostQueriesAPIsIT {
    private static final Logger LOGGER = LogManager.getLogger(ComplexCountPostQueriesAPIsIT.class);
    String id = "83c2e5c2-3574-4e11-9530-2b1fbdfce832";

    @Test
    @DisplayName("testing Complex count post query - 200 (success)- Geo Query(Circle)")
    void ComplexCountGeoQCircle() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=1000")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 200 (success)- Geo Query(Circle) with optional encryption")
    void ComplexCountGeoQVthOptionalEncry() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=10")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                //optional encryption
                //.header("publicKey", publicKeyValue)
                .post("/entityOperations/query")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 204 (Empty Response)- Geo Query(Circle)")
    void ComplexCountGeoQCircleEmptyResp() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(31.178).add(72.834))
                        .put("georel", "near;maxDistance=10")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex count post query - 400 (Invalid params)- Geo Query(Circle)")
    void ComplexCountGeoQCircleInvParams() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQuery", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=10")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex count post query - 404 (Not Found)- Geo Query(Circle)")
    void ComplexCountGeoQCircleNotFound() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce839")))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=10")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex count post query - 401 (Not Authorized)- Geo Query(Circle)")
    void ComplexCountGeoQCircleUnAuth() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=10")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex count post query - 200 (success)- Temporal between")
    void ComplexCountTemporalBtw() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "between")
                        .put("time", "2020-10-19T14:20:00Z")
                        .put("endtime", "2020-10-19T15:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 204 (Empty Response)- Temporal between")
    void ComplexCountTemporalBtwEmptyResp() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "between")
                        .put("time", "2021-12-01T14:20:00Z")
                        .put("endtime", "2021-12-01T14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 400 (Invalid params)- Temporal between")
    void ComplexCountTemporalBtwInvParams() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("temporalQ", new JsonObject()
                        .put("timerelation", "between")
                        .put("time", "2020-10-19T14:20:00Z")
                        .put("endtime", "2020-10-19T15:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 404 (Not Found)- Temporal between")
    void ComplexCountTemporalBtwNotFound() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce833")))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "between")
                        .put("time", "2020-10-19T14:20:00Z")
                        .put("endtime", "2020-10-19T15:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 401 (Not Authorized)- Temporal between")
    void ComplexCountTemporalBtwUnAuth() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "between")
                        .put("time", "2020-10-19T14:20:00Z")
                        .put("endtime", "2020-10-19T15:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 400 (Invalid date format)- Temporal between")
    void ComplexCountTemporalBtwInvDateFormat() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "between")
                        .put("time", "2020-09-18Z14:20:00Z")
                        .put("endtime", "2020-09-19Z14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 200 (success)- Temporal before")
    void ComplexCountTemporalBefore() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "before")
                        .put("time", "2020-10-19T14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 400 (Invalid params)- Temporal before")
    void ComplexCountTemporalBeforeInvParams() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("temporalQ", new JsonObject()
                        .put("timerelation", "before")
                        .put("time", "2020-10-19T14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 404 (Not Found)- Temporal before")
    void ComplexCountTemporalBeforeNotFound() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", "i83c2e5c2-3574-4e11-9530-2b1fbdfce831")))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "before")
                        .put("time", "2020-10-19T14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 401 (Not Authorized)- Temporal before")
    void ComplexCountTemporalBeforeUnAuth() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "before")
                        .put("time", "2020-10-19T14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 400 (Invalid date format)- Temporal before")
    void ComplexCountTemporalBeforeInvDateFormat() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "between")
                        .put("time", "2020-09-18Z14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }


    @Test
    @DisplayName("testing Complex Count post query - 200 (success) - Complex - geo - (circle) + temporal (between) + attribute (>)")
    void ComplexGeoCircleTempBetAttr1RespFilter() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=1000")
                        .put("geoproperty", "location"))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "between")
                        .put("time", "2020-10-18T14:20:00Z")
                        .put("endtime", "2020-10-19T14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("q", "speed>30.0")
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 204 (Empty Response) - Complex - geo - (circle) + temporal (between) + attribute (>)")
    void ComplexGeoCircleTempBetAttr1RespFilterEmpResp() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=1000")
                        .put("geoproperty", "location"))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "between")
                        .put("time", "2021-12-18T14:20:00Z")
                        .put("endtime", "2021-12-19T14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("q", "speed>30.0")
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 400 (Invalid params) - Complex - geo - (circle) + temporal (between) + attribute (>)")
    void ComplexGeoCircleTempBetAttr1RespFilterInvParams() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=1000")
                        .put("geoproperty", "location"))
                .put("temporalQuery", new JsonObject()
                        .put("timerel", "between")
                        .put("time", "2020-10-18T14:20:00Z")
                        .put("endtime", "2020-10-19T14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("q", "speed>30.0")
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 404 (Not Found) - Complex - geo - (circle) + temporal (between) + attribute (>)")
    void ComplexGeoCircleTempBetAttr1RespFilterNotFound() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce831")))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=1000")
                        .put("geoproperty", "location"))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "between")
                        .put("time", "2020-10-18T14:20:00Z")
                        .put("endtime", "2020-10-19T14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("q", "speed>30.0")
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(404)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 401 (Not Authorized) - Complex - geo - (circle) + temporal (between) + attribute (>)")
    void ComplexGeoCircleTempBetAttr1RespFilterUnAuth() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=1000")
                        .put("geoproperty", "location"))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "between")
                        .put("time", "2020-10-18T14:20:00Z")
                        .put("endtime", "2020-10-19T14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("q", "speed>30.0")
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/temporal/entityOperations/query")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 200 (success) - Complex - geo query-(polygon)")
    void ComplexGeoQPolygon() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Polygon")
                        .put("coordinates", new JsonArray().add(
                                new JsonArray().add(new JsonArray().add(72.76).add(21.15))
                                        .add(new JsonArray().add(72.76).add(21.13))
                                        .add(new JsonArray().add(72.78).add(21.13))
                                        .add(new JsonArray().add(72.78).add(21.15))
                                        .add(new JsonArray().add(72.76).add(21.15))
                        ))
                        .put("georel", "within")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 204 (Empty Response) - Complex - geo query-(polygon)")
    void ComplexGeoQPolygonEmptyResp() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Polygon")
                        .put("coordinates", new JsonArray().add(new JsonArray()
                                .add(new JsonArray().add(72.719).add(31))
                                .add(new JsonArray().add(72.842).add(31.2))
                                .add(new JsonArray().add(72.923).add(30.8))
                                .add(new JsonArray().add(72.74).add(30.34))
                                .add(new JsonArray().add(72.9).add(30.1))
                                .add(new JsonArray().add(72.67).add(30))
                                .add(new JsonArray().add(72.719).add(31)))
                        )
                        .put("georel", "within")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 400 (Invalid Params) - Complex - geo query-(polygon)")
    void ComplexGeoQPolygonInvParams() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Polygon")
                        .put("coordinates", new JsonArray().add(
                                new JsonArray().add(new JsonArray().add(72.76).add(21.15))
                                        .add(new JsonArray().add(72.76).add(21.13))
                                        .add(new JsonArray().add(72.78).add(21.13))
                                        .add(new JsonArray().add(72.78).add(21.15))
                                        .add(new JsonArray().add(72.76).add(21.15))
                        ))
                        .put("georelation", "within")
                        .put("geoproperty", "geoJsonLocation"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 404 (Not Found) - Complex - geo query-(polygon)")
    void ComplexGeoQPolygonNotFound() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce831")))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Polygon")
                        .put("coordinates", new JsonArray().add(
                                new JsonArray().add(new JsonArray().add(72.76).add(21.15))
                                        .add(new JsonArray().add(72.76).add(21.13))
                                        .add(new JsonArray().add(72.78).add(21.13))
                                        .add(new JsonArray().add(72.78).add(21.15))
                                        .add(new JsonArray().add(72.76).add(21.15))
                        ))
                        .put("georel", "within")
                        .put("geoproperty", "location"));

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 401 (Not Authorized) - Complex - geo query-(polygon)")
    void ComplexGeoQPolygonUnAuth() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Polygon")
                        .put("coordinates", new JsonArray().add(
                                new JsonArray().add(new JsonArray().add(72.76).add(21.15))
                                        .add(new JsonArray().add(72.76).add(21.13))
                                        .add(new JsonArray().add(72.78).add(21.13))
                                        .add(new JsonArray().add(72.78).add(21.15))
                                        .add(new JsonArray().add(72.76).add(21.15))
                        ))
                        .put("georel", "within")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 200 (success) - Complex - geo query-(bbox)")
    void ComplexGeoQBbox() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "bbox")
                        .put("coordinates", new JsonArray()
                                .add(new JsonArray().add(72.8296).add(21.2))
                                .add(new JsonArray().add(72.8297).add(21.15))
                        )
                        .put("georel", "within")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 204 (Empty Response) - Complex - geo query-(bbox)")
    void ComplexGeoQBboxEmptyResp() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "bbox")
                        .put("coordinates", new JsonArray()
                                .add(new JsonArray().add(72.8296).add(31.2))
                                .add(new JsonArray().add(72.8297).add(31.15))
                        )
                        .put("georel", "within")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 400 (Invalid Params) - Complex - geo query-(bbox)")
    void ComplexGeoQBboxInvParams() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "bbox")
                        .put("coordinates", new JsonArray()
                                .add(new JsonArray().add(72.8296).add(21.2))
                                .add(new JsonArray().add(72.8297).add(21.15))
                        )
                        .put("georelation", "within")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 404 (Not Found) - Complex - geo query-(bbox)")
    void ComplexGeoQBboxNotFound() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce831")))
                .put("geoQ", new JsonObject()
                        .put("geometry", "bbox")
                        .put("coordinates", new JsonArray()
                                .add(new JsonArray().add(72.8296).add(21.2))
                                .add(new JsonArray().add(72.8297).add(21.15))
                        )
                        .put("georel", "within")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 401 (Not Authorized) - Complex - geo query-(bbox)")
    void ComplexGeoQBboxUnAuth() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "bbox")
                        .put("coordinates", new JsonArray()
                                .add(new JsonArray().add(72.8296).add(21.2))
                                .add(new JsonArray().add(72.8297).add(21.15))
                        )
                        .put("georel", "within")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 200 (success) - Complex - geo query-(linestring)")
    void ComplexGeoQLineString() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "linestring")
                        .put("coordinates", new JsonArray()
                                .add(new JsonArray().add(72.833994).add(21.17798))
                                .add(new JsonArray().add(72.833978).add(21.178005))
                        )
                        .put("georel", "intersects")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 204 (Empty Response) - Complex - geo query-(linestring)")
    void ComplexGeoQLineStringEmptyResp() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "linestring")
                        .put("coordinates", new JsonArray()
                                .add(new JsonArray().add(72.842).add(31.2))
                                .add(new JsonArray().add(72.923).add(30.8))
                                .add(new JsonArray().add(72.74).add(30.34))
                                .add(new JsonArray().add(72.9).add(30.1))
                                .add(new JsonArray().add(72.67).add(30))
                        )
                        .put("georel", "intersects")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 400 (Invalid Params) - Complex - geo query-(linestring)")
    void ComplexGeoQLineStringInvParams() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "linestring")
                        .put("coordinates", new JsonArray()
                                .add(new JsonArray().add(72.833994).add(21.17798))
                                .add(new JsonArray().add(72.833978).add(21.178005))
                        )
                        .put("georelation", "intersects")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 404 (Not Found) - Complex - geo query-(linestring)")
    void ComplexGeoQLineStringNotFound() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce831")))
                .put("geoQ", new JsonObject()
                        .put("geometry", "linestring")
                        .put("coordinates", new JsonArray()
                                .add(new JsonArray().add(72.842).add(21.2))
                                .add(new JsonArray().add(72.923).add(20.8))
                                .add(new JsonArray().add(72.74).add(20.34))
                                .add(new JsonArray().add(72.9).add(20.1))
                                .add(new JsonArray().add(72.67).add(20))
                        )
                        .put("georel", "intersects")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Complex Count post query - 401 (Not Authorized) - Complex - geo query-(linestring)")
    void ComplexGeoQLineStringUnAuth() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "linestring")
                        .put("coordinates", new JsonArray()
                                .add(new JsonArray().add(72.833994).add(21.17798))
                                .add(new JsonArray().add(72.833978).add(21.178005))
                        )
                        .put("georel", "intersects")
                        .put("geoproperty", "location"))
                .put("options", "count");

        Response response = given()
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .body(requestBody.toString())
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }
}
