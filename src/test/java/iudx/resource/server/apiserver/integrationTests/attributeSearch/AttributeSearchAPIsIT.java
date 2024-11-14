package iudx.resource.server.apiserver.integrationTests.attributeSearch;

import io.restassured.response.Response;
import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.resource.server.authenticator.TokensForITs.openResourceToken;
import static iudx.resource.server.authenticator.TokensForITs.publicKeyValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the Attribute Search APIs in the resource server. The tests cover various
 * scenarios for performing attribute searches with different operators (>, <, >=, <=) and combinations
 * of multiple attributes. The test cases include scenarios for success, empty responses, invalid
 * parameters, not found, invalid credentials, and invalid operators.
 * These tests use RestAssured for making HTTP requests and validating responses.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class AttributeSearchAPIsIT{
    private static final Logger LOGGER = LogManager.getLogger(AttributeSearchAPIsIT.class);
    String id = "b58da193-23d9-43eb-b98a-a103d4b6103c";
    @Test
    @DisplayName("testing get attribute search  - 200 (success) attribute >")
    void GetAttributeSearchOp1() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>21.0")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 200 (success) attribute with optional encryption >")
    void GetAttributeSearchOptionalEncrypOp1() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>21.0")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 204 (Empty Response) attribute >")
    void GetAttributeSearchEmptyRespOp1() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>150.0")
                .header("token", openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get attribute search  - 400 (Invalid parameters) attribute >")
    void GetAttributeSearchInvParamsOp1() {
        Response response = given()
                .param("id", id)
                .param("q1", "referenceLevel>15.0")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 404 (Not Found) attribute >")
    void GetAttributeSearchNotFoundOp1() {
        Response response = given()
                .param("id", "b58da193-23d9-43eb-b98a-a103d4b6106c")
                .param("q", "referenceLevel>15.0")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 401 (Invalid Credentials) attribute >")
    void GetAttributeSearchInvCredOp1() {
        Response response = given()
                .param("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .param("q", "speed>30")
                .header("token", "abc")
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
    @DisplayName("testing get attribute search  - 400 (Invalid Operator) attribute >")
    void GetAttributeSearchInvOperatorOp1() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>>15.0")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 200 (success) attribute <")
    void GetAttributeSearchOp2() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<16.0;measuredDistance>=14.89")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 204 (Empty Response) attribute <")
    void GetAttributeSearchEmptyRespOp2() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<1.0")
                .header("token", openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get attribute search  - 400 (Invalid parameters) attribute <")
    void GetAttributeSearchInvParamsOp2() {
        Response response = given()
                .param("id", id)
                .param("q1", "referenceLevel<1.0")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 404 (Not Found) attribute <")
    void GetAttributeSearchNotFoundOp2() {
        Response response = given()
                .param("id", "b58da193-23d9-43eb-b98a-a103d4b6106c")
                .param("q", "referenceLevel<1.0")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 401 (Invalid Credentials) attribute <")
    void GetAttributeSearchInvCredOp2() {
        Response response = given()
                .param("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .param("q", "speed<500")
                .header("token", "abc")
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
    @DisplayName("testing get attribute search  - 400 (Invalid Operator) attribute <")
    void GetAttributeSearchInvOperatorOp2() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<<15.0")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 200 (success) attribute >=")
    void GetAttributeSearchOp3() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>=20.9")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 204 (Empty Response) attribute >=")
    void GetAttributeSearchEmptyRespOp3() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>=150")
                .header("token", openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get attribute search  - 400 (Invalid parameters) attribute >=")
    void GetAttributeSearchInvParamsOp3() {
        Response response = given()
                .param("id", id)
                .param("q1", "referenceLevel>=15.9")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 404 (Not Found) attribute >=")
    void GetAttributeSearchNotFoundOp3() {
        Response response = given()
                .param("id", "b58da193-23d9-43eb-b98a-a103d4b6106c")
                .param("q", "referenceLevel>=15.9")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 401 (Invalid Credentials) attribute >=")
    void GetAttributeSearchInvCredOp3() {
        Response response = given()
                .param("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .param("q", "speed>=50")
                .header("token", "abc")
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
    @DisplayName("testing get attribute search  - 400 (Invalid Operator) attribute >=")
    void GetAttributeSearchInvOperatorOp3() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<>=50")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 200 (success) attribute <=")
    void GetAttributeSearchOp4() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<=15.9;measuredDistance>=14.89")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 204 (Empty Response) attribute <=")
    void GetAttributeSearchEmptyRespOp4() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<=1")
                .header("token", openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get attribute search  - 400 (Invalid parameters) attribute <=")
    void GetAttributeSearchInvParamsOp4() {
        Response response = given()
                .param("id", id)
                .param("q1", "referenceLevel<=1")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 404 (Not Found) attribute <=")
    void GetAttributeSearchNotFoundOp4() {
        Response response = given()
                .param("id", "b58da193-23d9-43eb-b98a-a103d4b6106c")
                .param("q", "referenceLevel<=1")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 401 (Invalid Credentials) attribute <=")
    void GetAttributeSearchInvCredOp4() {
        Response response = given()
                .param("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .param("q", "speed<=50")
                .header("token", "abc")
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
    @DisplayName("testing get attribute search  - 400 (Invalid Operator) attribute <=")
    void GetAttributeSearchInvOperatorOp4() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<>=1")
                .header("token", openResourceToken)
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
    @DisplayName("testing get attribute search  - 200 (success) multi attribute")
    void GetAttributeSearchMultiAttr() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>15.0;currentLevel==1.01;measuredDistance>=14.89")
                .header("token", openResourceToken)
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
}
