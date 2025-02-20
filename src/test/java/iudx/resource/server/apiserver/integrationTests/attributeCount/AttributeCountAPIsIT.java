package iudx.resource.server.apiserver.integrationTests.attributeCount;

import io.restassured.response.Response;
import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.*;
import static iudx.resource.server.authenticator.TokensForITs.openResourceToken;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for the Attribute Count APIs in the resource server. The tests cover various
 * scenarios for performing attribute counts with different operators (>, <, >=, <=) and combinations
 * of multiple attributes. The test cases include scenarios for success, empty responses, invalid
 * parameters, not found, invalid credentials, and invalid operators.
 * These tests use RestAssured for making HTTP requests and validating responses.
 */
@ExtendWith(RestAssuredConfiguration.class)
public class AttributeCountAPIsIT {
    private static final Logger LOGGER = LogManager.getLogger(AttributeCountAPIsIT.class);
    String id= "b58da193-23d9-43eb-b98a-a103d4b6103c";

    @Test
    @DisplayName("testing get attribute count  - 200 (success) attribute >")
    void GetAttributeCountOp1() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>15.0")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 200 (success) attribute with optional encryption >")
    void GetAttributeCountOptionalEncrypOp1() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>15.0")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 204 (Empty Response) attribute >")
    void GetAttributeCountEmptyRespOp1() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>150.0")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get attribute count  - 400 (Invalid parameters) attribute >")
    void GetAttributeCountInvParamsOp1() {
        Response response = given()
                .param("id", id)
                .param("q1", "referenceLevel>150.0")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 404 (Not Found) attribute >")
    void GetAttributeCountNotFoundOp1() {
        Response response = given()
                .param("id", "b58da193-23d9-43eb-b98a-a103d4b6106c")
                .param("q", "referenceLevel>150.0")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 401 (Invalid Credentials) attribute >")
    void GetAttributeCountInvCredOp1() {
        Response response = given()
                .param("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .param("q", "speed>30")
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
    @DisplayName("testing get attribute count  - 400 (Invalid Operator) attribute >")
    void GetAttributeCountInvOperatorOp1() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>>15.0")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 200 (success) attribute <")
    void GetAttributeCountOp2() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<16.0")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 204 (Empty Response) attribute <")
    void GetAttributeCountEmptyRespOp2() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<1.0")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get attribute count  - 400 (Invalid parameters) attribute <")
    void GetAttributeCountInvParamsOp2() {
        Response response = given()
                .param("id", id)
                .param("q1", "referenceLevel<1.0")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 404 (Not Found) attribute <")
    void GetAttributeCountNotFoundOp2() {
        Response response = given()
                .param("id", "b58da193-23d9-43eb-b98a-a103d4b6106c")
                .param("q", "referenceLevel<1.0")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 401 (Invalid Credentials) attribute <")
    void GetAttributeCountInvCredOp2() {
        Response response = given()
                .param("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .param("q", "speed<500")
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
    @DisplayName("testing get attribute count  - 400 (Invalid Operator) attribute <")
    void GetAttributeCountInvOperatorOp2() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<<15.0")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 200 (success) attribute >=")
    void GetAttributeCountOp3() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>=15.9")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 204 (Empty Response) attribute >=")
    void GetAttributeCountEmptyRespOp3() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>=150")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get attribute count  - 400 (Invalid parameters) attribute >=")
    void GetAttributeCountInvParamsOp3() {
        Response response = given()
                .param("id", id)
                .param("q1", "referenceLevel>=15.9")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 404 (Not Found) attribute >=")
    void GetAttributeCountNotFoundOp3() {
        Response response = given()
                .param("id", "b58da193-23d9-43eb-b98a-a103d4b6106c")
                .param("q", "referenceLevel>=15.9")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 401 (Invalid Credentials) attribute >=")
    void GetAttributeCountInvCredOp3() {
        Response response = given()
                .param("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .param("q", "speed>=50")
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
    @DisplayName("testing get attribute count  - 400 (Invalid Operator) attribute >=")
    void GetAttributeCountInvOperatorOp3() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<>=50")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 200 (success) attribute <=")
    void GetAttributeCountOp4() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<=15.9")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 204 (Empty Response) attribute <=")
    void GetAttributeCountEmptyRespOp4() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<=1")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
                .contentType("application/json")
                .when()
                .get("/entities")
                .then()
                .statusCode(204)
                .extract()
                .response();
    }

    @Test
    @DisplayName("testing get attribute count  - 400 (Invalid parameters) attribute <=")
    void GetAttributeCountInvParamsOp4() {
        Response response = given()
                .param("id", id)
                .param("q1", "referenceLevel<=1")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 404 (Not Found) attribute <=")
    void GetAttributeCountNotFoundOp4() {
        Response response = given()
                .param("id", "b58da193-23d9-43eb-b98a-a103d4b6106c")
                .param("q", "referenceLevel<=1")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 401 (Invalid Credentials) attribute <=")
    void GetAttributeCountInvCredOp4() {
        Response response = given()
                .param("id", "83c2e5c2-3574-4e11-9530-2b1fbdfce832")
                .param("q", "speed<=50")
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
    @DisplayName("testing get attribute count  - 400 (Invalid Operator) attribute <=")
    void GetAttributeCountInvOperatorOp4() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel<>=1")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
    @DisplayName("testing get attribute count  - 200 (success) multi attribute")
    void GetAttributeCountMultiAttr() {
        Response response = given()
                .param("id", id)
                .param("q", "referenceLevel>15.0;currentLevel==1.01;measuredDistance>=14.89")
                .param("options", "count")
                .header("Authorization", "Bearer " + openResourceToken)
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
