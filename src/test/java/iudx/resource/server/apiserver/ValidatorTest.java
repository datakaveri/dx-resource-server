package iudx.resource.server.apiserver;


import static iudx.resource.server.apiserver.util.Constants.MSG_BAD_QUERY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ATTRIBUTE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOREL;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_Q;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import iudx.resource.server.common.CatalogueService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.configuration.Configuration;

@ExtendWith(VertxExtension.class)
public class ValidatorTest {
  private static JsonObject config;
  private static Configuration configuration;
  JsonObject json = new JsonObject(
      "{\"type\": \"Query\",\"entities\": [{\"id\": \"b58da193-23d9-43eb-b98a-a103d4b6103c\"}],\"geoQ\": {\"geometry\": \"Point\",\"coordinates\": [21.178,72.834],\"georel\": \"near;maxDistance=1000\",\"geoproperty\": \"location\"},\"temporalQ\": {\"timerel\": \"during\",\"time\": \"2020-10-18T14:20:00Z\",\"endtime\": \"2020-10-19T14:20:00Z\",\"timeProperty\": \"observationDateTime\"},\"q\":\"speed>30.0\",\"attrs\":\"id,speed\"}");
  private CatalogueService catalogueServiceMock;
  private ParamsValidator validator;
  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {

    configuration = new Configuration();
    config = configuration.configLoader(1, vertx);

    catalogueServiceMock = Mockito.mock(CatalogueService.class);
    validator = new ParamsValidator(catalogueServiceMock);

    assertNotNull(catalogueServiceMock);
    assertNotNull(validator);

    doAnswer(Answer -> Future
        .succeededFuture(new ArrayList<>(Arrays.asList("TEMPORAL", "SPATIAL", "ATTR"))))
            .when(catalogueServiceMock)
            .getApplicableFilters(any(String.class));

    testContext.completeNow();
  }

  @Test
  public void successCreatedJson(Vertx vertx, VertxTestContext testContext) {
    assertTrue(json.containsKey(NGSILDQUERY_TYPE));
    assertTrue(json.containsKey(NGSILDQUERY_ATTRIBUTE));
    assertTrue(json.containsKey(NGSILDQUERY_Q));
    assertTrue(json.containsKey("geoQ"));
    assertTrue(json.containsKey("temporalQ"));
    testContext.completeNow();
  }

  @Test
  public void validJsonTest(Vertx vertx, VertxTestContext testContext) {
    validator.validate(json).onComplete(handler -> {
      if (handler.succeeded() && handler.result()) {
        testContext.completeNow();
      } else {
        testContext.failNow(new Throwable("unexpected result"));
      }
    });
  }

  @Test
  public void extraParamTest(Vertx vertx, VertxTestContext testContext) {
    json.put("not_allowed_extra_param", "asdasda");
    validator.validate(json).onComplete(handler -> {
      if (handler.failed()) {
        assertEquals(MSG_BAD_QUERY, handler.cause().getMessage());
        testContext.completeNow();
      } else {
        testContext.failNow(new Throwable("unexpected result"));
      }
    });
  }

  @Test
  public void validWithoutAttrsJsonTest(Vertx vertx, VertxTestContext testContext) {
    json.remove(NGSILDQUERY_ATTRIBUTE);
    validator.validate(json).onComplete(handler -> {
      if (handler.succeeded() && handler.result()) {
        testContext.completeNow();
      } else {
        testContext.failNow(new Throwable("unexpected result"));
      }
    });
  }

  @Test
  public void validWithoutQJsonTest(Vertx vertx, VertxTestContext testContext) {
    json.remove(NGSILDQUERY_Q);
    validator.validate(json).onComplete(handler -> {
      if (handler.succeeded() && handler.result()) {
        testContext.completeNow();
      } else {
        testContext.failNow(new Throwable("unexpected result"));
      }
    });
  }

  @Test
  public void validWithoutGeoQJsonTest(Vertx vertx, VertxTestContext testContext) {
    json.remove("geoQ");
    validator.validate(json).onComplete(handler -> {
      if (handler.succeeded() && handler.result()) {
        testContext.completeNow();
      } else {
        testContext.failNow(new Throwable("unexpected result"));
      }
    });
  }


