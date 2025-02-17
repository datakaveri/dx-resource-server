package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.response.ResponseUtil.generateResponse;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.apiserver.util.Util.errorResponse;
import static iudx.resource.server.cache.cachelmpl.CacheType.CATALOGUE_CACHE;
import static iudx.resource.server.common.Constants.*;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.HttpStatusCode.NOT_FOUND;
import static iudx.resource.server.common.HttpStatusCode.UNAUTHORIZED;
import static iudx.resource.server.common.ResponseUrn.*;
import static iudx.resource.server.database.archives.Constants.ITEM_TYPES;
import static iudx.resource.server.database.archives.Constants.TIME_LIMIT;
import static iudx.resource.server.metering.util.Constants.DELEGATOR_ID;
import static iudx.resource.server.metering.util.Constants.EPOCH_TIME;
import static iudx.resource.server.metering.util.Constants.ISO_TIME;
import static iudx.resource.server.metering.util.Constants.PROVIDER_ID;
import static iudx.resource.server.metering.util.Constants.TYPE_KEY;

import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.handlers.*;
import iudx.resource.server.apiserver.management.ManagementApi;
import iudx.resource.server.apiserver.management.ManagementApiImpl;
import iudx.resource.server.apiserver.query.NgsildQueryParams;
import iudx.resource.server.apiserver.query.QueryMapper;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.service.CatalogueService;
import iudx.resource.server.apiserver.subscription.SubsType;
import iudx.resource.server.apiserver.subscription.SubscriptionService;
import iudx.resource.server.apiserver.util.RequestType;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.authenticator.handler.authentication.AuthHandler;
import iudx.resource.server.authenticator.handler.authorization.*;
import iudx.resource.server.authenticator.model.DxAccess;
import iudx.resource.server.authenticator.model.DxRole;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.RoutingContextHelper;
import iudx.resource.server.database.archives.DatabaseService;
import iudx.resource.server.database.latest.LatestDataService;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import iudx.resource.server.encryption.EncryptionService;
import iudx.resource.server.metering.MeteringService;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Resource Server API Verticle.
 *
 * <h1>Resource Server API Verticle</h1>
 *
 * <p>The API Server verticle implements the IUDX Resource Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 *
 * @version 1.0
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @since 2020-05-31
 */
