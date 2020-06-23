package iudx.resource.server.apiserver;

import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.resource.server.apiserver.query.NGSILDQueryParams;
import iudx.resource.server.apiserver.query.QueryMapper;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.response.RestResponse;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.database.DatabaseService;
import iudx.resource.server.databroker.DataBrokerService;
import iudx.resource.server.filedownload.FileDownloadService;
import iudx.resource.server.media.MediaService;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;


/**
 * The Resource Server API Verticle.
 * <h1>Resource Server API Verticle</h1>
 * <p>
 * The API Server verticle implements the IUDX Resource Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 * </p>
 * 
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @version 1.0
 * @since 2020-05-31
 */

public class ApiServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiServerVerticle.class);
  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private ServiceDiscovery discovery;
  private DatabaseService database;
  private DataBrokerService databroker;
  private AuthenticationService authenticator;
  private FileDownloadService filedownload;
  private MediaService media;
  private HttpServer server;
  private Router router;
  private Properties properties;
  private InputStream inputstream;
  private final int port = 8443;
  private String keystore;
  private String keystorePassword;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, reads the
   * configuration, obtains a proxy for the Event bus services exposed through service discovery,
   * start an HTTPs server at port 8443.
   * 
   * @throws Exception which is a startup exception
   */

  @Override
  public void start() throws Exception {

    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("Accept");
    allowedHeaders.add("token");
    allowedHeaders.add("Content-Length");
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("Host");
    allowedHeaders.add("Origin");
    allowedHeaders.add("Referer");
    allowedHeaders.add("Access-Control-Allow-Origin");

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {

        vertx = res.result();
        router = Router.router(vertx);
        properties = new Properties();
        inputstream = null;

        /* Define the APIs, methods, endpoints and associated methods. */

        router = Router.router(vertx);
        router.route().handler(
            CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
        router.route("/apis/*").handler(StaticHandler.create());
        router.route().handler(BodyHandler.create());

        /* NGSI-LD api endpoints */
        router.get(Constants.NGSILD_ENTITIES_URL).handler(this::handleEntitiesQuery);
        router.get(Constants.NGSILD_TEMPORAL_URL).handler(this::handleTemporalQuery);
        router.post(Constants.NGSILD_SUBSCRIPTION_URL).handler(this::handleSubscriptions);

        /* Read the configuration and set the HTTPs server properties. */

        try {

          inputstream = new FileInputStream("config.properties");
          properties.load(inputstream);

          keystore = properties.getProperty("keystore");
          keystorePassword = properties.getProperty("keystorePassword");

        } catch (Exception ex) {

          LOGGER.info(ex.toString());

        }

        /* Setup the HTTPs server properties, APIs and port. */

        server = vertx.createHttpServer(new HttpServerOptions().setSsl(true)
            .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword)));

        server.requestHandler(router).listen(port);

        /* Get a handler for the Service Discovery interface. */

        discovery = ServiceDiscovery.create(vertx);

        /* Get a handler for the DatabaseService from Service Discovery interface. */

        EventBusService.getProxy(discovery, DatabaseService.class,
            databaseServiceDiscoveryHandler -> {
              if (databaseServiceDiscoveryHandler.succeeded()) {
                database = databaseServiceDiscoveryHandler.result();
                LOGGER.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + database.getClass().getName() + " +++++++ ");
              } else {
                LOGGER.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /* Get a handler for the DataBrokerService from Service Discovery interface. */

        EventBusService.getProxy(discovery, DataBrokerService.class,
            databrokerServiceDiscoveryHandler -> {
              if (databrokerServiceDiscoveryHandler.succeeded()) {
                databroker = databrokerServiceDiscoveryHandler.result();
                LOGGER.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + databroker.getClass().getName() + " +++++++ ");
              } else {
                LOGGER.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /*
         * Get a handler for the AuthenticationService from Service Discovery interface.
         */

        EventBusService.getProxy(discovery, AuthenticationService.class,
            authenticatorServiceDiscoveryHandler -> {
              if (authenticatorServiceDiscoveryHandler.succeeded()) {
                authenticator = authenticatorServiceDiscoveryHandler.result();
                LOGGER.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + authenticator.getClass().getName() + " +++++++ ");
              } else {
                LOGGER.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /*
         * Get a handler for the FileDownloadService from Service Discovery interface.
         */

        EventBusService.getProxy(discovery, FileDownloadService.class,
            filedownloadServiceDiscoveryHandler -> {
              if (filedownloadServiceDiscoveryHandler.succeeded()) {
                filedownload = filedownloadServiceDiscoveryHandler.result();
                LOGGER.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + filedownload.getClass().getName() + " +++++++ ");
              } else {
                LOGGER.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /* Get a handler for the MediaService from Service Discovery interface. */

        EventBusService.getProxy(discovery, MediaService.class, mediaServiceDiscoveryHandler -> {
          if (mediaServiceDiscoveryHandler.succeeded()) {
            media = mediaServiceDiscoveryHandler.result();
            LOGGER
                .info("\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                    + media.getClass().getName() + " +++++++ ");
          } else {
            LOGGER.info("\n +++++++ Service Discovery Failed. +++++++ ");
          }
        });

      }
    });
  }

  /**
   * This method is used to handle all NGSI-LD queries for endpoint /ngsi-ld/v1/entities/**.
   * 
   * @param routingContext RoutingContext Object
   */
  private void handleEntitiesQuery(RoutingContext routingContext) {
    LOGGER.info("handleEntitiesQuery method started.");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    /* JsonObject of authentication related information */
    JsonObject authenticationInfo = new JsonObject();

    /* HTTP request body as Json */
    JsonObject requestBody = routingContext.getBodyAsJson();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader("Host");

    // get query paramaters
    MultiMap params = getQueryParams(routingContext, response).get();
    // validate request parameters
    Validator.validate(params, response);
    // parse query params
    NGSILDQueryParams ngsildquery = new NGSILDQueryParams(params);
    QueryMapper queryMapper = new QueryMapper();
    // create json
    JsonObject json = queryMapper.toJson(ngsildquery, false);
    json.put("instanceID", instanceID);
    LOGGER.info("IUDX query json : " + json);

    /* checking authentication info in requests */
    if (request.headers().contains("token")) {
      authenticationInfo.put("token", request.getHeader("token"));
    } else {
      authenticationInfo.put("token", "public");
    }

    /* Authenticating the request */
    authenticator.tokenInterospect(requestBody, authenticationInfo, authHandler -> {
      if (authHandler.succeeded()) {
        LOGGER
            .info("Authenticating entity search request ".concat(authHandler.result().toString()));

        // call database vertical for seaarch
        database.searchQuery(json, handler -> {
          if (handler.succeeded()) {
            response.putHeader("content-type", "application/json")
                .setStatusCode(ResponseType.Ok.getCode()).end(handler.result().toString());
          } else if (handler.failed()) {
            // handler.cause().getMessage();
            // TODO: get cause from handler message and return appropriate error message.
            response.putHeader("content-type", "application/json")
                .setStatusCode(ResponseType.InternalError.getCode())
                .end(new RestResponse.Builder().withError(ResponseType.InternalError)
                    .withMessage("Internal server error").build().toJsonString());
          }
        });
      } else if (authHandler.failed()) {
        LOGGER.error("Unathorized request".concat(authHandler.cause().toString()));
        response.putHeader("content-type", "application/json")
            .setStatusCode(ResponseType.AuthenticationFailure.getCode())
            .end(new RestResponse.Builder().withError(ResponseType.AuthenticationFailure)
                .withMessage("Unauthorised").build().toJsonString());
      }
    });
  }

  /**
   * This method is used to handler all temporal NGSI-LD queries for endpoint
   * /ngsi-ld/v1/temporal/**.
   * 
   * @param routingContext RoutingContext object
   * 
   */
  private void handleTemporalQuery(RoutingContext routingContext) {
    LOGGER.info("handleTemporalQuery method started.");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    /* JsonObject of authentication related information */
    JsonObject authenticationInfo = new JsonObject();

    /* HTTP request body as Json */
    JsonObject requestBody = routingContext.getBodyAsJson();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader("Host");

    // get query parameters
    MultiMap params = getQueryParams(routingContext, response).get();
    // validate request params
    Validator.validate(params, response);
    // parse query params
    NGSILDQueryParams ngsildquery = new NGSILDQueryParams(params);
    QueryMapper queryMapper = new QueryMapper();
    // create json
    JsonObject json = queryMapper.toJson(ngsildquery, true);
    json.put("instanceID", instanceID);
    LOGGER.info("IUDX temporal json query : " + json);

    /* checking authentication info in requests */
    if (request.headers().contains("token")) {
      authenticationInfo.put("token", request.getHeader("token"));
    } else {
      authenticationInfo.put("token", "public");
    }

    /* Authenticating the request */
    authenticator.tokenInterospect(requestBody, authenticationInfo, authHandler -> {
      if (authHandler.succeeded()) {
        LOGGER.info("Authenticating entity temporal search request "
            .concat(authHandler.result().toString()));

        // call database vertical for seaarch
        database.searchQuery(json, handler -> {
          if (handler.succeeded()) {
            response.putHeader("content-type", "application/json")
                .setStatusCode(ResponseType.Ok.getCode()).end(handler.result().toString());
          } else if (handler.failed()) {
            response.putHeader("content-type", "application/json")
                .setStatusCode(ResponseType.InternalError.getCode())
                .end(new RestResponse.Builder().withError(ResponseType.InternalError)
                    .withMessage("Internal server error").build().toJsonString());
          }
        });
      } else if (authHandler.failed()) {
        LOGGER.error("Unathorized request".concat(authHandler.cause().toString()));
        response.putHeader("content-type", "application/json")
            .setStatusCode(ResponseType.AuthenticationFailure.getCode())
            .end(new RestResponse.Builder().withError(ResponseType.AuthenticationFailure)
                .withMessage("Unauthorised").build().toJsonString());
      }
    });
  }

  /**
   * Method used to handle all subscription requests in NGSI-LD.
   * 
   * @param routingContext routingContext
   */
  // TODO : incomplete need to complete after management api completion
  private void handleSubscriptions(RoutingContext routingContext) {
    LOGGER.info("handleSubscription method started");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    /* JsonObject of authentication related information */
    JsonObject authenticationInfo = new JsonObject();

    /* HTTP request body as Json */
    JsonObject requestBody = routingContext.getBodyAsJson();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader("Host");

    JsonObject requestJsonObject = routingContext.getBodyAsJson();

    /* checking authentication info in requests */
    if (request.headers().contains("token")) {
      authenticationInfo.put("token", request.getHeader("token"));

      authenticator.tokenInterospect(requestJsonObject, authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          JsonArray authJson = authHandler.result();

          JsonObject jsonObj = new JsonObject();
          jsonObj.put("type", "streaming");
          jsonObj.put("name", authJson.getString(0));
          jsonObj.put("consumer", authJson.getString(1));
          jsonObj.put("instanceID", instanceID);

          JsonArray idsJsonArray = new JsonArray();

          jsonObj.put("entities", requestJsonObject.getJsonArray("entities"));

          databroker.registerStreamingSubscription(jsonObj, subsHandler -> {
            if (subsHandler.succeeded()) {
              response.putHeader("content-type", "application/json")
                  .setStatusCode(ResponseType.Created.getCode())
                  .end(new RestResponse.Builder().withError(ResponseType.Created)
                      .withMessage(subsHandler.result().toString()).build().toJsonString());
            } else {
              response.putHeader("content-type", "application/json")
                  .setStatusCode(ResponseType.InternalError.getCode())
                  .end(new RestResponse.Builder().withError(ResponseType.InternalError)
                      .withMessage(subsHandler.cause().toString()).build().toJsonString());
            }
          });
        } else if (authHandler.failed()) {
          response.putHeader("content-type", "application/json")
              .setStatusCode(ResponseType.AuthenticationFailure.getCode())
              .end(new RestResponse.Builder().withError(ResponseType.AuthenticationFailure)
                  .withMessage("Unauthorised").build().toJsonString());
        }
      });
    } else {
      response.putHeader("content-type", "application/json")
          .setStatusCode(ResponseType.AuthenticationFailure.getCode())
          .end(new RestResponse.Builder().withError(ResponseType.AuthenticationFailure)
              .withMessage("Unauthorised").build().toJsonString());

    }
  }

  /**
   * Get the request query parameters delimited by <b>&</b>, <i><b>;</b>(semicolon) is considered as
   * part of the parameter</i>.
   * 
   * @param routingContext RoutingContext Object
   * @param response HttpServerResponse
   * @return Optional Optional of Map
   */
  private Optional<MultiMap> getQueryParams(RoutingContext routingContext,
      HttpServerResponse response) {
    MultiMap queryParams = null;
    try {
      queryParams = MultiMap.caseInsensitiveMultiMap();
      Map<String, List<String>> decodedParams =
          new QueryStringDecoder(routingContext.request().uri(), HttpConstants.DEFAULT_CHARSET,
              true, 1024, true).parameters();
      for (Map.Entry<String, List<String>> entry : decodedParams.entrySet()) {
        queryParams.add(entry.getKey(), entry.getValue());
        System.out.println(entry.getKey() + " : " + entry.getValue());
      }
    } catch (IllegalArgumentException ex) {
      response.putHeader("content-type", "application/json")
          .setStatusCode(ResponseType.BadRequestData.getCode())
          .end(new RestResponse.Builder().withError(ResponseType.BadRequestData)
              .withMessage("Error while decoding query params").build().toJsonString());
    }
    return Optional.of(queryParams);
  }

}
