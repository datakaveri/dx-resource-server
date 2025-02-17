package iudx.resource.server.apiserver.integrationTests.temporalcountapis;

import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;

import static iudx.resource.server.authenticator.TokensForITs.openResourceToken;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(RestAssuredConfiguration.class)
public class GetAfterTemporalCountIT {
    String temporalId="b58da193-23d9-43eb-b98a-a103d4b6103c";
    @Test
    @DisplayName("200 (success) temporal (after)")
    public void getTemporalCountTest(){
        given()
                .queryParam("id",temporalId)
                .queryParam("timerel", "after")
                .queryParam("time", "2020-10-18T14:20:01Z")
                .queryParam("options","count")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(200)
                //.log().body()
                .body("title", equalTo("Success"))
                .body("type", equalTo("urn:dx:rs:success"))
                .body("results[0]", notNullValue())
                .body("results[0].totalHits", is(notNullValue()));
    }
    @Test
    @DisplayName("204 (Empty Response) temporal (after)")
    public void getTemporalCountWithEmptyResponseTest(){
        given()
                .queryParam("id",temporalId)
                .queryParam("timerel", "after")
                .queryParam("time", "2018-01-19T14:20:00Z")
                .queryParam("options","count")
                .header("Authorization", "Bearer " + openResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(204);
        //.log().body();
    }
    @Test
    @DisplayName("400 (Invalid params) temporal (after)")
    public void getTemporalCountWithInvalidParamsTest(){
        given()
                .queryParam("id",temporalId)
                .queryParam("timerelation", "after")
                .queryParam("timea", "2020-06-01T14:20:01Z")
                .queryParam("options","count")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(400)
                //.log().body()
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidTemporalRelationParam"));
    }
    @Test
    @DisplayName("400 (invalid date format) temporal (after)")
    public void getTemporalCountWithInvalidDateFormatTest(){
        given()
                .queryParam("id",temporalId)
                .queryParam("timerel", "after")
                .queryParam("time", "2020-06-01X14:20:01Z")
                .queryParam("options","count")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(400)
                //.log().body()
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidAttributeValue"));
    }
    @Test
    @DisplayName("404 (not found) temporal (after)")
    public void TemporalCountNotFoundTest(){
        String nonExistingTemporalId="b58da193-23d9-43eb-b98a-a103d4b6102c";
        given()
                .queryParam("id",nonExistingTemporalId)
                .queryParam("timerel", "after")
                .queryParam("time", "2020-06-01T14:20:01Z")
                .queryParam("options","count")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(404)
                //.log().body()
                .body("title", equalTo("Not Found"))
                .body("type", equalTo("urn:dx:rs:resourceNotFound"));
    }
    @Test
    @DisplayName("401(invalid credentials) temporal (after)")
    public void getTemporalCountWithInvalidCredentialsTest(){
        given()
                .queryParam("id",temporalId)
                .queryParam("timerel", "after")
                .queryParam("time", "2020-06-01T14:20:01Z")
                .queryParam("options","count")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + "abc")
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(401)
                //.log().body()
                .body("title", equalTo("Not Authorized"))
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"));
    }
}