public class ApiServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);

  /** Service addresses */
  private static final String DATABASE_SERVICE_ADDRESS = "iudx.rs.database.service";

  private static final String BROKER_SERVICE_ADDRESS = "iudx.rs.broker.service";
  private static final String LATEST_SEARCH_ADDRESS = "iudx.rs.latest.service";
  private static final String METERING_SERVICE_ADDRESS = "iudx.rs.metering.service";
  private static final String ENCRYPTION_SERVICE_ADDRESS = "iudx.rs.encryption.service";
  int timeLimitForAsync;
  private HttpServer server;
  private Router router;
  private int port;
  private boolean isssl;
  private String keystore;
  private String keystorePassword;
  private ManagementApi managementApi;
  private SubscriptionService subsService;
  private CatalogueService catalogueService;
  private MeteringService meteringService;
  private DatabaseService database;
  private PostgresService postgresService;
  private DataBrokerService databroker;
  private ParamsValidator validator;
  private EncryptionService encryptionService;
  private String dxApiBasePath;
  private Api api;
  private LatestDataService latestDataService;
  private CacheService cacheService;
  private int timeLimit;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, reads the
   * configuration, obtains a proxy for the Event bus services exposed through service discovery,
   * start an HTTPs server at port 8443 or an HTTP server at port 8080.
   *
   * @throws Exception which is a startup exception TODO Need to add documentation for all the
   */
  @Override
  public void start() throws Exception {

    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add(HEADER_ACCEPT);
    allowedHeaders.add(HEADER_TOKEN);
    allowedHeaders.add(HEADER_CONTENT_LENGTH);
    allowedHeaders.add(HEADER_CONTENT_TYPE);
    allowedHeaders.add(HEADER_HOST);
    allowedHeaders.add(HEADER_ORIGIN);
    allowedHeaders.add(HEADER_REFERER);
    allowedHeaders.add(HEADER_ALLOW_ORIGIN);
    allowedHeaders.add(HEADER_PUBLIC_KEY);
    allowedHeaders.add(HEADER_RESPONSE_FILE_FORMAT);
    allowedHeaders.add(HEADER_OPTIONS);

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);

    /* Create a reference to HazelcastClusterManager. */
    String[] timeLimitConfig = config().getString(TIME_LIMIT).split(",");
    timeLimit = Integer.valueOf(timeLimitConfig[2]);
    timeLimitForAsync = config().getInteger("timeLimitForAsync");

    router = Router.router(vertx);

    /* Get base paths from config */
    dxApiBasePath = config().getString("dxApiBasePath");
    api = Api.getInstance(dxApiBasePath);
    cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);

    /* Define the APIs, methods, endpoints and associated methods. */

    router = Router.router(vertx);
    router
        .route()
        .handler(
            CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));

    router
        .route()
        .handler(
            requestHandler -> {
              requestHandler
                  .response()
                  .putHeader("Cache-Control", "no-cache, no-store,  must-revalidate,max-age=0")
                  .putHeader("Pragma", "no-cache")
                  .putHeader("Expires", "0")
                  .putHeader("X-Content-Type-Options", "nosniff");
              requestHandler.next();
            });

    // attach custom http error responses to router
    HttpStatusCode[] statusCodes = HttpStatusCode.values();
    Stream.of(statusCodes)
        .forEach(
            code -> {
              router.errorHandler(
                  code.getValue(),
                  errorHandler -> {
                    HttpServerResponse response = errorHandler.response();
                    if (response.headWritten()) {
                      try {
                        response.close();
                      } catch (RuntimeException e) {
                        LOGGER.error("Error : " + e);
                      }
                      return;
                    }
                    response
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(code.getValue())
                        .end(errorResponse(code));
                  });
            });

    router.route().handler(BodyHandler.create());
    router.route().handler(TimeoutHandler.create(50000, 408));
    FailureHandler validationsFailureHandler = new FailureHandler();
    ValidationHandler entityValidationHandler = new ValidationHandler(vertx, RequestType.ENTITY);
    AuthenticationService authenticator =
        AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    AuthHandler authHandler = new AuthHandler(api, authenticator);
    GetIdHandler getIdHandler = new GetIdHandler(api);
    catalogueService = new CatalogueService(cacheService, config(), vertx);

    Handler<RoutingContext> userAndAdminAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(
                DxRole.DELEGATE, DxRole.CONSUMER, DxRole.PROVIDER, DxRole.ADMIN);
    Handler<RoutingContext> providerAndAdminAccessHandler =
        new AuthorizationHandler()
            .setUserRolesForEndpoint(DxRole.DELEGATE, DxRole.PROVIDER, DxRole.ADMIN);
    //    Handler<RoutingContext> providerAccessHandler =
    //        new AuthorizationHandler().setUserRolesForEndpoint(DxRole.DELEGATE, DxRole.PROVIDER);

    Handler<RoutingContext> apiConstraint =
        new ConstraintsHandlerForConsumer().consumerConstraintsForEndpoint(DxAccess.API);
    //    Handler<RoutingContext> mgmtConstraint =
    //        new
    // ConstraintsHandlerForConsumer().consumerConstraintsForEndpoint(DxAccess.MANAGEMENT);
    Handler<RoutingContext> subscriptionConstraint =
        new ConstraintsHandlerForConsumer().consumerConstraintsForEndpoint(DxAccess.SUBSCRIPTION);

    /*TODO: update example config-dev and config-dev */
    String audience = config().getString("audience");
    Handler<RoutingContext> isTokenRevoked = new TokenRevokedHandler(cacheService).isTokenRevoked();
    Handler<RoutingContext> validateToken =
        new AuthValidationHandler(api, cacheService, audience, catalogueService);

    /* NGSI-LD api endpoints */
    router
        .get(api.getEntitiesUrl())
        .handler(entityValidationHandler)
        .handler(getIdHandler.withNormalisedPath(api.getEntitiesUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::handleEntitiesQuery)
        .failureHandler(validationsFailureHandler);

    ValidationHandler latestValidationHandler = new ValidationHandler(vertx, RequestType.LATEST);
    router
        .get(api.getEntitiesUrl() + "/*")
        .handler(latestValidationHandler)
        .handler(getIdHandler.withNormalisedPath(api.getEntitiesUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::handleLatestEntitiesQuery)
        .failureHandler(validationsFailureHandler);

    ValidationHandler postTemporalValidationHandler =
        new ValidationHandler(vertx, RequestType.POST_TEMPORAL);
    router
        .post(api.getPostTemporalQueryPath())
        .consumes(APPLICATION_JSON)
        .handler(postTemporalValidationHandler)
        .handler(getIdHandler.withNormalisedPath(api.getPostTemporalQueryPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::handlePostEntitiesQuery)
        .failureHandler(validationsFailureHandler);

    ValidationHandler postEntitiesValidationHandler =
        new ValidationHandler(vertx, RequestType.POST_ENTITIES);
    router
        .post(api.getPostEntitiesQueryPath())
        .consumes(APPLICATION_JSON)
        .handler(postEntitiesValidationHandler)
        .handler(getIdHandler.withNormalisedPath(api.getPostEntitiesQueryPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::handlePostEntitiesQuery)
        .failureHandler(validationsFailureHandler);

    ValidationHandler temporalValidationHandler =
        new ValidationHandler(vertx, RequestType.TEMPORAL);
    router
        .get(api.getTemporalUrl())
        .handler(temporalValidationHandler)
        .handler(getIdHandler.withNormalisedPath(api.getTemporalUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::handleTemporalQuery)
        .failureHandler(validationsFailureHandler);

    // create sub
    ValidationHandler subsValidationHandler =
        new ValidationHandler(vertx, RequestType.SUBSCRIPTION);
    router
        .post(api.getSubscriptionUrl())
        .handler(subsValidationHandler)
        .handler(getIdHandler.withNormalisedPath(api.getSubscriptionUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::handleSubscriptions)
        .failureHandler(validationsFailureHandler);
    // append sub
    router
        .patch(api.getSubscriptionUrl() + "/:userid/:alias")
        .handler(subsValidationHandler)
        .handler(getIdHandler.withNormalisedPath(api.getSubscriptionUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::appendSubscription)
        .failureHandler(validationsFailureHandler);
    // update sub
    router
        .put(api.getSubscriptionUrl() + "/:userid/:alias")
        .handler(subsValidationHandler)
        .handler(getIdHandler.withNormalisedPath(api.getSubscriptionUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::updateSubscription)
        .failureHandler(validationsFailureHandler);
    // get sub
    router
        .get(api.getSubscriptionUrl() + "/:userid/:alias")
        .handler(getIdHandler.withNormalisedPath(api.getSubscriptionUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getSubscription)
        .failureHandler(validationsFailureHandler);

    // get sub for all queue
    router
        .get(api.getSubscriptionUrl())
        .handler(getIdHandler.withNormalisedPath(api.getSubscriptionUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getAllSubscriptionForUser)
        .failureHandler(validationsFailureHandler);

    // delete sub
    router
        .delete(api.getSubscriptionUrl() + "/:userid/:alias")
        .handler(getIdHandler.withNormalisedPath(api.getSubscriptionUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(subscriptionConstraint)
        .handler(isTokenRevoked)
        .handler(this::deleteSubscription)
        .failureHandler(validationsFailureHandler);

    /* Management Api endpoints */
    // Exchange

    router
        .get(api.getIudxConsumerAuditUrl())
        .handler(getIdHandler.withNormalisedPath(api.getIudxConsumerAuditUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::getConsumerAuditDetail)
        .failureHandler(validationsFailureHandler);

    router
        .get(api.getIudxProviderAuditUrl())
        .handler(getIdHandler.withNormalisedPath(api.getIudxProviderAuditUrl()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getProviderAuditDetail)
        .failureHandler(validationsFailureHandler);

    // adapter
    router
        .post(api.getIngestionPath())
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::registerAdapter)
        .failureHandler(validationsFailureHandler);

    router
        .delete(api.getIngestionPath() + "/*")
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::deleteAdapter)
        .failureHandler(validationsFailureHandler);

    router
        .get(api.getIngestionPath() + "/:UUID")
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getAdapterDetails)
        .failureHandler(validationsFailureHandler);

    router
        .post(api.getIngestionPath() + "/heartbeat")
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::publishHeartbeat)
        .failureHandler(validationsFailureHandler);

    router
        .post(api.getIngestionPathEntities())
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPathEntities()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::publishDataFromAdapter)
        .failureHandler(validationsFailureHandler);

    router
        .get(api.getIngestionPath())
        .handler(getIdHandler.withNormalisedPath(api.getIngestionPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(providerAndAdminAccessHandler)
        .handler(isTokenRevoked)
        .handler(this::getAllAdaptersForUsers)
        .failureHandler(validationsFailureHandler);

    // Metering extension
    router
        .get(api.getMonthlyOverview())
        .handler(getIdHandler.withNormalisedPath(api.getMonthlyOverview()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::getMonthlyOverview)
        .failureHandler(validationsFailureHandler);

    // Metering Summary
    router
        .get(api.getSummaryPath())
        .handler(getIdHandler.withNormalisedPath(api.getSummaryPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        .handler(apiConstraint)
        .handler(isTokenRevoked)
        .handler(this::getAllSummaryHandler)
        .failureHandler(validationsFailureHandler);

    /* Documentation routes */
    /* Static Resource Handler */
    /* Get openapiv3 spec */
    router
        .get(ROUTE_STATIC_SPEC)
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/openapi.yaml");
            });
    /* Get redoc */
    router
        .get(ROUTE_DOC)
        .produces(MIME_TEXT_HTML)
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/apidoc.html");
            });

    /* Read ssl configuration. */
    isssl = config().getBoolean("ssl");

    HttpServerOptions serverOptions = new HttpServerOptions();
    if (isssl) {

      /* Read the configuration and set the HTTPs server properties. */

      keystore = config().getString("keystore");
      keystorePassword = config().getString("keystorePassword");

      /*
       * Default port when ssl is enabled is 8443. If set through config, then that value is taken
       */
      port = config().getInteger("httpPort") == null ? 8443 : config().getInteger("httpPort");

      /* Setup the HTTPs server properties, APIs and port. */

      serverOptions
          .setSsl(true)
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));
      LOGGER.info("Info: Starting HTTPs server at port" + port);

    } else {

      /* Setup the HTTP server properties, APIs and port. */

      serverOptions.setSsl(false);
      /*
       * Default port when ssl is disabled is 8080. If set through config, then that value is taken
       */
      port = config().getInteger("httpPort") == null ? 8080 : config().getInteger("httpPort");
      LOGGER.info("Info: Starting HTTP server at port" + port);
    }

    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    server = vertx.createHttpServer(serverOptions);
    server.requestHandler(router).listen(port);

    /* Get a handler for the Service Discovery interface. */

    database = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);
    databroker = DataBrokerService.createProxy(vertx, BROKER_SERVICE_ADDRESS);
    meteringService = MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);
    latestDataService = LatestDataService.createProxy(vertx, LATEST_SEARCH_ADDRESS);
    managementApi = new ManagementApiImpl();
    subsService = new SubscriptionService();
    validator = new ParamsValidator(catalogueService);

    postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    encryptionService = EncryptionService.createProxy(vertx, ENCRYPTION_SERVICE_ADDRESS);

    router
        .route(api.getAsyncPath() + "/*")
        .subRouter(new AsyncRestApi(vertx, router, api, timeLimitForAsync, config()).init());

    router.route(ADMIN + "/*").subRouter(new AdminRestApi(vertx, router, api).init());

    router
        .post(api.getManagementApiPath())
        .handler(getIdHandler.withNormalisedPath(api.getManagementApiPath()))
        .handler(authHandler)
        .handler(validateToken)
        .handler(userAndAdminAccessHandler)
        //        .handler(mgmtConstraint) //TODO: uncomment after DxAccess.MANAGEMENT is added in
        // token constraints
        .handler(
            handler -> {
              new ManagementRestApi(databroker).resetPassword(handler);
            })
        .failureHandler(validationsFailureHandler);

    router
        .route()
        .last()
        .handler(
            requestHandler -> {
              HttpServerResponse response = requestHandler.response();
              response
                  .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                  .setStatusCode(404)
                  .end(generateResponse(NOT_FOUND, YET_NOT_IMPLEMENTED_URN).toString());
            });

    /* Print the deployed endpoints */
    printDeployedEndpoints(router);
    LOGGER.info("API server deployed on :" + serverOptions.getPort());
  }

  private void getMonthlyOverview(RoutingContext routingContext) {
    LOGGER.trace("Info: getMonthlyOverview Started.");
    HttpServerResponse response = routingContext.response();
    String startTime = RoutingContextHelper.getStartTime(routingContext);
    String endTime = RoutingContextHelper.getEndTime(routingContext);
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    String iid = jwtData.getIid().split(":")[1];
    String role = jwtData.getRole();

    if (!VALIDATION_ID_PATTERN.matcher(iid).matches()
        && (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate"))) {
      JsonObject jsonResponse =
          generateResponse(UNAUTHORIZED, UNAUTHORIZED_RESOURCE_URN, "Not Authorized");
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(401)
          .end(jsonResponse.toString());
      return;
    }

    meteringService.monthlyOverview(
        jwtData,
        startTime,
        endTime,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.debug("Successful");
            handleSuccessResponse(response, ResponseType.Ok.getCode(), handler.result().toString());
          } else {
            LOGGER.error("Fail: Bad request");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  private void getAllSummaryHandler(RoutingContext routingContext) {
    LOGGER.trace("Info: getAllSummary Started.");

    String startTime = RoutingContextHelper.getStartTime(routingContext);
    String endTime = RoutingContextHelper.getEndTime(routingContext);
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerResponse response = routingContext.response();

    String iid = jwtData.getIid().split(":")[1];
    String role = jwtData.getRole();

    if (!VALIDATION_ID_PATTERN.matcher(iid).matches()
        && (role.equalsIgnoreCase("provider") || role.equalsIgnoreCase("delegate"))) {
      JsonObject jsonResponse =
          generateResponse(UNAUTHORIZED, UNAUTHORIZED_RESOURCE_URN, "Not Authorized");
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(401)
          .end(jsonResponse.toString());
      return;
    }

    meteringService.summaryOverview(
        jwtData,
        startTime,
        endTime,
        handler -> {
          if (handler.succeeded()) {
            JsonObject jsonObject = handler.result();
            String checkType = jsonObject.getString("type");
            if (checkType.equalsIgnoreCase(NO_CONTENT)) {
              handleSuccessResponse(
                  response, ResponseType.NoContent.getCode(), handler.result().toString());
            } else {
              LOGGER.debug("Successful");
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), handler.result().toString());
            }
          } else {
            LOGGER.error("Fail: Bad request");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  private void printDeployedEndpoints(Router router) {
    for (Route route : router.getRoutes()) {
      if (route.getPath() != null) {
        LOGGER.info("API Endpoints deployed : " + route.methods() + " : " + route.getPath());
      }
    }
  }

  private Future<Void> getConsumerAuditDetail(RoutingContext routingContext) {
    LOGGER.trace("Info: getConsumerAuditDetail Started.");

    JsonObject entries = new JsonObject();
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();
    String userId = jwtData.getSub();
    String apiEndpoint = api.getIudxConsumerAuditUrl();

    entries.put("userid", userId);
    entries.put("endPoint", apiEndpoint);
    entries.put("startTime", request.getParam("time"));
    entries.put("endTime", request.getParam("endTime"));
    entries.put("timeRelation", request.getParam("timerel"));
    entries.put("options", request.headers().get("options"));
    entries.put("resourceId", request.getParam("id"));
    entries.put("api", request.getParam("api"));
    entries.put("offset", request.getParam("offset"));
    entries.put("limit", request.getParam("limit"));

    LOGGER.debug(entries);
    Promise<Void> promise = Promise.promise();
    HttpServerResponse response = routingContext.response();
    meteringService.executeReadQuery(
        entries,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.debug("Table Reading Done.");
            JsonObject jsonObject = handler.result();
            String checkType = jsonObject.getString("type");
            if (checkType.equalsIgnoreCase(NO_CONTENT)) {
              handleSuccessResponse(
                  response, ResponseType.NoContent.getCode(), handler.result().toString());
            } else {
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), handler.result().toString());
            }
            promise.complete();
          } else {
            LOGGER.error("Fail msg " + handler.cause().getMessage());
            LOGGER.error("Table reading failed.");
            processBackendResponse(response, handler.cause().getMessage());
            promise.complete();
          }
        });
    return promise.future();
  }

  private Future<Void> getProviderAuditDetail(RoutingContext routingContext) {
    LOGGER.trace("Info: getProviderAuditDetail Started.");
    JsonObject entries = new JsonObject();
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    HttpServerRequest request = routingContext.request();

    String userId = jwtData.getSub();
    String apiEndpoint = api.getIudxProviderAuditUrl();
    String iid = jwtData.getIid().split(":")[1];

    entries.put("endPoint", apiEndpoint);
    entries.put("userid", userId);
    entries.put("iid", iid);
    entries.put("startTime", request.getParam("time"));
    entries.put("endTime", request.getParam("endTime"));
    entries.put("timeRelation", request.getParam("timerel"));
    entries.put("providerID", request.getParam("providerID"));
    entries.put("consumerID", request.getParam("consumer"));
    entries.put("resourceId", request.getParam("id"));
    entries.put("api", request.getParam("api"));
    entries.put("options", request.headers().get("options"));
    entries.put("offset", request.getParam(OFFSETPARAM));
    entries.put("limit", request.getParam(LIMITPARAM));

    LOGGER.debug(entries);
    Promise<Void> promise = Promise.promise();
    HttpServerResponse response = routingContext.response();
    meteringService.executeReadQuery(
        entries,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.debug("Table Reading Done.");
            JsonObject jsonObject = handler.result();
            String checkType = jsonObject.getString("type");
            if (checkType.equalsIgnoreCase(NO_CONTENT)) {
              handleSuccessResponse(
                  response, ResponseType.NoContent.getCode(), handler.result().toString());
            } else {
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), handler.result().toString());
            }
            promise.complete();
          } else {
            LOGGER.error("Fail msg " + handler.cause().getMessage());
            LOGGER.error("Table reading failed.");
            processBackendResponse(response, handler.cause().getMessage());
            promise.complete();
          }
        });
    return promise.future();
  }

  private void handleLatestEntitiesQuery(RoutingContext routingContext) {
    LOGGER.trace("Info:handleLatestEntitiesQuery method started.;");
    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    // get query parameters
    MultiMap params = getQueryParams(routingContext, response).get();
    if (!params.isEmpty()) {
      RuntimeException ex =
          new RuntimeException("Query parameters are not allowed with latest query");
      routingContext.fail(ex);
    }
    Map<String, String> pathParams = routingContext.pathParams();
    String id = pathParams.get("*");

    JsonObject json = new JsonObject();

    /* HTTP request instance/host details */
    String instanceId = request.getHeader(HEADER_HOST);
    json.put(JSON_INSTANCEID, instanceId);
    json.put(JSON_ID, new JsonArray().add(id));
    json.put(JSON_SEARCH_TYPE, "latestSearch");
    LOGGER.debug("Info: IUDX query json;" + json);
    Future<List<String>> filtersFuture = catalogueService.getApplicableFilters(id);
    filtersFuture.onComplete(
        filtersHandler -> {
          if (filtersHandler.succeeded()) {
            json.put("applicableFilters", filtersHandler.result());
            executeLatestSearchQuery(routingContext, json, response);
          } else {
            LOGGER.error("catalogue item/group doesn't have filters.");
            handleResponse(
                response, BAD_REQUEST, INVALID_PARAM_URN, filtersHandler.cause().getMessage());
          }
        });
  }

  /**
   * This method is used to handle all NGSI-LD queries for endpoint /ngsi-ld/v1/entities/**.
   *
   * @param routingContext RoutingContext Object
   */
  private void handleEntitiesQuery(RoutingContext routingContext) {
    LOGGER.trace("Info:handleEntitiesQuery method started.;");
    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();
    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    // get query paramaters
    MultiMap params = getQueryParams(routingContext, response).get();
    // validate request parameters
    Future<Boolean> validationResult = validator.validate(params);
    validationResult.onComplete(
        validationHandler -> {
          if (validationHandler.succeeded()) {
            // parse query params
            NgsildQueryParams ngsildquery = new NgsildQueryParams(params);
            if (isTemporalParamsPresent(ngsildquery)) {
              DxRuntimeException ex =
                  new DxRuntimeException(
                      BAD_REQUEST.getValue(),
                      INVALID_TEMPORAL_PARAM_URN,
                      "Temporal parameters are not allowed in entities query.");
              routingContext.fail(ex);
            }
            // create json
            JsonObject json;
            QueryMapper queryMapper = new QueryMapper(routingContext, timeLimit);
            json = queryMapper.toJson(ngsildquery, false);
            /* HTTP request instance/host details */
            String instanceId = request.getHeader(HEADER_HOST);
            json.put(JSON_INSTANCEID, instanceId);
            LOGGER.debug("Info: IUDX query json;" + json);
            /* HTTP request body as Json */
            JsonObject requestBody = new JsonObject();
            requestBody.put("ids", json.getJsonArray("id"));
            Future<List<String>> filtersFuture =
                catalogueService.getApplicableFilters(json.getJsonArray("id").getString(0));
            filtersFuture.onComplete(
                filtersHandler -> {
                  if (filtersHandler.succeeded()) {
                    json.put("applicableFilters", filtersHandler.result());
                    if (json.containsKey(IUDXQUERY_OPTIONS)
                        && JSON_COUNT.equalsIgnoreCase(json.getString(IUDXQUERY_OPTIONS))) {
                      executeCountQuery(routingContext, json, response);
                    } else {
                      executeSearchQuery(routingContext, json, response);
                    }
                  } else if (validationHandler.failed()) {
                    handleResponse(
                        response,
                        BAD_REQUEST,
                        INVALID_PARAM_URN,
                        validationHandler.cause().getMessage());
                  }
                });
          } else if (validationHandler.failed()) {
            handleResponse(
                response, BAD_REQUEST, INVALID_PARAM_URN, validationHandler.cause().getMessage());
          }
        });
  }

  /**
   * this method is used to handle all entities queries from post endpoint.
   *
   * @param routingContext routingContext
   */
  public void handlePostEntitiesQuery(RoutingContext routingContext) {
    LOGGER.trace("Info: handlePostEntitiesQuery method started.");
    HttpServerRequest request = routingContext.request();
    JsonObject requestJson = routingContext.body().asJsonObject();
    LOGGER.debug("Info: request Json :: ;" + requestJson);
    HttpServerResponse response = routingContext.response();
    // get query paramaters
    MultiMap params = getQueryParams(routingContext, response).get();
    // validate request parameters
    Future<Boolean> validationResult = validator.validate(requestJson);
    validationResult.onComplete(
        validationHandler -> {
          if (validationHandler.succeeded()) {
            // parse query params
            NgsildQueryParams ngsildquery = new NgsildQueryParams(requestJson);
            QueryMapper queryMapper = new QueryMapper(routingContext, timeLimit);
            JsonObject json = queryMapper.toJson(ngsildquery, requestJson.containsKey("temporalQ"));
            String instanceId = request.getHeader(HEADER_HOST);
            json.put(JSON_INSTANCEID, instanceId);
            requestJson.put("ids", json.getJsonArray("id"));
            LOGGER.debug("Info: IUDX query json : ;" + json);
            Future<List<String>> filtersFuture =
                catalogueService.getApplicableFilters(json.getJsonArray("id").getString(0));
            filtersFuture.onComplete(
                filtersHandler -> {
                  if (filtersHandler.succeeded()) {
                    json.put("applicableFilters", filtersHandler.result());
                    // Add limit and offset value for pagination
                    if (params.contains("limit") && params.contains("offset")) {
                      json.put("limit", params.get("limit"));
                      json.put("offset", params.get("offset"));
                    }
                    if (json.containsKey(IUDXQUERY_OPTIONS)
                        && JSON_COUNT.equalsIgnoreCase(json.getString(IUDXQUERY_OPTIONS))) {
                      executeCountQuery(routingContext, json, response);
                    } else {
                      executeSearchQuery(routingContext, json, response);
                    }
                  } else {
                    LOGGER.error("catalogue item/group doesn't have filters.");
                  }
                });
          } else if (validationHandler.failed()) {
            handleResponse(
                response, BAD_REQUEST, INVALID_PARAM_URN, validationHandler.cause().getMessage());
          }
        });
  }

  /**
   * Execute a count query in DB
   *
   * @param json valid json query
   * @param response HttpServerResponse
   */
  private void executeCountQuery(
      RoutingContext context, JsonObject json, HttpServerResponse response) {
    Future<JsonObject> countQueryDbFuture = database.count(json);
    countQueryDbFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Count Success");
            if (context.request().getHeader(HEADER_PUBLIC_KEY) == null) {
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), handler.result().toString());
              context.data().put(RESPONSE_SIZE, response.bytesWritten());
              Future.future(fu -> updateAuditTable(context));
            } else {
              //                Encryption
              Future<JsonObject> future =
                  encryption(context, handler.result().getJsonArray("results").toString());
              future.onComplete(
                  encryptionHandler -> {
                    if (encryptionHandler.succeeded()) {
                      JsonObject result = encryptionHandler.result();
                      handler.result().put("results", result);
                      handleSuccessResponse(
                          response, ResponseType.Ok.getCode(), handler.result().encode());
                      context.data().put(RESPONSE_SIZE, response.bytesWritten());
                      Future.future(fu -> updateAuditTable(context));
                    } else {
                      LOGGER.error(
                          "Encryption not completed: " + encryptionHandler.cause().getMessage());
                      processBackendResponse(response, encryptionHandler.cause().getMessage());
                    }
                  });
            }
          } else if (handler.failed()) {
            LOGGER.error("Fail: Count Fail");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  /**
   * Execute a search query in DB
   *
   * @param json valid json query
   * @param response HttpServerResponse
   */
  private void executeSearchQuery(
      RoutingContext context, JsonObject json, HttpServerResponse response) {
    Future<JsonObject> searchDbFuture = database.search(json);
    searchDbFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Search Success");
            if (context.request().getHeader(HEADER_PUBLIC_KEY) == null) {
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), handler.result().toString());
              context.data().put(RESPONSE_SIZE, response.bytesWritten());
              Future.future(fu -> updateAuditTable(context));
            } else {
              // Encryption
              Future<JsonObject> future =
                  encryption(context, handler.result().getJsonArray("results").toString());
              future.onComplete(
                  encryptionHandler -> {
                    if (encryptionHandler.succeeded()) {
                      JsonObject result = encryptionHandler.result();
                      handler.result().put("results", result);
                      handleSuccessResponse(
                          response, ResponseType.Ok.getCode(), handler.result().encode());
                      context.data().put(RESPONSE_SIZE, response.bytesWritten());
                      Future.future(fu -> updateAuditTable(context));
                    } else {
                      LOGGER.error("Encryption not completed");
                      processBackendResponse(response, encryptionHandler.cause().getMessage());
                    }
                  });
            }
          } else if (handler.failed()) {
            LOGGER.error("Fail: Search Fail");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  private void executeLatestSearchQuery(
      RoutingContext context, JsonObject json, HttpServerResponse response) {
    latestDataService.getLatestData(
        json,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Latest data search succeeded");
            if (context.request().getHeader(HEADER_PUBLIC_KEY) == null) {
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), handler.result().toString());
              context.data().put(RESPONSE_SIZE, response.bytesWritten());
              Future.future(fu -> updateAuditTable(context));
            } else {
              //                Encryption
              Future<JsonObject> future =
                  encryption(context, handler.result().getJsonArray("results").toString());
              future.onComplete(
                  encryptionHandler -> {
                    if (encryptionHandler.succeeded()) {
                      JsonObject result = encryptionHandler.result();
                      handler.result().put("results", result);
                      handleSuccessResponse(
                          response, ResponseType.Ok.getCode(), handler.result().encode());
                      context.data().put(RESPONSE_SIZE, response.bytesWritten());
                      Future.future(fu -> updateAuditTable(context));
                    } else {
                      LOGGER.error("Encryption not completed");
                      processBackendResponse(response, encryptionHandler.cause().getMessage());
                    }
                  });
            }
          } else {
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  /**
   * Encrypts the result of API response. Used for Search and Count APIs
   *
   * @param context Routing Context
   * @param result value of result param in the API response
   * @return encrypted data
   */
  private Future<JsonObject> encryption(RoutingContext context, String result) {
    String urlBase64PublicKey = context.request().getHeader(HEADER_PUBLIC_KEY);
    Promise<JsonObject> promise = Promise.promise();

    /* get the urlbase64 public key from the header and send it for encryption */
    Future<JsonObject> future =
        encryptionService.encrypt(result, new JsonObject().put(ENCODED_KEY, urlBase64PublicKey));
    future.onComplete(
        handler -> {
          if (handler.succeeded()) {
            /*  get encoded cipher text */
            String encodedCipherText = handler.result().getString(ENCODED_CIPHER_TEXT);
            /* Send back the encoded cipherText to Client */
            final JsonObject jsonObject = new JsonObject();
            jsonObject.put(ENCRYPTED_DATA, encodedCipherText);
            promise.complete(jsonObject);
          } else {
            LOGGER.error("Failure in handler : " + handler.cause().getMessage());
            promise.fail("Failure in handler");
          }
        });
    return promise.future();
  }

  /**
   * This method is used to handler all temporal NGSI-LD queries for endpoint
   * /ngsi-ld/v1/temporal/**.
   *
   * @param routingContext RoutingContext object
   */
  private void handleTemporalQuery(RoutingContext routingContext) {
    LOGGER.trace("Info: handleTemporalQuery method started.");
    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    /* HTTP request instance/host details */
    String instanceId = request.getHeader(HEADER_HOST);
    // get query parameters
    MultiMap params = getQueryParams(routingContext, response).get();
    // validate request params
    Future<Boolean> validationResult = validator.validate(params);
    validationResult.onComplete(
        validationHandler -> {
          if (validationHandler.succeeded()) {
            // parse query params
            NgsildQueryParams ngsildquery = new NgsildQueryParams(params);
            // create json
            QueryMapper queryMapper = new QueryMapper(routingContext, timeLimit);

            JsonObject json = queryMapper.toJson(ngsildquery, true);
            json.put(JSON_INSTANCEID, instanceId);
            LOGGER.debug("Info: IUDX temporal json query;" + json);
            /* HTTP request body as Json */
            JsonObject requestBody = new JsonObject();
            requestBody.put("ids", json.getJsonArray("id"));
            Future<List<String>> filtersFuture =
                catalogueService.getApplicableFilters(json.getJsonArray("id").getString(0));
            filtersFuture.onComplete(
                filtersHandler -> {
                  if (filtersHandler.succeeded()) {
                    json.put("applicableFilters", filtersHandler.result());
                    if (json.containsKey(IUDXQUERY_OPTIONS)
                        && JSON_COUNT.equalsIgnoreCase(json.getString(IUDXQUERY_OPTIONS))) {
                      executeCountQuery(routingContext, json, response);
                    } else {
                      executeSearchQuery(routingContext, json, response);
                    }
                  } else {
                    LOGGER.error("catalogue item/group doesn't have filters.");
                    handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN, "faiuled");
                  }
                });
          } else if (validationHandler.failed()) {
            LOGGER.error("Fail: Bad request;");
            handleResponse(
                response, BAD_REQUEST, INVALID_PARAM_URN, validationHandler.cause().getMessage());
          }
        });
  }

  /**
   * Method used to handle all subscription requests.
   *
   * @param routingContext routingContext
   */
  private void handleSubscriptions(RoutingContext routingContext) {
    LOGGER.trace("Info: handleSubscription method started");
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = routingContext.body().asJsonObject();
    String instanceId = request.getHeader(HEADER_HOST);
    String subscriptionType = SubsType.STREAMING.type;
    requestBody.put(SUB_TYPE, subscriptionType);
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    String userId = jwtData.getSub();

    JsonObject jsonObj = requestBody.copy();
    jsonObj.put(USER_ID, userId);
    jsonObj.put(JSON_INSTANCEID, instanceId);
    String entities = jsonObj.getJsonArray("entities").getString(0);
    JsonObject cacheJson = new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);
    HttpServerResponse response = routingContext.response();
    cacheService
        .get(cacheJson)
        .onSuccess(
            cacheServiceResult -> {
              Set<String> type =
                  new HashSet<String>(cacheServiceResult.getJsonArray("type").getList());
              Set<String> itemTypeSet =
                  type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
              itemTypeSet.retainAll(ITEM_TYPES);

              String resourceGroup;
              if (!itemTypeSet.contains("Resource")) {
                resourceGroup = cacheServiceResult.getString("id");
              } else {
                resourceGroup = cacheServiceResult.getString("resourceGroup");
              }
              jsonObj.put("type", itemTypeSet.iterator().next());
              jsonObj.put("resourcegroup", resourceGroup);

              Future<JsonObject> subsReq =
                  subsService.createSubscription(
                      jsonObj, databroker, postgresService, jwtData, cacheService);
              subsReq.onComplete(
                  subHandler -> {
                    if (subHandler.succeeded()) {
                      LOGGER.info("Success: Handle Subscription request;");
                      routingContext.data().put(RESPONSE_SIZE, 0);
                      Future.future(fu -> updateAuditTable(routingContext));
                      handleSuccessResponse(
                          response, ResponseType.Created.getCode(), subHandler.result().toString());
                    } else {
                      LOGGER.error("Fail: Handle Subscription request;");
                      processBackendResponse(response, subHandler.cause().getMessage());
                    }
                  });
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure.getMessage());
              processBackendResponse(response, failure.getMessage());
            });
  }

  /**
   * handle append requests for subscription.
   *
   * @param routingContext routingContext
   */
  private void appendSubscription(RoutingContext routingContext) {
    LOGGER.trace("Info: appendSubscription method started");
    HttpServerRequest request = routingContext.request();
    String userid = request.getParam(USER_ID);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = userid + "/" + alias;
    JsonObject requestJson = routingContext.body().asJsonObject();
    String instanceId = request.getHeader(HEADER_HOST);
    requestJson.put(SUBSCRIPTION_ID, subsId);
    requestJson.put(JSON_INSTANCEID, instanceId);
    String subscriptionType = SubsType.STREAMING.type;
    requestJson.put(SUB_TYPE, subscriptionType);
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    String userId = jwtData.getSub();
    HttpServerResponse response = routingContext.response();
    String entities = requestJson.getJsonArray("entities").getString(0);
    JsonObject cacheJson = new JsonObject().put("key", entities).put("type", CATALOGUE_CACHE);

    getCacheItem(cacheJson)
        .onSuccess(
            handler -> {
              requestJson.mergeIn(handler);
              if (requestJson.getString(JSON_NAME).equalsIgnoreCase(alias)) {
                JsonObject jsonObj = requestJson.copy();
                jsonObj.put(USER_ID, userId);
                Future<JsonObject> subsReq =
                    subsService.appendSubscription(
                        jsonObj, databroker, postgresService, jwtData, cacheService);
                subsReq.onComplete(
                    subsRequestHandler -> {
                      if (subsRequestHandler.succeeded()) {
                        LOGGER.debug("Success: Appending subscription");
                        routingContext.data().put(RESPONSE_SIZE, 0);

                        /*JsonObject message =
                        new JsonObject()
                            .put("resource", jsonObj.getJsonArray(ENTITIES).getString(0))
                            .put(SUBSCRIPTION_ID, jsonObj.getString(SUBSCRIPTION_ID))
                            .put(SUB_TYPE, jsonObj.getString(SUB_TYPE))
                            .put(USER_ID, jsonObj.getString(USER_ID))
                            .put(SUBSCRIPTION_ID, subsId)
                            .put(JSON_EVENT_TYPE, EVENTTYPE_APPEND);*/

                        Future.future(fu -> updateAuditTable(routingContext));
                        handleSuccessResponse(
                            response,
                            ResponseType.Created.getCode(),
                            subsRequestHandler.result().toString());
                      } else {
                        LOGGER.error("Fail: Appending subscription");
                        processBackendResponse(response, subsRequestHandler.cause().getMessage());
                      }
                    });
              } else {
                LOGGER.error("Fail: Bad request");
                handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN, MSG_INVALID_NAME);
              }
            })
        .onFailure(
            fail -> {
              LOGGER.debug("fail");
            });
  }

  /**
   * handle update subscription requests.
   *
   * @param routingContext routingContext
   */
  private void updateSubscription(RoutingContext routingContext) {
    LOGGER.trace("Info: updateSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String userid = request.getParam(USER_ID);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = userid + "/" + alias;
    JsonObject requestJson = routingContext.body().asJsonObject();
    String instanceId = request.getHeader(HEADER_HOST);
    String subscriptionType = SubsType.STREAMING.type;
    requestJson.put(SUB_TYPE, subscriptionType);
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    String userId = jwtData.getSub();
    if (requestJson.getString(JSON_NAME).equalsIgnoreCase(alias)) {
      JsonObject jsonObj = requestJson.copy();
      jsonObj.put(SUBSCRIPTION_ID, subsId);
      jsonObj.put(JSON_INSTANCEID, instanceId);
      jsonObj.put(USER_ID, userId);
      Future<JsonObject> subsReq =
          subsService.updateSubscription(jsonObj, databroker, postgresService, jwtData);
      subsReq.onComplete(
          subsRequestHandler -> {
            if (subsRequestHandler.succeeded()) {
              LOGGER.info("result : " + subsRequestHandler.result());
              routingContext.data().put(RESPONSE_SIZE, 0);
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(
                  response, ResponseType.Created.getCode(), subsRequestHandler.result().toString());
            } else {
              LOGGER.error("Fail: Bad request");
              processBackendResponse(response, subsRequestHandler.cause().getMessage());
            }
          });
    } else {
      LOGGER.error("Fail: Bad request");
      handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN, MSG_INVALID_NAME);
    }
  }

  /**
   * get a subscription by id.
   *
   * @param routingContext routingContext
   */
  private void getSubscription(RoutingContext routingContext) {
    LOGGER.trace("Info: getSubscription method started");
    HttpServerRequest request = routingContext.request();
    String domain = request.getParam(USER_ID);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = domain + "/" + alias;
    JsonObject requestJson = new JsonObject();
    String instanceId = request.getHeader(HEADER_HOST);
    requestJson.put(SUBSCRIPTION_ID, subsId);
    requestJson.put(JSON_INSTANCEID, instanceId);
    String subscriptionType = SubsType.STREAMING.type;
    requestJson.put(SUB_TYPE, subscriptionType);

    if (requestJson != null && requestJson.containsKey(SUB_TYPE)) {
      JsonObject jsonObj = requestJson.copy();
      /* TODO: JSON_CONSUMER is always null as it is not being set anywhere in the authentication */
      //      jsonObj.put(JSON_CONSUMER, authInfo.getString(JSON_CONSUMER));
      Future<JsonObject> entityName = getEntityName(requestJson);
      entityName
          .onSuccess(
              entityNameHandler -> {
                /*TODO: why is this entity object being added here */
                //                ((JsonObject) routingContext.data().get("authInfo"))
                //                    .put("id", entityNameHandler.getValue("id"));
                var entity = entityNameHandler.getValue("id");
                Future<JsonObject> subsReq =
                    subsService.getSubscription(jsonObj, databroker, postgresService, entity);
                subsReq.onComplete(
                    subHandler -> {
                      HttpServerResponse response = routingContext.response();
                      if (subHandler.succeeded()) {
                        LOGGER.info("Success: Getting subscription");
                        routingContext.data().put(RESPONSE_SIZE, 0);
                        Future.future(fu -> updateAuditTable(routingContext));
                        handleSuccessResponse(
                            response, ResponseType.Ok.getCode(), subHandler.result().toString());
                      } else {
                        LOGGER.error("Fail: Bad request");
                        processBackendResponse(response, subHandler.cause().getMessage());
                      }
                    });
              })
          .onFailure(
              entityNameFailureHandler -> {
                HttpServerResponse response = routingContext.response();
                LOGGER.error("Fail: Bad request from DB ");
                handleResponse(
                    response,
                    NOT_FOUND,
                    RESOURCE_NOT_FOUND_URN,
                    entityNameFailureHandler.getLocalizedMessage());
              });
    } else {
      HttpServerResponse response = routingContext.response();
      LOGGER.error("Fail: Bad request");
      handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN, MSG_SUB_TYPE_NOT_FOUND);
    }
  }

  /**
   * get a subscription for all queue without subscription id.
   *
   * @param routingContext routingContext
   */
  private void getAllSubscriptionForUser(RoutingContext routingContext) {
    LOGGER.trace("Info: getSubscriptionQueue method started");
    HttpServerResponse response = routingContext.response();
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    String userId = jwtData.getSub();
    JsonObject jsonObj = new JsonObject();
    jsonObj.put(USER_ID, userId);
    Future<JsonObject> subsReq =
        subsService.getAllSubscriptionQueueForUser(jsonObj, postgresService);
    subsReq.onComplete(
        subHandler -> {
          if (subHandler.succeeded()) {
            LOGGER.info("Success: Getting subscription queue");
            /* routingContext.data().put(RESPONSE_SIZE, 0);
            Future.future(fu -> updateAuditTable(routingContext));*/
            handleSuccessResponse(
                response, ResponseType.Ok.getCode(), subHandler.result().toString());
          } else {
            LOGGER.error("Fail: Bad request");
            processBackendResponse(response, subHandler.cause().getMessage());
          }
        });
  }

  /**
   * delete a subscription by id.
   *
   * @param routingContext routingContext
   */
  private void deleteSubscription(RoutingContext routingContext) {
    LOGGER.trace("Info: deleteSubscription method started;");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String userid = request.getParam(USER_ID);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = userid + "/" + alias;
    JsonObject requestJson = new JsonObject();
    String instanceId = request.getHeader(HEADER_HOST);
    requestJson.put(SUBSCRIPTION_ID, subsId);
    requestJson.put(JSON_INSTANCEID, instanceId);
    String subscriptionType = SubsType.STREAMING.type;
    requestJson.put(SUB_TYPE, subscriptionType);
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    String userId = jwtData.getSub();
    if (requestJson.containsKey(SUB_TYPE)) {
      JsonObject jsonObj = requestJson.copy();
      jsonObj.put(USER_ID, userId);
      Future<JsonObject> entityName = getEntityName(requestJson);
      entityName
          .onSuccess(
              entityNameSuccessHandler -> {
                /* TODO: why is the entity object being added in the authInfo */
                //                ((JsonObject) routingContext.data().get("authInfo"))
                //                    .put("id", entityNameSuccessHandler.getValue("id"));
                var entity = entityNameSuccessHandler.getValue("id");
                Future<JsonObject> subsReq =
                    subsService.deleteSubscription(jsonObj, databroker, postgresService, entity);
                subsReq.onComplete(
                    subHandler -> {
                      if (subHandler.succeeded()) {
                        routingContext.data().put(RESPONSE_SIZE, 0);
                        Future.future(fu -> updateAuditTable(routingContext));
                        handleSuccessResponse(
                            response, ResponseType.Ok.getCode(), subHandler.result().toString());
                      } else {
                        processBackendResponse(response, subHandler.cause().getMessage());
                      }
                    });
              })
          .onFailure(
              entityNameFailureHandler -> {
                LOGGER.error("ERROR: bad request from DB");
                handleResponse(
                    response,
                    NOT_FOUND,
                    RESOURCE_NOT_FOUND_URN,
                    entityNameFailureHandler.getLocalizedMessage());
              });

    } else {
      handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN, MSG_SUB_TYPE_NOT_FOUND);
    }
  }

  /**
   * register an adapter in Rabbit MQ.
   *
   * @param routingContext routingContext
   */
  private void registerAdapter(RoutingContext routingContext) {
    LOGGER.trace("Info: registerAdapter method started;");
    JsonObject requestJson = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceId = request.getHeader(HEADER_HOST);
    requestJson.put(JSON_INSTANCEID, instanceId);
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    String userId = jwtData.getSub();
    requestJson.put(USER_ID, userId);

    Future<JsonObject> brokerResult =
        managementApi.registerAdapter(requestJson, databroker, cacheService, postgresService);

    brokerResult.onComplete(
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Registering adapter");
            routingContext.data().put(RESPONSE_SIZE, 0);
            Future.future(fu -> updateAuditTable(routingContext));
            handleSuccessResponse(
                response, ResponseType.Created.getCode(), handler.result().toString());
          } else if (brokerResult.failed()) {
            LOGGER.error("Fail: Bad request" + handler.cause().getMessage());
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  /**
   * delete a adapter in Rabbit MQ.
   *
   * @param routingContext routingContext
   */
  public void deleteAdapter(RoutingContext routingContext) {
    LOGGER.trace("Info: deleteAdapter method starts;");

    Map<String, String> pathParams = routingContext.pathParams();
    String id = pathParams.get("*");

    StringBuilder adapterIdBuilder = new StringBuilder();
    adapterIdBuilder.append(id);
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    String userId = jwtData.getSub();
    Future<JsonObject> brokerResult =
        managementApi.deleteAdapter(
            adapterIdBuilder.toString(), userId, databroker, postgresService);
    HttpServerResponse response = routingContext.response();
    brokerResult.onComplete(
        brokerResultHandler -> {
          if (brokerResultHandler.succeeded()) {
            LOGGER.info("Success: Deleting adapter");
            routingContext.data().put(RESPONSE_SIZE, 0);
            Future.future(fu -> updateAuditTable(routingContext));
            handleSuccessResponse(
                response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
          } else {
            LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
            processBackendResponse(response, brokerResultHandler.cause().getMessage());
          }
        });
  }

  /**
   * get Adapter details from Rabbit MQ.
   *
   * @param routingContext routingContext
   */
  public void getAdapterDetails(RoutingContext routingContext) {
    LOGGER.trace("getAdapterDetails method starts");

    Map<String, String> pathParams = routingContext.pathParams();
    String id = pathParams.get("UUID");
    Future<JsonObject> brokerResult = managementApi.getAdapterDetails(id, databroker);
    HttpServerResponse response = routingContext.response();
    brokerResult.onComplete(
        brokerResultHandler -> {
          if (brokerResultHandler.succeeded()) {
            routingContext.data().put(RESPONSE_SIZE, 0);
            Future.future(fu -> updateAuditTable(routingContext));
            handleSuccessResponse(
                response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
          } else {
            processBackendResponse(response, brokerResultHandler.cause().getMessage());
          }
        });
  }

  /**
   * publish heartbeat details to Rabbit MQ.
   *
   * @param routingContext routingContext Note: This is too frequent an operation to have info or
   *     error level logs
   */
  public void publishHeartbeat(RoutingContext routingContext) {
    LOGGER.trace("Info: publishHeartbeat method starts;");
    JsonObject requestJson = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceId = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/iudx/v1/adapter");
    requestJson.put(JSON_INSTANCEID, instanceId);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<JsonObject> brokerResult = managementApi.publishHeartbeat(requestJson, databroker);
      brokerResult.onComplete(
          brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: Published heartbeat");
              routingContext.data().put(RESPONSE_SIZE, 0);
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
            } else {
              LOGGER.debug("Fail: Unauthorized;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });

    } else {
      LOGGER.info("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  Future<Boolean> validateProviderUser(String providerUserId, JwtData jwtData) {
    LOGGER.trace("validateProviderUser() started");
    Promise<Boolean> promise = Promise.promise();
    try {
      if (jwtData.getRole().equalsIgnoreCase("delegate")) {
        if (jwtData.getDid().equalsIgnoreCase(providerUserId)) {
          LOGGER.info("success");
          promise.complete(true);
        } else {
          LOGGER.error("fail");
          promise.fail("incorrect providerUserId");
        }
      } else if (jwtData.getRole().equalsIgnoreCase("provider")) {
        if (jwtData.getSub().equalsIgnoreCase(providerUserId)) {
          LOGGER.info("success");
          promise.complete(true);
        } else {
          LOGGER.error("fail");
          promise.fail("incorrect providerUserId");
        }
      } else {
        LOGGER.error("fail");
        promise.fail("invalid role");
      }
    } catch (Exception e) {
      LOGGER.error("exception occurred while validating provider user : " + e.getMessage());
      promise.fail("exception occurred while validating provider user");
    }
    return promise.future();
  }

  Future<Boolean> isValidId(JwtData jwtData, String id) {
    Promise<Boolean> promise = Promise.promise();
    String jwtId = jwtData.getIid().split(":")[1];
    if (id.equalsIgnoreCase(jwtId)) {
      promise.complete(true);
    } else {
      LOGGER.error("Incorrect id value in jwt");
      promise.fail("Incorrect id value in jwt");
    }

    return promise.future();
  }

  /**
   * publish data from adapter to rabbit MQ.
   *
   * @param routingContext routingContext Note: All logs are debug level
   */
  public void publishDataFromAdapter(RoutingContext routingContext) {
    LOGGER.trace("Info: publishDataFromAdapter method started;");
    JsonArray requestJson = routingContext.body().asJsonArray();
    HttpServerResponse response = routingContext.response();
    Future<JsonObject> brokerResult = managementApi.publishDataFromAdapter(requestJson, databroker);
    brokerResult.onComplete(
        brokerResultHandler -> {
          if (brokerResultHandler.succeeded()) {
            LOGGER.debug("Success: publishing data from adapter");
            routingContext.data().put(RESPONSE_SIZE, 0);
            Future.future(fu -> updateAuditTable(routingContext));
            handleSuccessResponse(
                response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
          } else {
            LOGGER.debug("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
            processBackendResponse(response, brokerResultHandler.cause().getMessage());
          }
        });
  }

  private void getAllAdaptersForUsers(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JwtData jwtData = RoutingContextHelper.getJwtData(routingContext);
    String iid = jwtData.getIid().split(":")[1];
    JsonObject jsonObj = new JsonObject();

    JsonObject cacheRequest = new JsonObject();
    cacheRequest.put("type", CATALOGUE_CACHE);
    cacheRequest.put("key", iid);

    cacheService
        .get(cacheRequest)
        .onComplete(
            relHandler -> {
              if (relHandler.succeeded()) {
                String providerId = relHandler.result().getString("provider");
                jsonObj.put(PROVIDER_ID, providerId);
                Future<JsonObject> allAdapterForUser =
                    managementApi.getAllAdapterDetailsForUser(jsonObj, postgresService);
                allAdapterForUser.onComplete(
                    handler -> {
                      if (handler.succeeded()) {
                        LOGGER.debug("Successful");
                        if (handler.result().getJsonArray("result").isEmpty()) {
                          handleSuccessResponse(
                              response,
                              ResponseType.NoContent.getCode(),
                              handler.result().toString());
                        } else {
                          /*routingContext.data().put(RESPONSE_SIZE, 0);
                          Future.future(fu -> updateAuditTable(routingContext));*/
                          handleSuccessResponse(
                              response, ResponseType.Ok.getCode(), handler.result().toString());
                        }
                      } else {
                        LOGGER.debug(handler.cause());
                        processBackendResponse(response, handler.cause().getMessage());
                      }
                    });

              } else {
                LOGGER.debug("Fail in get Adapters method and didn't get rel");
              }
            });
  }

  /**
   * handle HTTP response.
   *
   * @param response HttpServerResponse object
   * @param statusCode Http status code for response
   * @param result String of response
   */
  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  private void processBackendResponse(HttpServerResponse response, String failureMessage) {
    LOGGER.debug("Info : " + failureMessage);
    try {
      JsonObject json = new JsonObject(failureMessage);
      int type = json.getInteger(JSON_TYPE);
      HttpStatusCode status = HttpStatusCode.getByValue(type);
      String urnTitle = json.getString(JSON_TITLE);
      ResponseUrn urn;
      if (urnTitle != null) {
        urn = fromCode(urnTitle);
      } else {
        urn = fromCode(String.valueOf(type));
      }
      // return urn in body
      if (json.getString("details") != null) {
        handleResponse(response, status, urn, json.getString("details"));
        return;
      }
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(type)
          .end(generateResponse(status, urn).toString());
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      handleResponse(response, BAD_REQUEST, BACKING_SERVICE_FORMAT_URN);
    }
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode code, ResponseUrn urn) {
    handleResponse(response, code, urn, code.getDescription());
  }

  private void handleResponse(
      HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn, String message) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, message).toString());
  }

  /**
   * Get the request query parameters delimited by <b>&</b>, <i><b>;</b>(semicolon) is considered as
   * part of the parameter</i>.
   *
   * @param routingContext RoutingContext Object
   * @param response HttpServerResponse
   * @return Optional Optional of Map
   */
  private Optional<MultiMap> getQueryParams(
      RoutingContext routingContext, HttpServerResponse response) {
    MultiMap queryParams = null;
    try {
      queryParams = MultiMap.caseInsensitiveMultiMap();
      // Internally + sign is dropped and treated as space, replacing + with %2B do the trick
      String uri = routingContext.request().uri().replaceAll("\\+", "%2B");
      Map<String, List<String>> decodedParams =
          new QueryStringDecoder(uri, HttpConstants.DEFAULT_CHARSET, true, 1024, true).parameters();
      for (Map.Entry<String, List<String>> entry : decodedParams.entrySet()) {
        LOGGER.debug("Info: param :" + entry.getKey() + " value : " + entry.getValue());
        queryParams.add(entry.getKey(), entry.getValue());
      }
    } catch (IllegalArgumentException ex) {
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(BAD_REQUEST.getValue())
          .end(generateResponse(BAD_REQUEST, INVALID_PARAM_URN).toString());
    }
    return Optional.of(queryParams);
  }

  @Override
  public void stop() {
    LOGGER.info("Stopping the API server");
  }

  private boolean isTemporalParamsPresent(NgsildQueryParams ngsildquery) {
    return ngsildquery.getTemporalRelation().getTemprel() != null
        || ngsildquery.getTemporalRelation().getTime() != null
        || ngsildquery.getTemporalRelation().getEndTime() != null;
  }

  private Future<Void> updateAuditTable(RoutingContext context) {
    JwtData jwtData = RoutingContextHelper.getJwtData(context);
    String endPoint = RoutingContextHelper.getEndPoint(context);
    String id = RoutingContextHelper.getId(context);

    Promise<Void> promise = Promise.promise();
    JsonObject request = new JsonObject();
    JsonObject cacheRequest = new JsonObject();
    cacheRequest.put("type", CATALOGUE_CACHE);
    cacheRequest.put("key", id);
    cacheService
        .get(cacheRequest)
        .onComplete(
            relHandler -> {
              if (relHandler.succeeded()) {
                JsonObject cacheResult = relHandler.result();

                String resourceGroup =
                    cacheResult.containsKey(RESOURCE_GROUP)
                        ? cacheResult.getString(RESOURCE_GROUP)
                        : cacheResult.getString(ID);
                ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
                String role = jwtData.getRole();
                String drl = jwtData.getDrl();
                if (role.equalsIgnoreCase("delegate") && drl != null) {
                  request.put(DELEGATOR_ID, jwtData.getDid());
                } else {
                  request.put(DELEGATOR_ID, jwtData.getSub());
                }
                if (endPoint.contains(api.getSubscriptionUrl())) {
                  request.put(EVENT, "subscriptions");
                }
                String type =
                    cacheResult.containsKey(RESOURCE_GROUP) ? "RESOURCE" : "RESOURCE_GROUP";
                long time = zst.toInstant().toEpochMilli();
                String providerId = cacheResult.getString("provider");
                String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();
                request.put(RESOURCE_GROUP, resourceGroup);
                request.put(TYPE_KEY, type);
                request.put(EPOCH_TIME, time);
                request.put(ISO_TIME, isoTime);
                request.put(USER_ID, jwtData.getSub());
                request.put(ID, id);
                request.put(API, endPoint);
                request.put(RESPONSE_SIZE, context.data().get(RESPONSE_SIZE));
                request.put(PROVIDER_ID, providerId);
                meteringService.insertMeteringValuesInRmq(
                    request,
                    handler -> {
                      if (handler.succeeded()) {
                        LOGGER.info("message published in RMQ.");
                        promise.complete();
                      } else {
                        LOGGER.error("failed to publish message in RMQ.");
                        promise.complete();
                      }
                    });
              } else {
                LOGGER.debug("Item not found and failed to call metering service");
              }
            });

    return promise.future();
  }

  private Future<JsonObject> getEntityName(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    String getEntityNameQuery = ENTITY_QUERY.replace("$0", request.getString(SUBSCRIPTION_ID));
    postgresService.executeQuery(
        getEntityNameQuery,
        pgHandler -> {
          if (pgHandler.succeeded() && !pgHandler.result().getJsonArray("result").isEmpty()) {
            request.put(
                "id",
                pgHandler.result().getJsonArray("result").getJsonObject(0).getString("entity"));
            promise.complete(request);
          } else {
            if (pgHandler.result().getJsonArray("result").isEmpty()) {
              LOGGER.error("Empty response from database.");
              promise.fail("Resource Not Found");
            } else {
              LOGGER.error("fail here");
              promise.fail(pgHandler.cause().getMessage());
            }
          }
        });
    return promise.future();
  }

  private Future<JsonObject> getCacheItem(JsonObject cacheJson) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject cacheResult = new JsonObject();
    cacheService
        .get(cacheJson)
        .onSuccess(
            cacheServiceResult -> {
              Set<String> type =
                  new HashSet<String>(cacheServiceResult.getJsonArray("type").getList());
              Set<String> itemTypeSet =
                  type.stream().map(e -> e.split(":")[1]).collect(Collectors.toSet());
              itemTypeSet.retainAll(ITEM_TYPES);
              String resourceGroup;
              if (!itemTypeSet.contains("Resource")) {
                resourceGroup = cacheServiceResult.getString("id");
              } else {
                resourceGroup = cacheServiceResult.getString("resourceGroup");
              }
              cacheResult.put("type", itemTypeSet.iterator().next());
              cacheResult.put("resourcegroup", resourceGroup);
              promise.complete(cacheResult);
            })
        .onFailure(
            fail -> {
              LOGGER.debug("Failed");
              promise.fail(fail.getMessage());
            });

    return promise.future();
  }
}
