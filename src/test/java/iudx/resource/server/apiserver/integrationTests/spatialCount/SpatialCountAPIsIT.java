package iudx.resource.server.apiserver.integrationTests.spatialCount;

import io.restassured.response.Response;
import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import iudx.resource.server.apiserver.integrationTests.spatialSearch.SpatialSearchAPIsIT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.resource.server.authenticator.TokensForITs.publicKeyValue;
import static iudx.resource.server.authenticator.TokensForITs.secureResourceToken;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the spatial count APIs in the resource server. The tests cover various
 * scenarios for performing spatial count queries such as number of Geo queries (circle, polygon, bbox, line string),
 * and include positive and negative cases for different parameters and conditions.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class SpatialCountAPIsIT {
    private static final Logger LOGGER = LogManager.getLogger(SpatialCountAPIsIT.class);
    String id = "83c2e5c2-3574-4e11-9530-2b1fbdfce832";

    @Test
    @DisplayName("testing get Geo query (circle) - 200")
    void GetGeoQueryCircle() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "near;maxdistance=10")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834]")
                .param("options","count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (circle) with optional encryption - 200")
    void GetGeoQueryCircleOptEncryption() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "near;maxdistance=10")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834]")
                .param("options","count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                //optional encryption
                //.header("publicKey", publicKeyValue)
                .get("/entities")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (circle) - 204 Empty Response")
    void GetGeoQueryCircleEmptyResp() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "near;maxdistance=1")
                .param("geometry", "Point")
                .param("coordinates", "[31.178,72.834]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (circle) - 400 Invalid Params")
    void GetGeoQueryCircleInvParams() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georelation", "near;maxdistance=10")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("type",is("urn:dx:rs:invalidParamameter"))
                .body("title", is("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (circle) - 401 Invalid Credentials")
    void GetGeoQueryCircleInvCredentials() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "near;maxdistance=10")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834]")
                .param("options", "count")
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(401)
                .body("type",is("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", is("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (circle) - 404 Not Found")
    void GetGeoQueryCircleNotFound() {
        Response response = given()
                .param("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce831")
                .param("geoproperty", "location")
                .param("georel", "near;maxdistance=1000")
                .param("geometry", "Point")
                .param("coordinates", "[21.178,72.834]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(404)
                .body("type",is("urn:dx:rs:resourceNotFound"))
                .body("title", is("Not Found"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (polygon) - 200")
    void GetGeoQueryPolygon() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "within")
                .param("geometry", "Polygon")
                .param("coordinates", "[[[72.719,21],[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20],[72.719,21]]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (polygon) - 204 Empty Response")
    void GetGeoQueryPolygonEmptyResp() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "within")
                .param("geometry", "Polygon")
                .param("coordinates", "[[[72.719,31],[72.842,31.2],[72.67,30],[72.719,31]]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (polygon) - 400 Invalid Params")
    void GetGeoQueryPolygonInvParams() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georelation", "within")
                .param("geometry", "Polygon")
                .param("coordinates", "[[[72.719,21],[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20],[72.719,21]]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", is("Bad Request"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (polygon) - 401 Invalid Credentials")
    void GetGeoQueryPolygonInvCredentials() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "within")
                .param("geometry", "Polygon")
                .param("coordinates", "[[[72.76,21.15],[72.76,21.13],[72.78,21.13],[72.78,21.15],[72.76,21.15]]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (polygon) - 404 Not Found")
    void GetGeoQueryPolygonNotFound() {
        Response response = given()
                .param("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce831")
                .param("geoproperty", "location")
                .param("georel", "within")
                .param("geometry", "Polygon")
                .param("coordinates", "[[[72.76,21.15],[72.76,21.13],[72.78,21.13],[72.78,21.15],[72.76,21.15]]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (bbox) - 200")
    void GetGeoQueryBbox() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "within")
                .param("geometry", "bbox")
                .param("coordinates", "[[72.8296,21.2],[72.8297,21.15]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (bbox) - 204 Empty Response")
    void GetGeoQueryBbox204() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "within")
                .param("geometry", "bbox")
                .param("coordinates", "[[72.8296,31.2],[72.8297,31.15]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (bbox) - 400 Invalid Params")
    void GetGeoQueryBboxInvParams() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georelation", "within")
                .param("geometry", "bbox")
                .param("coordinates", "[[72.8296,31.2],[72.8297,31.15]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                .body("title", is("Bad Request"))
                .extract()
                .response();
    }
    @Test
    @DisplayName("testing get Geo query (bbox) - 401 Invalid Credentials")
    void GetGeoQueryBboxInvCredentials() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "within")
                .param("geometry", "bbox")
                .param("coordinates", "[[72.8296,31.2],[72.8297,31.15]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(401)
                .body("title",is("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (bbox) - 404 Not Found")
    void GetGeoQueryBboxNotFound() {
        Response response = given()
                .param("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce831")
                .param("geoproperty", "location")
                .param("georel", "within")
                .param("geometry", "bbox")
                .param("coordinates", "[[72.8296,31.2],[72.8297,31.15]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(404)
                .body("title", is("Not Found"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (Line String) - 200 Success")
    void GetGeoQueryLineString() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "intersects")
                .param("geometry", "linestring")
                .param("coordinates", "[[72.84,21.19],[72.84,21.17]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (Line String) - 204 Empty Response")
    void GetGeoQueryLineStringEmptyResp() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "intersects")
                .param("geometry", "linestring")
                .param("coordinates", "[[72.842,31.2],[72.923,30.8],[72.74,30.34],[72.9,30.1],[72.67,30]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (Line String) - 400 Invalid Params")
    void GetGeoQueryLineStringInvParams() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georelation", "intersects")
                .param("geometry", "linestring")
                .param("coordinates", "[[72.84,21.19],[72.84,21.17]]")
                .param("options", "count")
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
    @DisplayName("testing get Geo query (Line String) - 401 Invalid Credentials")
    void GetGeoQueryLineStringInvCredentials() {
        Response response = given()
                .param("id", id)
                .param("geoproperty", "location")
                .param("georel", "intersects")
                .param("geometry", "linestring")
                .param("coordinates", "[[72.84,21.19],[72.84,21.17]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(401)
                .body("title", is("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get Geo query (Line String) - 404 Not Found")
    void GetGeoQueryLineStringNotFound() {
        Response response = given()
                .param("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce830")
                .param("geoproperty", "location")
                .param("georel", "intersects")
                .param("geometry", "linestring")
                .param("coordinates", "[[72.84,21.19],[72.84,21.17]]")
                .param("options", "count")
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(404)
                .body("title", is("Not Found"))
                .extract()
                .response();
    }
}
