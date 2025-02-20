package iudx.resource.server.apiserver.integrationTests.resetPasswordAPI;

import io.restassured.response.Response;
import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.resource.server.authenticator.TokensForITs.secureResourceToken;
import static org.hamcrest.Matchers.equalTo;

/**
 * Integration tests for the Reset Password APIs in the Resource Server.
 * These tests cover various scenarios such as resetting user passwords with different HTTP response codes.
 * The tests include cases for successful password reset (HTTP 200), password reset on a non-existent endpoint (HTTP 404),
 * and unauthorized password reset attempt (HTTP 401).
 */
@ExtendWith(RestAssuredConfiguration.class)
public class ResetPasswordAPIsIT {
    private static final Logger LOGGER = LogManager.getLogger(ResetPasswordAPIsIT.class);

    @Test
    @DisplayName("testing Reset Password  - 200 (Success) Reset user password")
    void ResetUserPwd() {
        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .post("/user/resetPassword")
                .then()
                .statusCode(200)
                .body("title", equalTo("successful"))
                .body("detail", equalTo("Successfully changed the password"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Reset Password  - 404 (Not Found) Reset user password")
    void ResetUserPwdNotFound() {
        Response response = given()
                .header("Authorization", "Bearer " + secureResourceToken)
                .contentType("application/json")
                .when()
                .post("/user/resetPasswords")
                .then()
                .statusCode(404)
                .body("title", equalTo("Not Found"))
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing Reset Password  - 401 (Not Authorized) Reset user password")
    void ResetUserPwdUnAuth() {
        Response response = given()
                .header("Authorization", "Bearer " + "abc")
                .contentType("application/json")
                .when()
                .post("/user/resetPassword")
                .then()
                .statusCode(401)
                .body("title", equalTo("Not Authorized"))
                .extract()
                .response();
    }
}
