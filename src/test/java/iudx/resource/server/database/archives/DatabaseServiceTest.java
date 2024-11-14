package iudx.resource.server.database.archives;

import static iudx.resource.server.database.archives.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.vertx.core.Future;
import iudx.resource.server.cache.CacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.configuration.Configuration;
import iudx.resource.server.database.elastic.ElasticClient;
@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class DatabaseServiceTest {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceTest.class);
  private static DatabaseService dbService;
  static DatabaseServiceImpl dbImpl;
  static DatabaseServiceImpl dbSpy;
  private static Vertx vertxObj;
  private static ElasticClient client;
  private static String databaseIP, user, password;
  private static int databasePort;
  private static Configuration config;
  private static String timeLimit;

  /** Values for test */
  private static String idOpen;
  private static String idClose;
  private static String temporalStartDate;
  private static String temporalEndDate;
  private static String tenantPrefix;
  private static DatabaseServiceImpl databaseServiceImpl;
  @Mock
  private static ElasticClient elasticClient;
  @Mock
  private static AsyncResult<JsonObject> asyncResult;
  @Mock
  private static Throwable throwable;
  @Mock
  static CacheService cacheService;


  /* TODO Need to update params to use contants */
  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    vertxObj = vertx;
    config = new Configuration();
    JsonObject dbConfig = config.configLoader(0, vertx);

    /* Read the configuration and set the rabbitMQ server properties. */
    /* Configuration setup */
    databaseIP = dbConfig.getString("databaseIP");
    databasePort = dbConfig.getInteger("databasePort");
    user = dbConfig.getString("dbUser");
    password = dbConfig.getString("dbPassword");
    timeLimit = dbConfig.getString("timeLimit");
    tenantPrefix = dbConfig.getString("tenantPrefix");

    idOpen = dbConfig.getString("testIdOpen");
    idClose = dbConfig.getString("testIdSecure");
    temporalStartDate = dbConfig.getString("temporalStartDate");
    temporalEndDate = dbConfig.getString("temporalEndDate");

    client = new ElasticClient(databaseIP, databasePort, user, password);
    dbService = new DatabaseServiceImpl(client, timeLimit, tenantPrefix,cacheService);
    dbImpl = new DatabaseServiceImpl(client, timeLimit, tenantPrefix, cacheService);
    dbSpy = Mockito.spy(dbImpl);
    testContext.completeNow();

  }

  @BeforeEach
  public void intialize(VertxTestContext vertxTestContext) {
    databaseServiceImpl = new DatabaseServiceImpl(elasticClient, timeLimit, tenantPrefix,cacheService);
    vertxTestContext.completeNow();
  }

  @AfterEach
  public void finish(VertxTestContext testContext) {
    LOGGER.info("Finishing....");
    vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  }

  @Test
  @DisplayName("Testing Geo-circle query")
  void searchGeoCircle(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "geoSearch_")
          .put("lon", 72.8296)
          .put("lat", 21.2)
          .put("radius", 1000)
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());
    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));

    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));
    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Basic Exceptions (No resource-id key)")
  void searchWithNoResourceId(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("searchType", "geoSearch_");
    
    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse = new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing Basic Exceptions (No resource-id key) in count query")
  void countWithNoResourceId(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("searchType", "geoSearch_");

    // dbService.countQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("No id found", new JsonObject(response.getMessage()).getString(
    // "detail"));
    // testContext.completeNow();
    // })));

    dbService.count(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse = new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Testing Basic Exceptions (resource-id is empty)")
  void searchEmptyResourceId(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject().put("id", new JsonArray()).put("searchType", "geoSearch_");

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("resource-id is empty", new JsonObject(response.getMessage()).getString(
    // "detail"));
    // testContext.completeNow();
    // })));

    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse=new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing Basic Exceptions (resource-id is empty) in count query")
  void countEmptyResourceId(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject().put("id", new JsonArray()).put("searchType", "geoSearch_");

    // dbService.countQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("resource-id is empty", new JsonObject(response.getMessage()).getString(
    // "detail"));
    // testContext.completeNow();
    // })));

    dbService.count(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse=new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();

    });
  }

  @Test
  @DisplayName("Testing Basic Exceptions (No searchType key)")
  void searchWithSearchType(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"));

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("No searchType found", new JsonObject(response.getMessage()).getString(
    // "detail"));
    // testContext.completeNow();
    // })));

    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse=new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
      
    });
  }

  @Test
  @DisplayName("Testing Basic Exceptions (No searchType key) in count query")
  void countWithSearchType(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"));

    // dbService.countQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("No searchType found", new JsonObject(response.getMessage()).getString(
    // "detail"));
    // testContext.completeNow();
    // })));

    dbService.count(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse=new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
      
    });

  }

  @Test
  @DisplayName("Testing Geo Exceptions (Missing necessary parameters [lat,lon,radius])")
  void searchMissingGeoParamsCircle(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
          .put("searchType", "geoSearch_");
    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("Missing/Invalid geo parameters", new JsonObject(response.getMessage())
    // .getString("detail"));
    // testContext.completeNow();
    // })));


//    assertThrows(ESQueryDecodeException.class, ()->dbService.search(request));
//    testContext.completeNow();
    
    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      LOGGER.info("handler" +handler.getMessage());
      //JsonObject exceptionResponse=new JsonObject(handler.getMessage());
      //assertEquals("Missing/Invalid geo parameters", exceptionResponse.getString("detail"));
      testContext.completeNow();
      
    });
    
  }

  @Test
  @DisplayName("Testing Geo-Polygon query")
  void searchGeoPolygon(VertxTestContext testContext) {
    /**
     * coordinates should look like this
     * [[[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5],[lo1,la1]]]
     */
    String id =
        "b58da193-23d9-43eb-b98a-a103d4b6103c";

    JsonObject request = new JsonObject()
        .put("id", new JsonArray().add(id))
          .put("geometry", "polygon")
          .put("georel", "within")
          .put("coordinates",
              "[[[72.76,21.15],[72.76,21.13],[72.78,21.13],[72.78,21.15],[72.76,21.15]]]")
          .put("geoproperty", "location")
          .put("searchType", "geoSearch_")
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertTrue(response.getJsonArray("results").getJsonObject(0).getString("id").equals(id));
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));
    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      assertNotNull(handler.getJsonArray("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Geo Exceptions (Missing necessary parameters [coordinates, geoproperty, georel)")
  void searchMissingGeoParamsGeometry(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
          .put("searchType", "geoSearch_")
          .put("geometry", "polygon");

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("Missing/Invalid geo parameters", new JsonObject(response.getMessage())
    // .getString("detail"));
    // testContext.completeNow();
    // })));

    // assertThrows(ESQueryDecodeException.class, ()->dbService.search(request));
    // testContext.completeNow();
    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse = new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
    });


  }

  @Test
  @DisplayName("Testing Geo Polygon Exceptions (First and Last coordinates don't match)")
  void searchPolygonFirstLastNoMatch(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
          .put("geometry", "polygon")
          .put("georel", "within")
          .put("coordinates",
              "[[[82.9735,25.3703],[83.0053,25.3567],[82.9766,25.3372],[82.95,25.3519],"
                  + "[82.936,25.3722]]]")
          .put("geoproperty", "geoJsonLocation")
          .put("searchType", "geoSearch_");

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("Coordinate mismatch (Polygon)", new JsonObject(response.getMessage())
    // .getString("detail"));
    // testContext.completeNow();
    // })));

    // assertThrows(ESQueryDecodeException.class, ()->dbService.search(request));
    // testContext.completeNow();

    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse = new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();

    });

  }

  @Test
  @DisplayName("Testing Geo-LineString query")
  void searchGeoLineString(VertxTestContext testContext) {
    /**
     * coordinates should look like this [[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5]]
     */

    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("geometry", "linestring")
          .put("georel", "intersects")
          .put("coordinates", "[[72.84,21.19],[72.84,21.17]]")
          .put("geoproperty", "location")
          .put("searchType", "geoSearch_")
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertTrue(72.5 <= response.getJsonArray("results").getJsonObject(0)
    // .getJsonObject("location").getJsonArray("coordinates").getDouble(0)
    // && response.getJsonArray("results").getJsonObject(0)
    // .getJsonObject("location").getJsonArray("coordinates").getDouble(0) <= 72.9);
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));
    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      assertEquals(handler,esJson);
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });

  }

  @Test
  @DisplayName("Testing Geo-BBOX query")
  void searchGeoBbox(VertxTestContext testContext) {
    /**
     * coordinates should look like this [[lo1,la1],[lo3,la3]]
     */

    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("geometry", "bbox")
          .put("georel", "within")
          .put("coordinates", "[[72.8296,21.2],[72.8297,21.15]]")
          .put("geoproperty", "location")
          .put("searchType", "geoSearch_")
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertTrue(72.8 <= response.getJsonArray("results").getJsonObject(0)
    // .getJsonObject("location").getJsonArray("coordinates").getDouble(0)
    // && response.getJsonArray("results").getJsonObject(0)
    // .getJsonObject("location").getJsonArray("coordinates").getDouble(0) <= 73);
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));
    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      assertNotNull(handler.getString("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });

  }

  @Test
  @DisplayName("Testing Geo-BBOX Exceptions [empty response]")
  void searchBboxEmptyResponse(VertxTestContext testContext) {
    /**
     * coordinates should look like this [[lo1,la1],[lo3,la3]]
     */
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("geometry", "bbox")
          .put("georel", "within")
          .put("coordinates", "[[82,25.33],[82.01,25.317]]")
          .put("geoproperty", "geoJsonLocation")
          .put("searchType", "geoSearch_")
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("Empty response", new JsonObject(response.getMessage())
    // .getString("detail"));
    // testContext.completeNow();
    // })));

    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
      
    }).onFailure(handler -> {
//      assertEquals("Empty response", handler.getString("detail"));
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Testing Response Filter")
  void searchResponseFilter(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "responseFilter_geoSearch_")
          .put("geometry", "bbox")
          .put("georel", "within")
          .put("coordinates", "[[72.8296,21.2],[72.8297,21.15]]")
          .put("geoproperty", "geoJsonLocation")
          .put("attrs", new JsonArray().add("id").add("license_plate").add("speed"))
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    Set<String> attrs = new HashSet<>();
    attrs.add("id");
    attrs.add("license_plate");
    attrs.add("speed");
    // dbService.searchQuery(request, testContext.succeeding(response -> {
    // Set<String> resAttrs = new HashSet<>();
    // for (Object obj : response.getJsonArray("results")) {
    // JsonObject jsonObj = (JsonObject) obj;
    // if (resAttrs != attrs) {
    // resAttrs = jsonObj.fieldNames();
    // }
    // }
    // Set<String> finalResAttrs = resAttrs;
    // testContext.verify(() -> {
    // assertEquals(attrs, finalResAttrs);
    // testContext.completeNow();
    // });
    // }));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {

      assertEquals(handler, esJson);
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });

  }

  @Test
  @DisplayName("Testing Response Filter Exceptions (Missing parameters [attrs]")
  void searchMissingResponseFilterParams(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
          .put("searchType", "responseFilter_");

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("Missing/Invalid responseFilter parameters", new JsonObject(response
    // .getMessage()).getString("detail"));
    // testContext.completeNow();
    // })));

    // assertThrows(ESQueryDecodeException.class, () -> dbService.search(request));
    // testContext.completeNow();

    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse = new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query",exceptionResponse.getString("detail"));
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing Complex (Geo + Response Filter) Search")
  void searchComplexGeoResponse(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "responseFilter_geoSearch_")
          .put("attrs", new JsonArray().add("id").add("location").add("speed"))
          .put("lon", 72.8296)
          .put("lat", 21.2)
          .put("radius", 1000)
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    Set<String> attrs = new HashSet<>();
    attrs.add("id");
    attrs.add("location");
    attrs.add("speed");
    // dbService.searchQuery(request, testContext.succeeding(response -> {
    // Set<String> resAttrs = new HashSet<>();
    // for (Object obj : response.getJsonArray("results")) {
    // JsonObject jsonObj = (JsonObject) obj;
    // if (resAttrs != attrs) {
    // resAttrs = jsonObj.fieldNames();
    // }
    // }
    // Set<String> finalResAttrs = resAttrs;
    // testContext.verify(() -> {
    // assertTrue(72.8 < response.getJsonArray("results").getJsonObject(0)
    // .getJsonObject("location").getJsonArray("coordinates").getDouble(0));
    // assertEquals(attrs, finalResAttrs);
    // testContext.completeNow();
    // });
    // }));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));
    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      assertEquals(esJson, handler);
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });

  }

  @Test
  @DisplayName("Testing Count Geo-Circle query")
  void countGeoCircle(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "geoSearch_")
          .put("lon", 72.834)
          .put("lat", 21.178)
          .put("radius", 10)
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    // dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertTrue(response.getJsonArray("results").getJsonObject(0).containsKey("totalHits"));
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.count(request).onSuccess(handler -> {
      assertTrue(handler.getJsonArray("results").getJsonObject(0).containsKey("totalHits"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });


  }

  @Test
  @DisplayName("Testing Temporal Queries (During)")
  void searchDuringTemporal(VertxTestContext testContext) throws ParseException {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray().add(idOpen))
          .put("searchType", "temporalSearch_")
          .put("timerel", "during")
          .put("time", temporalStartDate)
          .put("endtime", temporalEndDate)
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalStartDate, dateTimeFormatter);
    OffsetDateTime end = OffsetDateTime.parse(temporalEndDate, dateTimeFormatter);
    LOGGER.debug("### start date: " + start);

    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // OffsetDateTime resDate = OffsetDateTime.parse(response.getJsonArray("results")
    // .getJsonObject(5).getString("observationDateTime"), dateTimeFormatter);
    // OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
    // LOGGER.debug("#### response Date " + resDateUtc);
    // assertTrue(!(resDateUtc.isBefore(start) || resDateUtc.isAfter(end)));
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      assertNotNull(handler.getString("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Temporal Queries (Before)")
  void searchBeforeTemporal(VertxTestContext testContext) throws ParseException {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray().add(idOpen))
          .put("searchType", "temporalSearch_")
          .put("timerel", "before")
          .put("time", temporalEndDate)
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalEndDate, dateTimeFormatter);
    LOGGER.debug("### start date: " + start);

    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // OffsetDateTime resDate = OffsetDateTime.parse(response.getJsonArray("results")
    // .getJsonObject(6).getString("observationDateTime"), dateTimeFormatter);
    // OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
    // LOGGER.debug("#### response Date " + resDateUtc);
    // assertTrue(resDateUtc.isBefore(start));
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      assertNotNull(handler.getString("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Temporal Queries (After)")
  void searchAfterTemporal(VertxTestContext testContext) throws ParseException {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray().add(idOpen))
          .put("searchType", "temporalSearch_")
          .put("timerel", "after")
          .put("time", temporalStartDate)
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));;

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalStartDate, dateTimeFormatter);
    LOGGER.debug("### start date: " + start);

    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // OffsetDateTime resDate =
    // OffsetDateTime.parse(response.getJsonArray("results").getJsonObject(3)
    // .getString("observationDateTime"), dateTimeFormatter);
    // OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
    // LOGGER.debug("#### response Date " + resDateUtc);
    // assertTrue(resDateUtc.isAfter(start) || resDateUtc.isEqual(start));
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      assertNotNull(handler.getString("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Count Geo-Polygon query")
  void countGeoPolygon(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("geometry", "polygon")
          .put("georel", "within")
          .put("coordinates",
              "[[[72.719,21],[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20],[72.719,21]]]")
          .put("geoproperty", "location")
          .put("searchType", "geoSearch_")
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    // dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertTrue(response.getJsonArray("results").getJsonObject(0).containsKey("totalHits"));
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.count(request).onSuccess(handler -> {
      assertTrue(handler.getJsonArray("results").getJsonObject(0).containsKey("totalHits"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });

  }

  /**
   * @Test @DisplayName("Testing Temporal Queries (TEquals)") @Disabled("no tequals supported by
   *       IUDX") void searchTequalsTemporal(VertxTestContext testContext) throws ParseException {
   *       JsonObject request = new JsonObject() .put("id", new JsonArray().add(idOpen))
   *       .put("searchType", "temporalSearch_").put("timerel", "before") .put("time",
   *       temporalStartDate);
   * 
   *       DateTimeFormatter dateTimeFormatter =
   *       DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX"); OffsetDateTime start =
   *       OffsetDateTime.parse(temporalStartDate, dateTimeFormatter); LOGGER.debug("### start date:
   *       " + start);
   * 
   *       dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(()
   *       -> { OffsetDateTime resDate =
   *       OffsetDateTime.parse(response.getJsonArray("results").getJsonObject(0)
   *       .getString("observationDateTime"), dateTimeFormatter); OffsetDateTime resDateUtc =
   *       resDate.withOffsetSameInstant(ZoneOffset.UTC); LOGGER.debug("#### response Date " +
   *       resDateUtc); assertTrue(resDateUtc.isEqual(start)); testContext.completeNow(); }))); }
   **/
  @Test
  @DisplayName("Testing Count Geo-Linestring query")
  void countGeoLineString(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("geometry", "linestring")
          .put("georel", "intersects")
          .put("coordinates", "[[72.833994,21.17798],[72.833978,21.178005]]")
          .put("geoproperty", "location")
          .put("searchType", "geoSearch_")
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    // dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertTrue(response.getJsonArray("results").getJsonObject(0).containsKey("totalHits"));
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.count(request).onSuccess(handler -> {
      assertTrue(handler.getJsonArray("results").getJsonObject(0).containsKey("totalHits"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });

  }

  @Test
  @DisplayName("Testing Temporal Queries (Before) with IST date format")
  void searchBeforeTemporalIST(VertxTestContext testContext) throws ParseException {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "temporalSearch_")
          .put("timerel", "before")
          .put("time", "2020-10-29T10:00:00+05:30")
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    ZonedDateTime start = ZonedDateTime.parse("2020-10-29T10:00:00+05:30");
    LOGGER.debug("### start date: " + start);

    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // ZonedDateTime resDate = ZonedDateTime.parse(response.getJsonArray("results")
    // .getJsonObject(6).getString("observationDateTime"));
    // LOGGER.debug("#### response Date " + resDate);
    // assertTrue(resDate.isBefore(start));
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      assertEquals(handler,esJson);
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Temporal Queries (Before) with IST date format with limit exceed response")
  void searchBeforeTemporalISTLimitExceed(VertxTestContext testContext) throws ParseException {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "temporalSearch_")
          .put("timerel", "before")
          .put("time", "2020-09-29T10:00:00+05:30")
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));;

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // JsonObject json=new JsonObject(response.getMessage());
    // assertTrue(json.getString("title").equals("urn:dx:rs:payloadTooLarge"));
    // testContext.completeNow();
    // })));

    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
//      assertTrue(handler.getString("title").equals("urn:dx:rs:payloadTooLarge"));
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing Temporal Exceptions (invalid date)")
  void searchTemporalInvalidDate(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "temporalSearch_")
          .put("timerel", "after")
          .put("time", "Invalid date");

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("exception while parsing date/time", new JsonObject(response.getMessage())
    // .getString("detail"));
    // testContext.completeNow();
    // })));

//    assertThrows(ESQueryDecodeException.class, ()->dbService.search(request));
//    testContext.completeNow();
    
    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse = new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing Temporal Exceptions (invalid end date)")
  void searchTemporalInvalidEndDate(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "temporalSearch_")
          .put("timerel", "during")
          .put("time", "2020-09-18T14:20:00Z")
          .put("endtime", "Invalid date");

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("exception while parsing date/time", new JsonObject(response.getMessage())
    // .getString("detail"));
    // testContext.completeNow();
    // })));
    
//    assertThrows(ESQueryDecodeException.class, ()->dbService.search(request));
//    testContext.completeNow();
    
    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse = new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Testing Temporal Exceptions (invalid timeRel date)")
  void searchTemporalInvalidTimerel(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "temporalSearch_")
          .put("timerel", "adadas")
          .put("time", "2020-09-18T14:20:00Z")
          .put("endtime", "2020-09-19T14:20:00Z");

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("exception while parsing date/time", new JsonObject(response.getMessage())
    // .getString("detail"));
    // testContext.completeNow();
    // })));

//    assertThrows(ESQueryDecodeException.class, ()->dbService.search(request));
//    testContext.completeNow();
    
    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse = new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing Temporal Exceptions (end date before start date)")
  void searchTemporalInvalidTimeInterval(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "temporalSearch_")
          .put("timerel", "during")
          .put("time", "2020-09-28T14:20:00Z")
          .put("endtime", "2020-09-19T14:20:00Z");

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("end date is before start date", new JsonObject(response.getMessage())
    // .getString("detail"));
    // testContext.completeNow();
    // })));

//    assertThrows(ESQueryDecodeException.class, ()->dbService.search(request));
//    testContext.completeNow();
    
    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse = new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing Count Geo-Bbox query")
  void countGeoBbox(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("geometry", "bbox")
          .put("georel", "within")
          .put("coordinates", "[[72.8296,21.2],[72.8297,21.15]]")
          .put("geoproperty", "location")
          .put("searchType", "geoSearch_")
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    // dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertTrue(response.getJsonArray("results").getJsonObject(0).containsKey("totalHits"));
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.count(request).onSuccess(handler -> {
      assertTrue(handler.getJsonArray("results").getJsonObject(0).containsKey("totalHits"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Complex Queries (Geo + Temporal + Response Filter)")
  void searchComplexPart2(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray().add(idClose))
          .put("searchType", "temporalSearch_geoSearch_responseFilter_")
          .put("timerel", "before")
          .put("time", temporalStartDate)
          .put("lon", 72.8296)
          .put("lat", 21.2)
          .put("radius", 500)
          .put("attrs", new JsonArray().add("id").add("location").add("observationDateTime"))
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    Set<String> attrs = new HashSet<>();
    attrs.add("id");
    attrs.add("observationDateTime");
    attrs.add("location");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalStartDate, dateTimeFormatter);

    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // OffsetDateTime resDate =
    // OffsetDateTime.parse(response.getJsonArray("results").getJsonObject(3)
    // .getString("observationDateTime"), dateTimeFormatter);
    // OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
    // // assertEquals(72.833759, response.getJsonArray("results").getJsonObject(0).getJsonObject(
    // // "location").getJsonArray("coordinates").getDouble(0));
    // assertTrue(resDateUtc.isBefore(start));
    // assertEquals(attrs, response.getJsonArray("results").getJsonObject(2).fieldNames());
    // testContext.completeNow();
    // })));

    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));
    dbSpy.search(request).onSuccess(handler -> {
      assertEquals(esJson, handler);
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing response filter with count")
  void countResponseFilter(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
          .put("searchType", "responseFilter_")
          .put("attrs", new JsonArray().add("id").add("latitude").add("longitude"));

    // dbService.countQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("Count is not supported with filtering", new JsonObject(response
    // .getMessage()).getString("detail"));
    // testContext.completeNow();
    // })));

    dbService.count(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse = new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
    });
  }

  /**
   * @Test @DisplayName("Testing Partial Complex Queries (Geo + Temporal + invalid-Response
   *       Filter)")
   * @Disabled void searchPartialComplex(VertxTestContext testContext) { JsonObject request = new
   *           JsonObject() .put("id", new JsonArray().add(idClose)) .put("searchType",
   *           "temporalSearch_geoSearch_response@KSf_") .put("timerel", "before") .put("time",
   *           temporalEndDate) .put("lon", 21.2) .put("lat", 72.8296) .put("radius", 500)
   *           .put("attrs", new JsonArray() .add("id") .add("longitude")
   *           .add("observationDateTime")); DateTimeFormatter dateTimeFormatter =
   *           DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX"); OffsetDateTime start =
   *           OffsetDateTime.parse(temporalEndDate, dateTimeFormatter);
   * 
   *           dbService.searchQuery(request, testContext.succeeding(response ->
   *           testContext.verify(() -> { OffsetDateTime resDate =
   *           OffsetDateTime.parse(response.getJsonArray("results").getJsonObject(3)
   *           .getString("observationDateTime"), dateTimeFormatter); OffsetDateTime resDateUtc =
   *           resDate.withOffsetSameInstant(ZoneOffset.UTC); // assertEquals(72.833759,
   *           response.getJsonArray("results").getJsonObject(0).getJsonObject( //
   *           "location").getJsonArray("coordinates").getDouble(0));
   *           assertTrue(resDateUtc.isBefore(start) || resDateUtc.isEqual(start));
   *           testContext.completeNow(); }))); }
   **/

  @Test
  @DisplayName("Testing invalid Search request")
  void searchInvalidType(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
          .put("searchType", "response!@$_geoS241");

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("Invalid search request",
    // new JsonObject(response.getMessage()).getString("detail"));
    // testContext.completeNow();
    // })));

    
//    assertThrows(ESQueryDecodeException.class, ()->dbService.search(request));
//    testContext.completeNow();
    
    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse=new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
      
    });
    
  }

  @Test
  @DisplayName("Testing Attribute Search")
  void searchAttributeGt(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "attributeSearch_")
          .put("attr-query",
              new JsonArray()
                  .add(new JsonObject()
                      .put("attribute", "referenceLevel")
                        .put("operator", ">")
                        .put("value", "2")))
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));
    dbSpy.search(request).onSuccess(handler -> {
      assertNotNull(handler.getJsonArray("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Attribute Search ")
  void searchAttributeLt(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "attributeSearch_")
          .put("attr-query",
              new JsonArray()
                  .add(new JsonObject()
                      .put("attribute", "referenceLevel")
                        .put("operator", "<")
                        .put("value", "5")))
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertTrue(response.getJsonArray("results").getJsonObject(2).getDouble("referenceLevel") <
    // 5);
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));
    dbSpy.search(request).onSuccess(handler -> {
      assertNotNull(handler.getJsonArray("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Attribute Search (property is greater than equal)")
  void searchAttributeGte(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "attributeSearch_")
          .put("attr-query",
              new JsonArray()
                  .add(new JsonObject()
                      .put("attribute", "referenceLevel")
                        .put("operator", ">=")
                        .put("value", "3")))
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertTrue(
    // response.getJsonArray("results").getJsonObject(3).getDouble("referenceLevel") >= 3);
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));

    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      assertNotNull(handler.getJsonArray("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Attribute Search (property is lesser than equal)")
  void searchAttributeLte(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "attributeSearch_")
          .put("attr-query",
              new JsonArray()
                  .add(new JsonObject()
                      .put("attribute", "referenceLevel")
                        .put("operator", "<=")
                        .put("value", "5")))
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertTrue(
    // response.getJsonArray("results").getJsonObject(7).getDouble("referenceLevel") <= 5);
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));
    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));


    dbSpy.search(request).onSuccess(handler -> {
      assertNotNull(handler.getJsonArray("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Attribute Search (property is between)")
  void searchAttributeBetween(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "attributeSearch_")
          .put("attr-query",
              new JsonArray()
                  .add(new JsonObject()
                      .put("attribute", "referenceLevel")
                        .put("operator", "<==>")
                        .put("valueLower", "3")
                        .put("valueUpper", "5")))
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertTrue(response.getJsonArray("results").getJsonObject(9).getDouble("referenceLevel") > 3
    // && response.getJsonArray("results").getJsonObject(9).getDouble("referenceLevel") < 5);
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));
    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      assertNotNull(handler.getJsonArray("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  /*
   * TODO: Need to understand operator parameter of JsonObject from the APIServer would look like.
   */

  // @Test
  // @DisplayName("Testing Attribute Search (property is like)")
  // void searchAttributeLike(VertxTestContext testContext){
  // JsonObject request = new JsonObject().put("id", new JsonArray()
  // .add("rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01")).put("searchType",
  // "attributeSearch_").put("attr-query", new JsonArray().add(new JsonObject().put(
  // "attribute", "CO2").put("operator", "like").put("value", "500")));
  // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(()->{
  // assertTrue(response.getJsonObject(4).getDouble("CO2") > 500);
  // testContext.completeNow();
  // })));
  // }

  @Test
  @DisplayName("Testing Attribute Search (property is equal)")
  void searchAttributeEqual(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "attributeSearch_")
          .put("attr-query",
              new JsonArray()
                  .add(new JsonObject()
                      .put("attribute", "referenceLevel")
                        .put("operator", "==")
                        .put("value", "4.2")))
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertEquals(response.getJsonArray("results").getJsonObject(0)
    // .getDouble("referenceLevel"), 4.2);
    // testContext.completeNow();
    // })));

    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));
    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      assertNotNull(handler.getJsonArray("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Attribute Search (property is not equal)")
  void searchAttributeNe(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "b58da193-23d9-43eb-b98a-a103d4b6103c"))
          .put("searchType", "attributeSearch_")
          .put("attr-query",
              new JsonArray()
                  .add(new JsonObject()
                      .put("attribute", "referenceLevel")
                        .put("operator", "!=")
                        .put("value", "5")))
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
    // assertTrue(
    // response.getJsonArray("results").getJsonObject(4).getDouble("referenceLevel") != 5);
    // testContext.completeNow();
    // })));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",1)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));
    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbSpy.search(request).onSuccess(handler -> {
      assertNotNull(handler.getJsonArray("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Attribute Search (invalid property operator)")
  void searchInvalidAttributeOP(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "83c2e5c2-3574-4e11-9530-2b1fbdfce832"))
          .put("searchType", "attributeSearch_")
          .put("attr-query",
              new JsonArray()
                  .add(new JsonObject()
                      .put("attribute", "referenceLevel")
                        .put("operator", "asasd")
                        .put("value", "5")))
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("invalid attribute operator", new
    // JsonObject(response.getMessage()).getString("detail"));
    // testContext.completeNow();
    // })));

    
    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse=new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      testContext.completeNow();
      
    });
    
  }

  /**
   * @Test @DisplayName("Testing Latest Search")
   * @Disabled void latestSearch(VertxTestContext testContext) { JsonObject request = new
   *           JsonObject().put("id", new JsonArray()
   *           .add("datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR018"))
   *           .put("searchType", "latestSearch"); JsonArray id = request.getJsonArray("id");
   *           JsonArray idFromResponse = new JsonArray(); dbService.searchQuery(request,
   *           testContext.succeeding(response -> testContext.verify(() -> { for (Object o :
   *           response.getJsonArray("results")) { JsonObject doc = (JsonObject) o;
   *           idFromResponse.add(doc.getString("id")); } assertEquals(id, idFromResponse);
   *           testContext.completeNow(); }))); }
   **/
  /**
   * @Test @DisplayName("Testing Latest Search with Response Filter")
   * @Disabled void latestSearchFiltered(VertxTestContext testContext) { JsonObject request = new
   *           JsonObject().put("id", new JsonArray()
   *           .add("datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR018"))
   *           .put("searchType", "latestSearch") .put("attrs", new JsonArray().add("id")
   *           .add("observationDateTime")); Set<String> attrs = new HashSet<>(); attrs.add("id");
   *           attrs.add("observationDateTime"); JsonArray id = request.getJsonArray("id");
   *           JsonArray idFromResponse = new JsonArray(); dbService.searchQuery(request,
   *           testContext.succeeding(response -> { Set<String> resAttrs = new HashSet<>(); for
   *           (Object obj : response.getJsonArray("results")) { JsonObject jsonObj = (JsonObject)
   *           obj; idFromResponse.add(jsonObj.getString("id")); if (resAttrs != attrs) { resAttrs =
   *           jsonObj.fieldNames(); } } Set<String> finalResAttrs = resAttrs; testContext.verify(()
   *           -> { assertEquals(id, idFromResponse); assertEquals(attrs, finalResAttrs);
   *           testContext.completeNow(); }); })); }
   **/
  @Test
  @DisplayName("Testing auto index selection with malformed id")
  void malformedResourceId(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray().add("malformed-id"))
          .put("searchType", "latestSearch");
    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("Malformed Id [\"malformed-id\"]", new JsonObject(response.getMessage())
    // .getString("detail"));
    // testContext.completeNow();
    // })));

    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
    }).onFailure(handler -> {
      LOGGER.debug(" res : "+handler.getMessage());
      testContext.completeNow();
    });
    // {
    // testContext.failed();
    // }).onFailure(handler -> {
    //// LOGGER.debug("failed"+handler);
    //// LOGGER.debug(handler.getMessage().toString());
    //// LOGGER.debug(handler.getLocalizedMessage().toString());
    //// JsonObject json=new JsonObject(handler.getMessage().);
    //// assertEquals("Malformed Id [\"malformed-id\"]", json.getString("detail"));
    // handler.res
    // testContext.completeNow();
    //
    // });
  }

  @Test
  @DisplayName("Testing auto index selection with Invalid resource-id")
  void autoIndexInvalidId(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id",
            new JsonArray()
                .add("ii/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx"
                    + ".io/surat-itms-realtime-information/surat-itms-live-eta"))
          .put("searchType", "geoSearch_")
          .put("lon", 72.8296)
          .put("lat", 21.2)
          .put("radius", 500)
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // LOGGER.debug(response.getMessage());
    // assertEquals("Invalid resource id",
    // new JsonObject(response.getMessage()).getString("detail"));
    // testContext.completeNow();
    // })));

    dbService.search(request).onSuccess(handler -> {
      testContext.failed();
      
    }).onFailure(handler -> {
//      assertEquals("Invalid resource id", handler.getString("detail"));
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing search empty response with 204")
  void searchEmptyResponse(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
          .put("searchType", "geoSearch_")
          .put("lon", 72.8296)
          .put("lat", 23.2)
          .put("radius", 10)
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("Empty response", new JsonObject(response.getMessage()).getString("detail"));
    // testContext.completeNow();
    // })));

    dbService.search(request).onSuccess(handler -> {
      testContext.failNow(handler.toString());
     
    }).onFailure(handler -> {
//      assertEquals("Empty response", handler.getString("detail"));
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing count empty response with 204")
  void countEmptyResponse(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
          .put("searchType", "geoSearch_")
          .put("lon", 72.8296)
          .put("lat", 23.2)
          .put("radius", 10)
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    // dbService.countQuery(request, testContext.failing(response -> testContext.verify(() -> {
    // assertEquals("Empty response", new JsonObject(response.getMessage()).getString("detail"));
    // testContext.completeNow();
    // })));

    dbService.count(request).onSuccess(handler -> {
      testContext.failed();
      
    }).onFailure(handler -> {
//      assertEquals("Empty response", handler.getString("detail"));
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Test getOrDefault method")
  public void test_getOrDefault(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("30302000", "333");
    assertEquals(333, databaseServiceImpl.getOrDefault(jsonObject, "30302000", 12));
    assertEquals(12, databaseServiceImpl.getOrDefault(new JsonObject(), "abcd", 12));
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test countQuery method : with malformed ID")
  public void test_countQuery_for_invalid_id(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, "{ \"key\" : \"value\" }");
    request.put(SEARCH_TYPE, "somevalue");
    request.put(ID, jsonArray);
    JsonObject expected = new JsonObject();


    // databaseServiceImpl.countQuery(request,handler -> {
    // if(handler.succeeded())
    // {
    // vertxTestContext.failNow(handler.cause());
    // }
    // else
    // {
    // assertEquals(expected.toString(),handler.cause().getMessage());
    // vertxTestContext.completeNow();
    // }
    // });

    dbService.count(request).onSuccess(handler -> {
      vertxTestContext.failed();
    }).onFailure(handler -> {
      JsonObject exceptionResponse=new JsonObject(handler.getMessage());
      assertEquals("Exception occured executing query", exceptionResponse.getString("detail"));
      vertxTestContext.completeNow();
    });
  }

  @Test
  @DisplayName("Test countQuery method : with Exception")
  public void test_countQuery_with_exception(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
          .put("geometry", "bbox")
          .put("georel", "within")
          .put("coordinates", "[[72.8296,21.2],[72.8297,21.15]]")
          .put("geoproperty", "location")
          .put(REQ_TIMEREL, null)
          .put(TIME_KEY, null)
          .put("searchType", TEMPORAL_SEARCH_REGEX)
          .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    JsonObject expected = new JsonObject();
    expected.put("type", 400);
    expected.put("title", "Failed");
    expected.put("detail", "text");
    // databaseServiceImpl.countQuery(request,handler -> {
    // if(handler.succeeded())
    // {
    // vertxTestContext.failNow(handler.cause());
    // }
    // else
    // {
    // assertEquals(expected.toString(),handler.cause().getMessage());
    // vertxTestContext.completeNow();
    // }
    // });

    dbService.count(request).onSuccess(handler -> {
      vertxTestContext.failed();
    }).onFailure(handler -> {
      //assertEquals(expected.toString(), handler.getMessage());
      vertxTestContext.completeNow();
    });
  }

  // TODO : enable at last
  // @Test
  // @DisplayName("Test searchQuery method : failure")
  // public void test_searchQuery(VertxTestContext vertxTestContext)
  // {
  // JsonObject jsonObject = mock(JsonObject.class);
  // JsonArray jsonArray = mock(JsonArray.class);
  // JsonObject request =
  // new JsonObject()
  // .put("id",
  // new JsonArray().add(
  // "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
  // .put("geometry", "bbox").put("georel", "within")
  // .put("coordinates", "[[82,25.33],[82.01,25.317]]")
  // .put("geoproperty", "geoJsonLocation")
  // .put("searchType", "geoSearch_")
  // .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
  //
  // when(asyncResult.result()).thenReturn(jsonObject);
  // when(jsonObject.getJsonArray(anyString())).thenReturn(jsonArray);
  // when(jsonArray.getJsonObject(anyInt())).thenReturn(jsonObject);
  // when(jsonObject.getInteger(anyString())).thenReturn(3);
  // when(asyncResult.succeeded()).thenReturn(true,false);
  // when(asyncResult.cause()).thenReturn(throwable);
  // when(throwable.getMessage()).thenReturn("Failure message");
  //
  //
  // Future<JsonObject> countFuture=elasticClient.asyncCount(any(), null);
  //
  // doAnswer(new Answer<AsyncResult<JsonObject>>() {
  // @Override
  // public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
  // ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
  // return null;
  // }
  // }).when(elasticClient).countAsync(anyString(),anyString(),any());
  // doAnswer(new Answer<AsyncResult<JsonObject>>() {
  // @Override
  // public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
  // ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
  // return null;
  // }
  // }).when(elasticClient).asyncSearch(anyString(),any(),any(),any(),any());
  // databaseServiceImpl.searchQuery(request,handler -> {
  // if(handler.succeeded())
  // {
  // vertxTestContext.failNow(handler.cause());
  // }
  // else
  // {
  // assertEquals("Failure message",handler.cause().getMessage());
  // vertxTestContext.completeNow();
  // }
  // });
  // }

  @Test
  @DisplayName("Test setMessage method ")
  public void test_setMessage(VertxTestContext vertxTestContext) {
    ResponseBuilder builder = new ResponseBuilder("status");
    JsonObject jsonObject = mock(JsonObject.class);
    JsonArray jsonArray = mock(JsonArray.class);
    JsonObject expected = new JsonObject();
    expected.put("detail", "dummy failure reason - no idea ! ");

    when(jsonObject.getInteger(anyString())).thenReturn(400);
    when(jsonObject.getJsonObject(anyString())).thenReturn(jsonObject);
    when(jsonObject.getString(anyString())).thenReturn("dummy failure reason - no idea ! ");
    when(jsonObject.getJsonArray(anyString())).thenReturn(jsonArray);
    when(jsonArray.getJsonObject(anyInt())).thenReturn(jsonObject);
    assertNotNull(builder.setMessage(jsonObject));

    assertEquals(expected, builder.getResponse());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test setMessage method ")
  public void test_setMessage2(VertxTestContext vertxTestContext) {
    ResponseBuilder builder = new ResponseBuilder("status");
    JsonObject jsonObject = mock(JsonObject.class);
    JsonArray jsonArray = mock(JsonArray.class);
    JsonObject expected = new JsonObject();
    expected.put("detail", INVALID_RESOURCE_ID);

    when(jsonObject.getInteger(anyString())).thenReturn(404);
    when(jsonObject.getJsonObject(anyString())).thenReturn(jsonObject);
    when(jsonObject.getString(anyString())).thenReturn(INDEX_NOT_FOUND);
    lenient().when(jsonObject.getJsonArray(anyString())).thenReturn(jsonArray);
    lenient().when(jsonArray.getJsonObject(anyInt())).thenReturn(jsonObject);
    assertNotNull(builder.setMessage(jsonObject));

    assertEquals(expected, builder.getResponse());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test setFromParam method")
  public void test_setFromParam(VertxTestContext vertxTestContext) {
    ResponseBuilder builder = new ResponseBuilder("status");
    JsonObject expected = new JsonObject();
    expected.put("from", 3);
    assertNotNull(builder.setFromParam(3));
    assertEquals(expected, builder.getResponse());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test setSizeParam method")
  public void test_setSizeParam(VertxTestContext vertxTestContext) {
    ResponseBuilder builder = new ResponseBuilder("status");
    JsonObject expected = new JsonObject();
    expected.put("size", 3);
    assertNotNull(builder.setSizeParam(3));
    assertEquals(expected, builder.getResponse());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Testing Attribute Search count>5000")
  void searchAttribute3(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
            .put("id", new JsonArray()
                    .add(
                            "b58da193-23d9-43eb-b98a-a103d4b6103c"))
            .put("searchType", "attributeSearch_")
            .put("attr-query",
                    new JsonArray()
                            .add(new JsonObject()
                                    .put("attribute", "referenceLevel")
                                    .put("operator", "!=")
                                    .put("value", "5")))
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

   when(cacheService.get(any(JsonObject.class))).thenReturn(Future.succeededFuture(jsonObject));

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",60000)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));
JsonObject expectedJson = new JsonObject().put("type",413).put("title","urn:dx:rs:payloadTooLarge").put("details","Response size exceeds limit use filters to get value within limit or use async api");
    dbService
        .search(request)
        .onFailure(
            handler -> {
              assertEquals(
                  handler.getMessage(),
                  expectedJson.toString());
              testContext.completeNow();
            })
        .onSuccess(
            handler -> {
              testContext.failed();
            });
  }
  @Test
  @DisplayName("Testing Attribute Search (property is not equal)")
  void searchAttribute2(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
            .put("id", new JsonArray()
                    .add(
                            "b58da193-23d9-43eb-b98a-a103d4b6103c"))
            .put("searchType", "attributeSearch_")
            .put("attr-query",
                    new JsonArray()
                            .add(new JsonObject()
                                    .put("attribute", "referenceLevel")
                                    .put("operator", "!=")
                                    .put("value", "5")))
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    when(cacheService.get(any(JsonObject.class))).thenReturn(Future.succeededFuture(jsonObject));

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",0)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.succeededFuture(esJson));
    when(elasticClient.asyncSearch(anyString(),any(),anyInt(),anyInt(),any())).thenReturn(Future.succeededFuture(esJson));

    dbService.search(request).onSuccess(handler -> {
      assertNotNull(handler.getJsonArray("results"));
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failed();
    });
  }
  @Test
  @DisplayName("Testing Attribute Search - MalFormedID")
  void searchAttribute4(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
            .put("id", new JsonArray()
                    .add(
                            "b58da193-23d9-43eb-b98a-a103d4b6103c"))
            .put("searchType", "attributeSearch_")
            .put("attr-query",
                    new JsonArray()
                            .add(new JsonObject()
                                    .put("attribute", "referenceLevel")
                                    .put("operator", "!=")
                                    .put("value", "5")))
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    List<String> list = new ArrayList<String>();
    list.add("iudx:ResourceGroup");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    when(cacheService.get(any(JsonObject.class))).thenReturn(Future.succeededFuture(jsonObject));

    dbService
        .search(request)
        .onFailure(
            handler -> {
              assertEquals(
                  handler.getMessage(),
                  "{\"status\":400,\"type\":\"urn:dx:rs:badRequest\",\"title\":\"bad request parameter\",\"detail\":\"Malformed Id \"}");
              testContext.completeNow();
            })
        .onSuccess(
            handler -> {
              testContext.failed();
            });
  }
  @Test
  @DisplayName("Testing Attribute Count - MalFormedID")
  void searchAttribute5(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
            .put("id", new JsonArray()
                    .add(
                            "b58da193-23d9-43eb-b98a-a103d4b6103c"))
            .put("searchType", "attributeSearch_")
            .put("attr-query",
                    new JsonArray()
                            .add(new JsonObject()
                                    .put("attribute", "referenceLevel")
                                    .put("operator", "!=")
                                    .put("value", "5")))
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    List<String> list = new ArrayList<String>();
    list.add("iudx:ResourceGroup");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    when(cacheService.get(any(JsonObject.class))).thenReturn(Future.succeededFuture(jsonObject));

    dbService
            .count(request)
            .onFailure(
                    handler -> {
                      assertEquals(
                              handler.getMessage(),
                              "{\"status\":400,\"type\":\"urn:dx:rs:badRequest\",\"title\":\"bad request parameter\",\"detail\":\"Malformed Id \"}");
                      testContext.completeNow();
                    })
            .onSuccess(
                    handler -> {
                      testContext.failed();
                    });
  }
  @Test
  @DisplayName("Testing Attribute Search")
  void searchAttribute6(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
            .put("id", new JsonArray()
                    .add(
                            "b58da193-23d9-43eb-b98a-a103d4b6103c"))
            .put("searchType", "attributeSearch_")
            .put("attr-query",
                    new JsonArray()
                            .add(new JsonObject()
                                    .put("attribute", "referenceLevel")
                                    .put("operator", "!=")
                                    .put("value", "5")))
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    when(cacheService.get(any(JsonObject.class))).thenReturn(Future.succeededFuture(jsonObject));

    JsonObject esJson = new JsonObject().put("type","urn:dx:rs:success").put("title","Success").put("results", new JsonArray().add(new JsonObject().put("totalHits",0)));
    when(elasticClient.asyncCount(anyString(),any())).thenReturn(Future.failedFuture("esJson"));

    dbService.count(request).onFailure(handler -> {
      testContext.completeNow();
    }).onSuccess(handler -> {
      testContext.failed();
    });
  }

  @Test
  @DisplayName("Testing Temporal Queries (During)")
  void searchDuringTemporalFailed(VertxTestContext testContext) throws ParseException {
    JsonObject request = new JsonObject()
            .put("id", new JsonArray().add(idOpen))
            .put("searchType", "temporalSearch_")
            .put("timerel", "during")
            .put("time", temporalStartDate)
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalStartDate, dateTimeFormatter);
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());
    dbSpy.search(request).onSuccess(handler -> {
      testContext.failNow("Failed");
    }).onFailure(handler -> {
      assertEquals("{\"status\":400,\"type\":\"urn:dx:rs:badRequest\",\"title\":\"bad request parameter\",\"detail\":\"No endDate[required mandatory field] provided for query\"}",handler.getMessage());
      LOGGER.info(handler.getMessage());
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing Temporal Queries (During)")
  void searchDuringTemporalFailed2(VertxTestContext testContext) throws ParseException {
    JsonObject request = new JsonObject()
            .put("id", new JsonArray().add(idOpen))
            .put("searchType", "temporalSearch_")
            .put("timerel", "during")
            .put("time", temporalStartDate)
            .put("endtime", "2020-09-15T14:20:00Z")
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalStartDate, dateTimeFormatter);
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());
    dbSpy
        .search(request)
        .onSuccess(
            handler -> {
              testContext.failNow("Failed");
            })
        .onFailure(
            handler -> {
              assertEquals(
                  "{\"status\":400,\"type\":\"urn:dx:rs:badRequest\",\"title\":\"bad request parameter\",\"detail\":\"end date is before start date\"}",
                  handler.getMessage());
              LOGGER.info(handler.getMessage());
              testContext.completeNow();
            });
  }
  @Test
  @DisplayName("Testing Temporal Queries (During)")
  void searchDuringTemporalFailed3(VertxTestContext testContext) throws ParseException {
    JsonObject request = new JsonObject()
            .put("id", new JsonArray().add(idOpen))
            .put("searchType", "temporalSearch_")
            .put("timerel", "during")
            .put("time", "2020-0915T14:20:00Z")
            .put("endtime", "2020-09-15T14:20:00Z")
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalStartDate, dateTimeFormatter);
    List<String> list = new ArrayList<String>();
    list.add("iudx:Resource");
    list.add("iudx:TransitManagement");

    JsonObject jsonObject = new JsonObject()
            .put("id", "b58da193-23d9-43eb-b98a-a103d4b6103c")
            .put("type", list)
            .put("name","dummy_name")
            .put("resourceGroup","5b7556b5-0779-4c47-9cf2-3f209779aa22");

    doAnswer(Answer -> Future.succeededFuture(jsonObject))
            .when(dbSpy).checkQuery(any());
    dbSpy
        .search(request)
        .onSuccess(
            handler -> {
              testContext.failNow("Failed");
            })
        .onFailure(
            handler -> {
              assertEquals(
                  "{\"status\":400,\"type\":\"urn:dx:rs:badRequest\",\"title\":\"bad request parameter\",\"detail\":\"exception while parsing date/time\"}",
                  handler.getMessage());
              LOGGER.info(handler.getMessage());
              testContext.completeNow();
            });
  }
}


