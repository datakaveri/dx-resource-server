package iudx.resource.server.apiserver.integrationTests.summaryapis;

import iudx.resource.server.apiserver.integrationTests.RestAssuredConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;


import static iudx.resource.server.authenticator.TokensForITs.*;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class SummaryAPIsIT {
    @Test
    @DisplayName("200 - (Success)ForAllSubscriptionQueue")
    public void getAllSubscriptionQueueSuccessTest(){
        given()
                .header("Content-Type", "application/json")
                .header("token", secureResourceToken)
                .when()
                .get("/subscription")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("200 - (Success)ForAllAdapaterExchange")
    public void getAllAdapterExchangeSuccessTest(){
        given()
                .header("Content-Type", "application/json")
                .header("token", delegateToken)
                .when()
                .get("/ingestion")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("401 - (Not Authorized)ForAllSubscriptionQueue")
    public void getAllSubscriptionQueueWithInvalidTokenTest(){
        String invalidToken="123";
        given()
                .header("Content-Type", "application/json")
                .header("token", invalidToken)
                .when()
                .get("/subscription")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"))
                .extract().response();
    }
    @Test
    @DisplayName("401 - (Not Authorized)ForAllAdapaterExchange")
    public void getAllAdapterExchangeWithInvalidTokenTest(){
        given()
                .header("Content-Type", "application/json")
                .header("token", secureResourceToken)
                .when()
                .get("/ingestion")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"))
                .extract().response();
    }
    @Test
    @DisplayName("200 - (Success)Overview Delegate/Provider without Params")
    public void getOverviewDelegateWithoutParamsSuccessTest(){
        given()
                .header("Content-Type", "application/json")
                .header("token", delegateToken)
                .when()
                .get("/overview")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("401 - (Not Authorized)Overview")
    public void getOverviewDelegateWithoutParamsWithInvalidTokenSuccessTest(){
        String invalidToken="123";
        given()
                .header("Content-Type", "application/json")
                .header("token", invalidToken)
                .when()
                .get("/overview")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"))
                .extract().response();
    }
    @Test
    @DisplayName("200 - (Success)Overview Consumer with Params")
    public void getOverviewConsumerWithParamsSuccessTest(){
        given()
                .queryParam("starttime","2022-11-01T00:00:00Z")
                .queryParam("endtime","2022-12-31T00:00:00Z")
                .header("Content-Type", "application/json")
                .header("token", secureResourceToken)
                .when()
                .get("/overview")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("200 - (Success)Overview Admin with Params")
    public void getOverviewAdminWithParamsSuccessTest(){
        given()
                .queryParam("starttime","2022-11-01T00:00:00Z")
                .queryParam("endtime","2022-11-30T23:59:00Z")
                .header("Content-Type", "application/json")
                .header("token", adminToken)
                .when()
                .get("/overview")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("400 - (Bad Request)Overview")
    public void getOverviewBadRequestTest(){
        given()
                .queryParam("starttime","2022-11-20T00:00:00Z")
                .header("Content-Type", "application/json")
                .header("token", delegateToken)
                .when()
                .get("/overview")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:general"))
                .body("title", equalTo("Bad Request"))
                .extract().response();
    }
    @Test
    @DisplayName("200 - (Success)Overview Delegate/Provider with Params")
    public void getOverviewDelegateWithParamsSuccessTest(){
        given()
                .queryParam("starttime","2022-11-20T00:00:00Z")
                .queryParam("endtime","2023-01-29T00:00:00Z")
                .header("Content-Type", "application/json")
                .header("token", delegateToken)
                .when()
                .get("/overview")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("200 - (Success)Overview Consumer without Params")
    public void getOverviewConsumerWithoutParamsSuccessTest(){
        given()
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/overview")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("200 - (Success)Overview Admin wihout Params")
    public void getOverviewAdminWithoutParamsSuccessTest(){
        given()
                .header("Content-Type", "application/json")
                .header("token", adminToken)
                .when()
                .get("/overview")
                .then()
                .statusCode(200)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("200- (Success) Summary")
    public void getSummarySuccessTest(){
        given()
                .queryParam("starttime","2022-11-20T00:00:00Z")
                .queryParam("endtime","2023-01-27T00:00:00Z")
                .header("Content-Type", "application/json")
                .header("token", adminToken)
                .when()
                .get("/summary")
                .then()
                .statusCode(200)
                // .log().body()
                .body("type", equalTo("urn:dx:dm:Success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("401- (Not Authorized Token) Summary")
    public void getSummaryWithInvalidTokenSuccessTest(){
        String invalidToken="123";
        given()
                .header("Content-Type", "application/json")
                .header("token", invalidToken)
                .when()
                .get("/summary")
                .then()
                .statusCode(401)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"))
                .extract().response();
    }
    @Test
    @DisplayName("400 - (Bad Request)Summary")
    public void getSummaryBadRequestTest(){
        given()
                .queryParam("starttime","2022-11-20T00:00:00Z")
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/summary")
                .then()
                .statusCode(400)
                //.log().body()
                .body("type", equalTo("urn:dx:rs:general"))
                .body("title", equalTo("Bad Request"))
                .extract().response();
    }
    @Test
    @DisplayName("204- (No Content) Summary")
    public void getSummaryWithNoContentTest(){
        given()
                .queryParam("starttime","2022-01-20T00:00:00Z")
                .queryParam("endtime","2022-10-27T00:00:00Z")
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/summary")
                .then()
                .statusCode(204)
                //.log().body()
                .extract().response();
    }
    @AfterEach
    public void tearDown() {
        // Introduce a delay
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}