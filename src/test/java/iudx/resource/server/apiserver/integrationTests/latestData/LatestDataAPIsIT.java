package iudx.resource.server.apiserver.integrationTests.latestData;

import io.restassured.response.Response;
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
 * Integration tests for the Latest Data APIs in the resource server. The tests cover various scenarios
 * for retrieving the latest data from both secure and open resources. The test cases include scenarios
 * for success, not found, unauthorized access, and optional encryption.
 * These tests use RestAssured for making HTTP requests and validating responses.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class LatestDataAPIsIT {
    private static final Logger LOGGER = LogManager.getLogger(LatestDataAPIsIT.class);

    @Test
    @DisplayName("testing get latest data from secure resource - 200 (success)")
    void GetLatestDataSecureResource() {
        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities/83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get latest data from secure resource - 200 (success) with optional encryption")
    void GetLatestDataSecureResourceWithOptionalEncryp() {
        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                //optional encryption
                //.header("publicKey", publicKeyValue)
                .get("/entities/83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get latest data from open resource - 200 (success)")
    void GetLatestDataOpenResource() {
        Response response = given()
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities/b58da193-23d9-43eb-b98a-a103d4b6103c")
                .then()
                .statusCode(200)
                .body("type", is("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get latest data from open resource - 404 (Not Found)")
    void GetLatestDataOpenResourceNotFound() {
        Response response = given()
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities/b58da193-23d9-43eb-b98a-a103d4b6102c")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get latest data from open resource - 401 (Not Authorized)")
    void GetLatestDataOpenResourceUnAuth() {
        Response response = given()
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .when()
                .get("/entities/b58da193-23d9-43eb-b98a-a103d4b6103c")
                .then()
                .statusCode(401)
                .body("type", is("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }
}
