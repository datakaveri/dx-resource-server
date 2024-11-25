package iudx.resource.server.apiserver.query;

import static iudx.resource.server.apiserver.util.Constants.JSON_ATTRIBUTE;
import static iudx.resource.server.apiserver.util.Constants.JSON_ATTR_QUERY;
import static iudx.resource.server.apiserver.util.Constants.JSON_LAT;
import static iudx.resource.server.apiserver.util.Constants.JSON_LON;
import static iudx.resource.server.apiserver.util.Constants.JSON_OPERATOR;
import static iudx.resource.server.apiserver.util.Constants.JSON_RADIUS;
import static iudx.resource.server.apiserver.util.Constants.JSON_VALUE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ATTRIBUTE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_COORDINATES;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ENDTIME;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOMETRY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOPROPERTY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOREL;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ID;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_Q;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIME;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIMEPROPERTY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIMEREL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.stream.Stream;

import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;



@ExtendWith({VertxExtension.class,MockitoExtension.class})
public class QueryMapperTest {

  @Mock
  RoutingContext context;
  private QueryMapper qm;

  static Stream<Arguments> invalidQTermsValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("refrenceLevel+10", "Operator not allowed."),
        Arguments.of("refrenceLevel/-10", "Operator not allowed."),
        Arguments.of("refrenceLevel<+10", "Operator not allowed."),
        Arguments.of("refrenceLevel>&10", "Operator not allowed."),
        Arguments.of("refrenceLevel>+10", "Operator not allowed."),
        Arguments.of("refrenceLevel+<10", "Operator not allowed."));

  }

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    qm = new QueryMapper(context,10);
    testContext.completeNow();
  }

  @Test
  public void testGetQueryTerms(Vertx vertx, VertxTestContext testContext) {
    String q = "speed>=300";
    JsonObject json = qm.getQueryTerms(q);
    assertEquals("speed", json.getString(JSON_ATTRIBUTE));
    assertEquals(">=", json.getString(JSON_OPERATOR));
    assertEquals("300", json.getString(JSON_VALUE));
    testContext.completeNow();

  }

  @Test
  public void testToJson(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(NGSILDQUERY_ID, "id1");
    map.add(NGSILDQUERY_ATTRIBUTE, "attr1");
    NgsildQueryParams params = new NgsildQueryParams(map);
    JsonObject json = qm.toJson(params, false);

    assertTrue(json.containsKey(NGSILDQUERY_ID));
    assertTrue(json.containsKey(NGSILDQUERY_ATTRIBUTE));
    assertTrue(json.getJsonArray(NGSILDQUERY_ID) instanceof JsonArray);
    assertTrue(json.getJsonArray(NGSILDQUERY_ATTRIBUTE) instanceof JsonArray);
    testContext.completeNow();
  }

  @Test
  @Description("QueryMapper test for Circle geo-query")
  public void testToJson4CircleQuery(Vertx vertx, VertxTestContext testContext) {
    // georel=near;maxDistance==360&geometry=Point&coordinates=%5B8.684783577919006%2C49.406131991436396%5D
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(NGSILDQUERY_ID, "id1");
    map.add(NGSILDQUERY_ATTRIBUTE, "attr1");
    map.add(NGSILDQUERY_GEOREL, "near;maxdistance=360");
    map.add(NGSILDQUERY_GEOMETRY, "point");
    map.add(NGSILDQUERY_COORDINATES, "[8.6846,49.40606]");
    map.add(NGSILDQUERY_GEOPROPERTY, "location");
    NgsildQueryParams params = new NgsildQueryParams(map);

    JsonObject json = qm.toJson(params, false);

    assertTrue(json.containsKey(JSON_LAT));
    assertTrue(json.containsKey(JSON_LON));
    assertTrue(json.containsKey(JSON_RADIUS));
    assertFalse(json.containsKey(NGSILDQUERY_TIMEREL));
    assertFalse(json.containsKey(NGSILDQUERY_TIME));
    assertFalse(json.containsKey(NGSILDQUERY_ENDTIME));
    testContext.completeNow();
  }

  @Test
  @Description("QueryMapper test for geo-query")
  public void testToJson4GeoQuery(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(NGSILDQUERY_ID, "id1,id2");
    map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
    map.add(NGSILDQUERY_GEOREL, "within");
    map.add(NGSILDQUERY_GEOMETRY, "Polygon");
    map.add(NGSILDQUERY_COORDINATES,
        "[[[8.68462,49.40606],[8.68550,49.40622],[8.68545,49.406344],[8.68457,49.40617],[8.6846,49.4060]]]");
    map.add(NGSILDQUERY_GEOPROPERTY, "location");
    NgsildQueryParams params = new NgsildQueryParams(map);

    JsonObject json = qm.toJson(params, false);

    assertTrue(json.containsKey(NGSILDQUERY_ID));
    assertTrue(json.containsKey(NGSILDQUERY_ATTRIBUTE));
    assertTrue(json.containsKey(NGSILDQUERY_COORDINATES));
    assertTrue(json.containsKey(NGSILDQUERY_GEOREL));
    assertTrue(json.containsKey(NGSILDQUERY_GEOMETRY));
    assertFalse(json.containsKey(NGSILDQUERY_TIMEREL));
    assertFalse(json.containsKey(NGSILDQUERY_TIME));
    assertFalse(json.containsKey(NGSILDQUERY_ENDTIME));
    testContext.completeNow();
  }
  
  @Test
  @Description("QueryMapper test for temporal queries(during)")
  public void testToJson4TemporalDuringQuery(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(NGSILDQUERY_ID, "id1,id2");
    map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
    map.add(NGSILDQUERY_TIMEREL, "during");
    map.add(NGSILDQUERY_TIME, "2020-01-23T14:20:00Z");
    map.add(NGSILDQUERY_ENDTIME, "2020-01-24T14:40:00Z");
    NgsildQueryParams params = new NgsildQueryParams(map);

    JsonObject json = qm.toJson(params, true);

    assertTrue(json.containsKey(NGSILDQUERY_ID));
    assertTrue(json.containsKey(NGSILDQUERY_ATTRIBUTE));
    assertTrue(json.containsKey(NGSILDQUERY_TIMEREL));
    assertTrue(json.containsKey(NGSILDQUERY_TIME));
    assertTrue(json.containsKey(NGSILDQUERY_ENDTIME));
    testContext.completeNow();
  }
  
  @Test
  @Description("QueryMapper test for temporal queries(before)")
  public void testToJson4TemporalQuery(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(NGSILDQUERY_ID, "id1,id2");
    map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
    map.add(NGSILDQUERY_TIMEREL, "before");
    map.add(NGSILDQUERY_TIME, "2020-01-23T14:20:00Z");
    map.add(NGSILDQUERY_TIMEPROPERTY, "observationTimeRel");
    NgsildQueryParams params = new NgsildQueryParams(map);

    JsonObject json = qm.toJson(params, true);
    assertTrue(json.containsKey(NGSILDQUERY_ID));
    assertTrue(json.containsKey(NGSILDQUERY_ATTRIBUTE));
    assertTrue(json.containsKey(NGSILDQUERY_TIMEREL));
    assertTrue(json.containsKey(NGSILDQUERY_TIME));
    testContext.completeNow();
  }

  @Test
  @Description("QueryMapper test for temporal queries(Invalid time format)")
  public void testToJson4TemporalInvalidTimeQuery(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(NGSILDQUERY_ID, "id1,id2");
    map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
    map.add(NGSILDQUERY_TIMEREL, "during");
    map.add(NGSILDQUERY_TIME, "2020-01-23X14:20:00Z");
    map.add(NGSILDQUERY_ENDTIME, "2020-01-24T14:40:00Z");
    map.add(NGSILDQUERY_TIMEPROPERTY, "observationTimeRel");
    NgsildQueryParams params = new NgsildQueryParams(map);

    qm.toJson(params, true);
    verify(context,times(1)).fail(anyInt(),any());
    testContext.completeNow();
  }

  @Test
  @Description("QueryMapper test for simple attribute query")
  public void testToJson4SimpleAttributeQuery(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(NGSILDQUERY_ID, "id1,id2");
    map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
    map.add(NGSILDQUERY_Q, "speed>=300;temperature==35");
    NgsildQueryParams params = new NgsildQueryParams(map);

    JsonObject json = qm.toJson(params, false);

    assertTrue(json.containsKey(NGSILDQUERY_ID));
    assertTrue(json.containsKey(NGSILDQUERY_ATTRIBUTE));
    assertTrue(json.containsKey(JSON_ATTR_QUERY));
    assertTrue(json.getJsonArray(JSON_ATTR_QUERY) instanceof JsonArray);
    assertEquals(json.getJsonArray(JSON_ATTR_QUERY).size(), 2);
    testContext.completeNow();
  }

  @ParameterizedTest
  @Disabled /* Request Validation Check done in QTypeValidator - Unit is not responsible for checking validity*/
  @MethodSource("invalidQTermsValues")
  @Description("coordinates type parameter invalid values.")
  public void testInvalidQTermValue(String value, String result, Vertx vertx,
      VertxTestContext testContext) {

    Exception exception = assertThrows(DxRuntimeException.class, () -> qm.getQueryTerms(value));
    String  expectedMessage = exception.getMessage();
    System.out.println(expectedMessage);
    testContext.completeNow();
  }

  @Test
  @Description("test temporal between without end time")
  public void testInvalidTemporalQuery(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(NGSILDQUERY_ID, "id1,id2");
    map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
    map.add(NGSILDQUERY_TIMEREL, "during");
    map.add(NGSILDQUERY_TIME, "2020-01-23T14:20:00Z");
    NgsildQueryParams params = new NgsildQueryParams(map);
      
    qm.toJson(params, true);
    verify(context,times(2)).fail(anyInt(),any());
    testContext.completeNow();
  }

  @Test
  @Description("test temporal interval for between")
  public void testInvalidTemporalIntervalQuery(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(NGSILDQUERY_ID, "id1,id2");
    map.add(NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
    map.add(NGSILDQUERY_TIMEREL, "during");
    map.add(NGSILDQUERY_TIME, "2020-01-13T14:20:00Z");
    map.add(NGSILDQUERY_ENDTIME, "2020-01-30T14:40:00Z");
    NgsildQueryParams params = new NgsildQueryParams(map);
    
    qm.toJson(params, true);
    verify(context,times(1)).fail(anyInt(),any());  
    testContext.completeNow();
  }

  @Test
  @Description("QueryMapper test for invalid geo-query")
  public void testIncompleteGeoQuery(Vertx vertx, VertxTestContext testContext) {
    // georel=near;maxDistance==360&geometry=Point&coordinates=%5B8.684783577919006%2C49.406131991436396%5D
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(NGSILDQUERY_ID, "id1");
    map.add(NGSILDQUERY_ATTRIBUTE, "attr1");
    map.add(NGSILDQUERY_GEOREL, "near;maxdistance=360");
    map.add(NGSILDQUERY_GEOMETRY, "point");
    map.add(NGSILDQUERY_COORDINATES, "[8.6846,49.40606]");

    NgsildQueryParams params = new NgsildQueryParams(map);
    qm.toJson(params, true);
    verify(context,times(1)).fail(anyInt(),any());
    testContext.completeNow();
  }

  @AfterEach
  public void teardown() {

  }

}
