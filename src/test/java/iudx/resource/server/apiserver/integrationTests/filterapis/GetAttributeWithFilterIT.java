package iudx.resource.server.apiserver.integrationTests.filterapis;

import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;

import static iudx.resource.server.authenticator.TokensForITs.openResourceToken;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@ExtendWith(RestAssuredConfiguration.class)
public class GetAttributeWithFilterIT {
    String filterId="b58da193-23d9-43eb-b98a-a103d4b6103c";
    String q="referenceLevel>15.0";
    String attrs="id,currentLevel,referenceLevel";
    @Test
    @DisplayName("200 (success) attribute > with filter")
    public void getAttributeWithFilterTest(){
        given()
                .queryParam("id",filterId)
                .queryParam("q", q+";measuredDistance>=14.89")
                .queryParam("attrs", attrs)
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/entities")
                .then()
                .statusCode(200)
                // .log().body()
                .body("title", equalTo("Success"))
                .body("type", equalTo("urn:dx:rs:success"))
                .body("results[0]", notNullValue())
                .body("results[0].id", notNullValue())
                .body("results[0].currentLevel", notNullValue())
                .body("results[0].referenceLevel", greaterThan(15.0f));
    }
    @Test
    @DisplayName("200 (success) attribute > with filter with optional encryption")
    public void getAttributeWithFilterWithOptionalEncryptionTest(){
        given()
                .queryParam("id",filterId)
                .queryParam("q", q+";measuredDistance>=14.89")
                .queryParam("attrs", attrs)
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/entities")
                .then()
                .statusCode(200)
                //.log().body()
                .body("title", equalTo("Success"))
                .body("type", equalTo("urn:dx:rs:success"))
                .body("results[0]", notNullValue())
                .body("results[0].id", notNullValue())
                .body("results[0].currentLevel", notNullValue())
                .body("results[0].referenceLevel", greaterThan(15.0f));
    }

    @Test
    @DisplayName("413 (Payload too large) attribute > with filter with optional encryption")
    public void getAttributeWithFilterWithOptionalEncryptionTestPayLoadTooLarge(){
        given()
                .queryParam("id",filterId)
                .queryParam("q", q)
                .queryParam("attrs", attrs)
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/entities")
                .then()
                .statusCode(413)
                .body("title", equalTo("Payload Too Large"));
    }
    @Test
    @DisplayName("204 (Empty Response) attribute > with filter")
    public void getAttributeWithFilterWithEmptyResponseTest(){
        given()
                .queryParam("id",filterId)
                .queryParam("q", "referenceLevel>150.0")
                .queryParam("attrs", attrs)
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/entities")
                .then()
                .statusCode(204);
        //.log().body();
    }
    @Test
    @DisplayName("400 (invalid params)attribute > with filter")
    public void getAttributeWithFilterWithInvalidParamsTest(){
        given()
                .queryParam("id",filterId)
                .queryParam("q", q)
                .queryParam("attributes", attrs)
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                //.log().body()
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidParamameter"));
    }
    @Test
    @DisplayName("400 (invalid operator)attribute > with filter")
    public void getAttributeWithFilterWithInvalidOperatorTest(){
        given()
                .queryParam("id",filterId)
                .queryParam("q", "referenceLevel>>150.0")
                .queryParam("attributes", attrs)
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/entities")
                .then()
                .statusCode(400)
                // .log().body()
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidParameterValue"));
    }
    @Test
    @DisplayName("404 (not found)attribute > with filter")
    public void getNotFoundAttributeWithFilterTest(){
        String nonExistingFilterId="b58da193-23d9-43eb-b98a-a103d4b6101c";
        given()
                .queryParam("id",nonExistingFilterId)
                .queryParam("q", q)
                .queryParam("attributes", attrs)
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/entities")
                .then()
                .statusCode(404)
                //.log().body()
                .body("title", equalTo("Not Found"))
                .body("type", equalTo("urn:dx:rs:resourceNotFound"));
    }
    @Test
    @DisplayName("401 (not authorized)attribute > with filter")
    public void getAttributeWithFilterWithInvalidTokenTest(){
        given()
                .queryParam("id",filterId)
                .queryParam("q", q)
                .queryParam("attributes", attrs)
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .when()
                .get("/entities")
                .then()
                .statusCode(401)
                //.log().body()
                .body("title", equalTo("Not Authorized"))
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"));
    }
}