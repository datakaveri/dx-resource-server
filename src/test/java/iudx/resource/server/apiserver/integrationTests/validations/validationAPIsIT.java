package iudx.resource.server.apiserver.integrationTests.validations;

import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static iudx.resource.server.authenticator.TokensForITs.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the validation APIs in the Resource Server API Server.
 * These tests cover various scenarios related to input validations.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class validationAPIsIT {
    private static final Logger LOGGER = LogManager.getLogger(validationAPIsIT.class);
    String id = "83c2e5c2-3574-4e11-9530-2b1fbdfce832";

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid length")
    void GetEntityInvalidIDLen() {
        Response response = given()
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities/8b95ab80-2aaf-4636-a65e-7f2563d0d3718978976543454534567865646787987654e678iNsm39qa8Z6GfSZrq5b0xmBA0apYYjJVgtxq5kZHd1fq2TGQ3c12gkri34j10msxSPaXt7uynP0OI8EvHPK5V1YsefHxRfem25C8TTASr1ggdtk9ORXuQ6ry0leCorbzFvVtQdJO4HAuLjplMpIkipmTTGfoSRWWIT7yt60nn4ZqHYsXhhm4ud1hYkCUGrDeC3CkacthEuJAah10OpzaCyWHMf0XWqirLAYahsJzhv9p2BRLRZrEG7gP0TWD9BjMvIuVk8ILGVgZyYWjC3dJsKuNe2cLo5P4fWW0iRik4dtvHTYaFA2TGXs1euujSpBYY1XsTCuLuOhuCMAsMS9PPmlePl7vlIA109IbhDOqrQqjOGx7LELr8P02F9arsqK3MfCTSwfQJPg3vcGBeMJsVPtZjDTDhJjVHCFzRtFsnmCTySZUcgEYVwU3byAg3CFSPIPhE6LvJpj6iv0xx8GmnG8Qot4kplFcVA0yvOQbaMG7gTzQBwokBwBdkibDDrW7")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid geo property")
    void GetEntityInvGeoProperty() {
        Response response = given()
                .param("id",id)
                .param("geoproperty", "place")
                .param("georel", "near;maxdistance=10")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834]")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) Empty geo property")
    void GetEntityEmptyGeoProperty() {
        Response response = given()
                .param("id",id)
                .param("geoproperty", "")
                .param("georel", "near;maxdistance=10")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834]")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) Missing geo property")
    void GetEntityMissingGeoProperty() {
        Response response = given()
                .param("id",id)
                .param("georel", "near;maxdistance=10")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834]")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid georel")
    void GetEntityInvGeoRel() {
        Response response = given()
                .param("id",id)
                .param("geoproperty", "location")
                .param("georel", "beyond")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834]")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidGeoRel"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) Empty georel")
    void GetEntityEmptyGeoRel() {
        Response response = given()
                .param("id",id)
                .param("geoproperty", "place")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834]")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidGeoValue"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid max distance")
    void GetEntityInvMaxDist() {
        Response response = given()
                .param("id",id)
                .param("geoproperty", "location")
                .param("georel", "near;maxdistance=10000000")
                .param("geometry", "ellipse")
                .param("coordinates", "[21.178,72.834]")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidGeoValue"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid geometry")
    void GetEntityInvGeometry() {
        Response response = given()
                .param("id",id)
                .param("geoproperty", "location")
                .param("georel", "near;maxdistance=100")
                .param("geometry", "ellipse")
                .param("coordinates", "[21.178,72.834]")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidGeoValue"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) empty geometry")
    void GetEntityEmptyGeometry() {
        Response response = given()
                .param("id",id)
                .param("geoproperty", "location")
                .param("georel", "near;maxdistance=100")
                .param("geometry", "")
                .param("coordinates", "[21.178,72.834]")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidGeoValue"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid coordinate format for points")
    void GetEntityInvCoordinatesFormat() {
        Response response = given()
                .param("id",id)
                .param("geoproperty", "location")
                .param("georel", "near;maxdistance=100")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834,23.56789]")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidGeoValue"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid coordinate precision format>6")
    void GetEntityInvCoordinatesPrecisionFormat() {
        Response response = given()
                .param("id",id)
                .param("geoproperty", "location")
                .param("georel", "near;maxdistance=100")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.8341234]")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidGeoValue"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid options param value")
    void GetEntityInvOptionsVal() {
        Response response = given()
                .param("id",id)
                .param("geoproperty", "location")
                .param("georel", "near;maxdistance=100")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834,23.56789]")
                .param("options", "total")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidParameterValue"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) empty options param value")
    void GetEntityEmptyOptionsVal() {
        Response response = given()
                .param("id",id)
                .param("geoproperty", "location")
                .param("georel", "near;maxdistance=100")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834,23.56789]")
                .param("options", "")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidParameterValue"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid options param value")
    void GetTemporalEntityInvTimerel() {
        Response response = given()
                .param("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .param("timerel", "beyond")
                .param("time", "2020-09-18T14:20:00Z")
                .param("endtime", "2020-09-19T14:20:00Z")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidTemporalRelationParam"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) no end time for between")
    void GetTemporalEntityNoEndTime() {
        Response response = given()
                .param("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .param("timerel", "between")
                .param("time", "2020-09-18T14:20:00Z")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidTemporalParam"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid interval")
    void GetTemporalEntityInvInterval() {
        Response response = given()
                .param("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .param("timerel", "beyond")
                .param("time", "2020-09-01T14:20:00Z")
                .param("endtime", "2020-09-19T14:20:00Z")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidTemporalRelationParam"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid operator in <q> query")
    void GetEntityInvOperator() {
        Response response = given()
                .param("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .param("q", "referenceLevel>/15.0")
                .param("attrs", "id,currentLevel,referenceLevel")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidParameterValue"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid number of attributes")
    void GetEntityInvAttributes() {
        Response response = given()
                .param("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .param("q", "referenceLevel>15.0")
                .param("attrs", "id,currentLevel,referenceLevel,attr1,atte2,attr3,attr4")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidAttributeValue"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid attribute length")
    void GetTemporalEntityInvAttributeLength() {
        Response response = given()
                .param("id","b58da193-23d9-43eb-b98a-a103d4b6103c")
                .param("q", "referenceLevel>15.0")
                .param("attrs", "id,currentLevel,referenceLevelW45soq9yt0acejbMxwggziMSK8e7FsfylXQH3b5jDVXl6IQma7Ak6hfqlUldp3lf6K11Z0F2jJwm2cDC8lzxQ3KetN3dgn01Gv7G")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidAttributeValue"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid param in body")
    void CreateEntityInvGeoProperty() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=10")
                        .put("geoproperty", "location"))
                .put("invalid param", "invalid param");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.toString())
                .contentType("application/json")
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type",is("urn:dx:rs:invalidPayloadFormat"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) no geo property in request body")
    void CreateEntityEmptyGeoProperty() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=10"));

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.toString())
                .contentType("application/json")
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type",is("urn:dx:rs:invalidGeoParam"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid geometry in body")
    void CreateEntityInvGeometry() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "ellipse")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=10")
                        .put("geoproperty", "location"));

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.toString())
                .contentType("application/json")
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type",is("urn:dx:rs:invalidPayloadFormat"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid timerel in body")
    void CreateEntityInvTimerel() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834))
                        .put("georel", "near;maxDistance=1000")
                        .put("geoproperty", "location"))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "beyond")
                        .put("time", "2020-09-18T14:20:00Z")
                        .put("endtime", "2020-09-19T14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("q", "speed>30.0")
                .put("attrs", "id,speed");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.toString())
                .contentType("application/json")
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type",is("urn:dx:rs:invalidPayloadFormat"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid number of points in coordinate")
    void CreateEntityInvNoOfPoints() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Point")
                        .put("coordinates", new JsonArray().add(21.178).add(72.834).add(34.3189))
                        .put("georel", "near;maxDistance=1000")
                        .put("geoproperty", "location"))
                .put("temporalQ", new JsonObject()
                        .put("timerel", "beyond")
                        .put("time", "2020-09-18T14:20:00Z")
                        .put("endtime", "2020-09-19T14:20:00Z")
                        .put("timeProperty", "observationDateTime"))
                .put("q", "speed>30.0")
                .put("attrs", "id,speed");

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.toString())
                .contentType("application/json")
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type",is("urn:dx:rs:invalidPayloadFormat"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid geolocation")
    void CreateEntityInvGeoLocation() {
        JsonObject requestBody = new JsonObject()
                .put("type", "Query")
                .put("entities", new JsonArray().add(new JsonObject().put("id", id)))
                .put("geoQ", new JsonObject()
                        .put("geometry", "Polygon")
                        .put("coordinates", new JsonArray().add(new JsonArray()
                                .add(new JsonArray().add(72.719).add(21))
                                .add(new JsonArray().add(72.842).add(21.2))
                                .add(new JsonArray().add(72.923).add(20.8))
                                .add(new JsonArray().add(72.74).add(20.34))
                                .add(new JsonArray().add(72.9).add(20.1))
                                .add(new JsonArray().add(72.67).add(20))
                                .add(new JsonArray().add(72.719).add(21))
                        ))
                        .put("georel", "within")
                        .put("geoproperty", "geoJsonLocation"));

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(requestBody.toString())
                .contentType("application/json")
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .body("type",is("urn:dx:rs:invalidPayloadFormat"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing validations  - 400 (Bad Request) invalid json")
    void CreateEntityInvJson() {
        String jsonString = "{\n" +
                "    \"type\": \"Query\",\n" +
                "    \"entities\": [\n" +
                "        {\n" +
                "            \"id\": \"83c2e5c2-3574-4e11-9530-2b1fbdfce832\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"geoQ\": {\n" +
                "        \"geometry\": \"Polygon\",\n" +
                "        \"coordinates\": [[[72.719,21],[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20],[72.719,21]]],\n" +
                "        \"georel\": \"within\",\n" +
                "        \"geoproperty\": \"geoJsonLocation\"\n" +
                "    }";

        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .body(jsonString)
                .contentType("application/json")
                .when()
                .post("/entityOperations/query")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"))
                .extract()
                .response();
    }
}