  static Stream<Arguments> invalidCoordinatesValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("point", "[21.1783156415464,72.834]"),
        Arguments.of("point", "[21.17,72.83467867874564]"),
        Arguments.of("point", "[21.178,72.834,23.5678]"),
        Arguments.of("point", "[21178345635353535353534521312,72.8342]"),
        Arguments.of("point", "[21.17834,7283425675567567567567567567567]"),
        Arguments.of("polygon", "[21.178,72.834,23.567]"),
        Arguments.of("polygon",
            "[[[73.8444,18.5307],[73.84357,18.52820],[73.8492,18.52836],[73.84632,18.52250],[73.83816,18.52934],[73.83576,18.52063],[73.84357,18.51631],[73.84992,18.51338],[73.85833,18.51672],[73.86065,18.52185],[73.861770,18.52567],[73.85928,18.52966],[73.85447,18.53414],[73.8444,18.53072]]]"),
        Arguments.of("polygon",
            "[[[73.8425,18.52791],[73.8411,18.5194],[73.8541,18.51880],[73.85892,18.5275],[73.84250,18.5279235684]]]"));
  }

  @ParameterizedTest
  @MethodSource("invalidCoordinatesValues")
  @Description("coordinates type parameter invalid values.")
  public void testInvalidCoordinatesTypeValue(String geom, String coords, Vertx vertx,
      VertxTestContext testContext) {

    json.getJsonObject("geoQ").put("geometry", geom);
    json.getJsonObject("geoQ").put("coordinates", coords);
    assertThrows(DxRuntimeException.class, () -> validator.validate(json));
    testContext.completeNow();
  }


  static Stream<Arguments> invalidAttrsValues() {
    // Add any valid value which will pass successfully.
    return Stream.of(
        Arguments.of("refrenceLeval,Co2,NO2,SO2,CO,ABC", "More than 5 attributes are not allowed."),
        Arguments.of(RandomStringUtils.random(102) + ",refrenceLeval,Co2,NO2,SO2",
            "One of the attribute exceeds allowed characters(only 100 characters allowed)."),
        Arguments.of("refrence$Leval,Co2,NO2,SO2", "Invalid attribute value."));
  }


  @ParameterizedTest
  @MethodSource("invalidAttrsValues")
  @Description("attrs type parameter invalid values.")
  public void testInvalidAttrsTypeValue(String value, String result, Vertx vertx,
      VertxTestContext testContext) {
    json.put(NGSILDQUERY_ATTRIBUTE, value);
    assertThrows(DxRuntimeException.class, () -> validator.validate(json));
    testContext.completeNow();
  }


  static Stream<Arguments> invalidQValues() {
    // Add any invalid value which will throw error.
    return Stream.of(
        Arguments.of(RandomStringUtils.random(600)),
        Arguments.of("referenceLevel<>15.0"),
        Arguments.of("referenceLevel>>15.0"),
        Arguments.of("referenceLevel===15.0"),
        Arguments.of("referenceLevel+15.0"),
        Arguments.of("referenceLevel/15.0"),
        Arguments.of("referenceLevel*15.0"),
        Arguments.of("reference_Level$>15.0"),
        Arguments.of("reference$Level>15.0"));
  }


  @ParameterizedTest
  @MethodSource("invalidQValues")
  @Description("q type parameter invalid values.")
  public void testInvalidQTypeValue(String value, Vertx vertx, VertxTestContext testContext) {
    json.put(NGSILDQUERY_Q, value);
    assertThrows(DxRuntimeException.class, () -> validator.validate(json));
    testContext.completeNow();
  }


  static Stream<Arguments> invalidGeoRelValues() {
    // Add any invalid value which will throw error.
    return Stream.of(
        Arguments.of(RandomStringUtils.random(600)),
        Arguments.of("near;maxDistance=-1"),
        Arguments.of("near;maxDistance=0"),
        Arguments.of("near;maxDistance=10001"),
        Arguments.of("near;maxDistance"),
        Arguments.of("near;maxDistance=235689563213215413"));
  }

  @ParameterizedTest
  @MethodSource("invalidGeoRelValues")
  @Description("geoRel type parameter invalid values.")
  public void testInvalidGeoRelTypeValue(String value, Vertx vertx, VertxTestContext testContext) {
    json.getJsonObject("geoQ").put(NGSILDQUERY_GEOREL, value);
    assertThrows(DxRuntimeException.class, () -> validator.validate(json));
    testContext.completeNow();
  }

  static Stream<Arguments> validGeoRelValues() {
    // Add any invalid value which will throw error.
    return Stream.of(
        Arguments.of("within"),
        Arguments.of("intersects"),
        Arguments.of("near;maxDistance=500"));
  }


  @ParameterizedTest
  @MethodSource("validGeoRelValues")
  @Description("geoRel type parameter valid values.")
  public void testValidGeoRelTypeValue(String value, Vertx vertx, VertxTestContext testContext) {
    json.getJsonObject("geoQ").put(NGSILDQUERY_GEOREL, value);
    validator.validate(json).onComplete(handler -> {
      if (handler.succeeded()) {
        assertTrue(handler.result());
        testContext.completeNow();
      } else {
        testContext.failNow(new Throwable("Unexpected result"));
      }
    });
  }

}

