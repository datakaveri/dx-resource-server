package iudx.resource.server.apiserver.integrationTests.consumerauditapis;

import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;

import static iudx.resource.server.authenticator.TokensForITs.secureResourceToken;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class ConsumerAuditAPIsIT {
    String consumerAuditId="b58da193-23d9-43eb-b98a-a103d4b6103c";
    String timerel="between";
    String time="2023-06-20T00:00:00Z";
    String endTime="2023-06-21T16:00:00Z";
    String options="count";
    String api="/ngsi-ld/v1/temporal/entities";
    String invalidToken="abcd";
    @Test
    @DisplayName("200 (success) Get total API calls made")
    public void getConsumerAuditTotalAPICallsTest(){
        given()
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("endTime", endTime)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + secureResourceToken)
                .header("options",options)
                .when()
                .get("/consumer/audit")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("401 (not authorized) Get total API calls made")
    public void getConsumerAuditTotalAPICallsWithInvalidTokenTest(){

        given()
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("endTime", endTime)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + invalidToken)
                .header("options",options)
                .when()
                .get("/consumer/audit")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"))
                .extract().response();
    }
    @Test
    @DisplayName("200 (success) Get Data Read Query")
    public void getConsumerAuditDataReadQueryTest(){
        given()
                .queryParam("id",consumerAuditId)
                .queryParam("timerel", "during")
                .queryParam("time", time)
                .queryParam("endTime", endTime)
                .queryParam("api",api)
                .queryParam("offset",0)
                .queryParam("limit",2000)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + secureResourceToken)
                .when()
                .get("/consumer/audit")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("401 (not authorised) Get Data Read Query")
    public void getConsumerAuditDataReadQueryWithInvalidTokenTest(){
        given()
                .queryParam("id",consumerAuditId)
                .queryParam("timerel", "during")
                .queryParam("time", time)
                .queryParam("endTime", endTime)
                .queryParam("api",api)
                .queryParam("offset",0)
                .queryParam("limit",2000)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + invalidToken)
                .when()
                .get("/consumer/audit")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"))
                .extract().response();
    }
    @Test
    @DisplayName("200 (success) Get Data Count Query")
    public void getConsumerAuditDataCountQueryTest(){
        given()
                .queryParam("id",consumerAuditId)
                .queryParam("timerel", "during")
                .queryParam("time", time)
                .queryParam("endTime", endTime)
                .queryParam("api",api)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + secureResourceToken)
                .header("options",options)
                .when()
                .get("/consumer/audit")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("204 (failure) Get Data Read Query")
    public void getConsumerAuditDataReadQueryFailureTest(){
        given()
                .queryParam("id",consumerAuditId)
                .queryParam("timerel", "during")
                .queryParam("time", "2021-10-20T14:20:00Z")
                .queryParam("endTime", "2021-10-24T14:20:00Z")
                .queryParam("api",api)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + secureResourceToken)
                .when()
                .get("/consumer/audit")
                .then()
                .statusCode(204);
        //.log().body();
    }
    @Test
    @DisplayName("400 (failure) Get Data Read Query")
    public void getConsumerAuditDataReadQueryBadRequestTest(){
        given()
                .queryParam("id",consumerAuditId)
                .queryParam("timerel", "during")
                .queryParam("time", "2021-12-20T14:20:00Z")
                .queryParam("endTime", "2021-11-24T14:20:00Z")
                .queryParam("api",api)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + secureResourceToken)
                .header("options",options)
                .when()
                .get("/consumer/audit")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:badRequest"))
                .body("title", equalTo("Bad Request"))
                .extract().response();
    }

}