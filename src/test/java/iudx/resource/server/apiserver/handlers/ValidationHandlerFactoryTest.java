package iudx.resource.server.apiserver.handlers;

import static iudx.resource.server.apiserver.util.Constants.DOMAIN;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_GROUP;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_NAME;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_SERVER;
import static iudx.resource.server.apiserver.util.Constants.USERSHA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.util.RequestType;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ValidationHandlerTest {
  RequestType requestType;
  @Mock
  RoutingContext routingContext;
  @Mock
  HttpServerRequest httpServerRequest;
  ValidationHandler validationHandler;

  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of(RequestType.ENTITY),
        Arguments.of(RequestType.TEMPORAL),
        Arguments.of(RequestType.LATEST),
        Arguments.of(RequestType.POST_TEMPORAL),
        Arguments.of(RequestType.POST_ENTITIES),
        Arguments.of(RequestType.SUBSCRIPTION),
        Arguments.of(RequestType.ASYNC_SEARCH),
        Arguments.of(RequestType.ASYNC_STATUS),
        Arguments.of(RequestType.OVERVIEW)
        );
  }

  @DisplayName("Test handle method")
  @ParameterizedTest
  @MethodSource("data")
  public void testHandle(RequestType value, VertxTestContext vertxTestContext) {
    requestType = value;
    
    //path params
    Map<String, String> stringMap = new HashMap<>();
    stringMap.put("id",
        "b58da193-23d9-43eb-b98a-a103d4b6103c");
    
    //parameters
    MultiMap parameters = MultiMap.caseInsensitiveMultiMap();
    parameters.add("timerel", "during");
    parameters.add("time", "2020-10-18T14:20:00Z");
    parameters.add("endtime", "2021-09-18T14:20:00Z");
    parameters.add("searchId", UUID.randomUUID().toString());
    
    //for latest
    parameters.add("*","b58da193-23d9-43eb-b98a-a103d4b6103c");
    
    //headers
    MultiMap header = MultiMap.caseInsensitiveMultiMap();
    header.add("options", "streaming");
    
    //body
    JsonObject body = null;
    if(requestType.equals(RequestType.POST_ENTITIES)) {
      body=getPostEntitiesJsonRequestBody();
    }else if(requestType.equals(RequestType.POST_TEMPORAL)){
      body = getPostTemporalJsonRequestBody();
    }else if(requestType.equals(RequestType.SUBSCRIPTION)) {
      body=getSubscriptionBody();
    }

    RequestBody requestBody = mock(RequestBody.class);

    when(routingContext.body()).thenReturn(requestBody);
    when(requestBody.asJsonObject()).thenReturn(body);

    when(routingContext.request()).thenReturn(httpServerRequest);
    when(httpServerRequest.params()).thenReturn(parameters);
    when(httpServerRequest.headers()).thenReturn(header);
    when(routingContext.pathParams()).thenReturn(stringMap);

    validationHandler = new ValidationHandler(Vertx.vertx(), requestType);
    validationHandler.handle(routingContext);
    verify(routingContext, times(4)).request();
    verify(httpServerRequest).params();
    verify(routingContext).body();
    verify(routingContext).pathParams();
    vertxTestContext.completeNow();
  }
  
  
  private JsonObject getPostTemporalJsonRequestBody() {
    return new JsonObject("{\n"
        + "    \"type\": \"Query\",\n"
        + "    \"entities\": [\n"
        + "        {\n"
        + "            \"id\": \"b58da193-23d9-43eb-b98a-a103d4b6103c\"\n"
        + "        }\n"
        + "    ],\n"
        + "    \"geoQ\": {\n"
        + "        \"geometry\": \"Point\",\n"
        + "        \"coordinates\": [21.178,72.834],\n"
        + "        \"georel\": \"near;maxDistance=1000\",\n"
        + "        \"geoproperty\": \"location\"\n"
        + "    },\n"
        + "    \"temporalQ\": {\n"
        + "        \"timerel\": \"between\",\n"
        + "        \"time\": \"2020-10-18T14:20:00Z\",\n"
        + "        \"endtime\": \"2020-10-19T14:20:00Z\",\n"
        + "        \"timeProperty\": \"observationDateTime\"\n"
        + "    },\n"
        + "    \"q\":\"speed>30.0\",\n"
        + "    \"attrs\":\"id,speed\"\n"
        + "}");
  }
  
  private JsonObject getPostEntitiesJsonRequestBody() {
    return new JsonObject("{\n"
        + "    \"type\": \"Query\",\n"
        + "    \"entities\": [\n"
        + "        {\n"
        + "            \"id\": \"b58da193-23d9-43eb-b98a-a103d4b6103c\"\n"
        + "        }\n"
        + "    ],\n"
        + "    \"geoQ\": {\n"
        + "        \"geometry\": \"Point\",\n"
        + "        \"coordinates\": [21.178,72.834],\n"
        + "        \"georel\": \"near;maxDistance=10\",\n"
        + "        \"geoproperty\": \"location\"\n"
        + "    }\n"
        + "}");
  }
  
  private JsonObject getSubscriptionBody() {
    return new JsonObject("{\n"
        + "    \"name\": \"integration-test-alias-RL\",\n"
        + "    \"type\": \"subscription\",\n"
        + "    \"entities\": [\"b58da193-23d9-43eb-b98a-a103d4b6103c\"]\n"
        + "}");
  }
}
