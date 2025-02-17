package iudx.resource.server.apiserver.integrationTests.providerAuditAPI;

import io.restassured.response.Response;
import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.given;
import static iudx.resource.server.authenticator.TokensForITs.providerToken;
import static org.hamcrest.Matchers.equalTo;

/**
 * Integration tests for the Provider Audit APIs in the Resource Server.
 * These tests cover various scenarios such as querying data read and count operations with different HTTP response codes.
 * The tests include cases for successful data read query (HTTP 200), unauthorized data read query attempt (HTTP 401),
 * successful data count query (HTTP 200), empty response for data count query (HTTP 204), and bad request for data count query (HTTP 400).
 */
@ExtendWith(RestAssuredConfiguration.class)
public class ProviderAuditAPIsIT {
    private static final Logger LOGGER = LogManager.getLogger(ProviderAuditAPIsIT.class);

    @Test
    @DisplayName("testing provider audit API  - 200 (Success) Get Data Read Query")
    void GetDataReadQuery() {
        Response response = given()
                .param("timerel", "during")
                .param("time", "2023-06-05T14:20:00Z")
                .param("endTime", "2023-07-05T14:20:00Z")
                .param("api","/"+basePath+"/entityOperations/query")
                .param("providerID","b2c27f3f-2524-4a84-816e-91f9ab23f837")
                .param("consumer", "15c7506f-c800-48d6-adeb-0542b03947c6")
                .param("offset",0)
                .param("limit", 2000)
                .header("Authorization", "Bearer " + providerToken)
                .contentType("application/json")
                .when()
                .get("/provider/audit")
                .then()
                .statusCode(200)
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing provider audit API  - 401 (Not Authorized) Get Data Read Query")
    void GetDataReadQueryUnAuth() {
        Response response = given()
                .param("id","83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .param("timerel", "during")
                .param("time", "2021-11-20T14:20:00Z")
                .param("endTime", "2021-12-02T14:20:00Z")
                .param("api","/"+basePath+"/entityOperations/query")
                .param("providerID","b2c27f3f-2524-4a84-816e-91f9ab23f837")
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .when()
                .get("/provider/audit")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing provider audit API  - 200 (Success) Get Data Count Query")
    void GetDataCountQuery() {
        Response response = given()
                .param("id","83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .param("timerel", "during")
                .param("time", "2023-06-05T14:20:00Z")
                .param("endTime", "2023-07-05T14:20:00Z")
                .param("api","/"+basePath+"/entityOperations/query")
                .param("providerID","b2c27f3f-2524-4a84-816e-91f9ab23f837")
                .header("Authorization", "Bearer " + providerToken)
                .header("options", "count")
                .contentType("application/json")
                .when()
                .get("/provider/audit")
                .then()
                .statusCode(200)
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing provider audit API  - 204 (Failure) Get Data Count Query")
    void GetDataCountQueryEmptyResp() {
        Response response = given()
                .param("id","83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .param("timerel", "during")
                .param("time", "2020-11-20T14:20:00Z")
                .param("endTime", "2020-11-24T14:20:00Z")
                .param("api","/"+basePath+"/entityOperations/query")
                .param("providerID","b2c27f3f-2524-4a84-816e-91f9ab23f837")
                .header("Authorization", "Bearer " + providerToken)
                .header("options", "count")
                .contentType("application/json")
                .when()
                .get("/provider/audit")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing provider audit API  - 400 (Bad Request) Get Data Count Query")
    void GetDataCountQueryBadRequest() {
        Response response = given()
                .param("id","83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .param("timerel", "during")
                .param("time", "2021-11-20T14:20:00Z")
                .param("endTime", "2020-11-02T14:20:00Z")
                .param("api","/"+basePath+"/entityOperations/query")
                .param("providerID","b2c27f3f-2524-4a84-816e-91f9ab23f837")
                .header("Authorization", "Bearer " + providerToken)
                .header("options", "count")
                .contentType("application/json")
                .when()
                .get("/provider/audit")
                .then()
                .statusCode(400)
                .extract()
                .response();
    }
}
