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
public class GetBetweenTemporalAttributeWithFilterIT {
    String filterId="b58da193-23d9-43eb-b98a-a103d4b6103c";
    String timerel="between";
    String time="2020-10-18T14:20:00Z";
    String endTime="2020-10-19T14:20:00Z";
    String attrs="id,currentLevel,referenceLevel";
    @Test
    @DisplayName("200 (success) attribute > with filter")
    public void getTemporalAttributeWithFilterTest(){
        given()
                .queryParam("id",filterId)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("endtime",endTime)
                .queryParam("attrs",attrs)
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
                .body("results[0].id", notNullValue())
                .body("results[0].currentLevel", notNullValue())
                .body("results[0].referenceLevel", greaterThan(15.0f));
    }
    @Test
    @DisplayName("204 (Empty Response) temporal (between) with filter")
    public void getTemporalAttributeWithFilterWithEmptyResponseTest(){
        given()
                .queryParam("id",filterId)
                .queryParam("timerel", timerel)
                .queryParam("time", "2020-01-18T14:20:00Z")
                .queryParam("endtime","2020-01-19T14:20:00Z")
                .queryParam("attrs",attrs)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(204);
        //.log().body();
    }
    @Test
    @DisplayName("400 (invalid date format) temporal (between) with filter")
    public void getTemporalAttributeWithFilterWithInvalidDateFormatTest(){
        String nonExistingFilterId="b58da193-23d9-43eb-b98a-a103d4b6101c";
        given()
                .queryParam("id",nonExistingFilterId)
                .queryParam("timerel", timerel)
                .queryParam("time", "2020-09-18X14:20:00Z")
                .queryParam("endtime","2020-09-19X14:20:00Z")
                .queryParam("attrs",attrs)
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
    @DisplayName("404 (not found) temporal (between) with filter")
    public void getNotFoundTemporalAttributeWithFilterTest(){
        String nonExistingFilterId="b58da193-23d9-43eb-b98a-a103d4b6101c";
        given()
                .queryParam("id",nonExistingFilterId)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("endtime",endTime)
                .queryParam("attrs",attrs)
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
    @DisplayName("401 (not authorized)attribute > with filter")
    public void getAttributeWithFilterWithInvalidTokenTest(){
        given()
                .queryParam("id",filterId)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("endtime",endTime)
                .queryParam("attrs",attrs)
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