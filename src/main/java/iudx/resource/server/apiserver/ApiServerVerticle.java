package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.HttpStatusCode.NOT_FOUND;
import static iudx.resource.server.common.ResponseUrn.YET_NOT_IMPLEMENTED_URN;
import static iudx.resource.server.common.ResponseUtil.generateResponse;
import static iudx.resource.server.common.Util.errorResponse;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import iudx.resource.server.apiserver.admin.controller.AdminController;
import iudx.resource.server.apiserver.async.controller.AsyncController;
import iudx.resource.server.apiserver.ingestion.controller.IngestionController;
import iudx.resource.server.apiserver.metering.controller.MeteringController;
import iudx.resource.server.apiserver.search.controller.SearchController;
import iudx.resource.server.apiserver.subscription.controller.SubscriptionController;
import iudx.resource.server.apiserver.usermanagement.controller.UserManagementController;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.HttpStatusCode;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ApiServerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);
  Router router;
  private HttpServer server;
  private String keystore;
  private String keystorePassword;
  private boolean isssl;
  private int port;
  private String dxApiBasePath;
  private Api api;

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
    router = Router.router(vertx);

    /* Get base paths from config */
    dxApiBasePath = config().getString("dxApiBasePath");
    api = Api.getInstance(dxApiBasePath);

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
                    // TODO:: Need to add responses
                    response
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(code.getValue())
                        .end(errorResponse(code));
                  });
            });

    router.route().handler(BodyHandler.create());
    router.route().handler(TimeoutHandler.create(50000, 408));

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
      serverOptions
          .setSsl(true)
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));
      LOGGER.info("Info: Starting HTTPs server at port" + port);

    } else {
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

    new SubscriptionController(vertx, router, api, config()).init();
    new SearchController(vertx, router, api, config()).init();
    new AdminController(vertx, router, api).init();
    new UserManagementController(router, vertx, api, config()).init();
    new AsyncController(vertx, router, api, config()).init();
    new IngestionController(vertx, router, api, config()).init();
    new MeteringController(vertx, router, api, config()).init();

    router
        .route()
        .last()
        .handler(
            requestHandler -> {
              HttpServerResponse response = requestHandler.response();
              LOGGER.warn("⚠️ Last route handling request for: " + requestHandler.request().path());
              response
                  .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                  .setStatusCode(404)
                  .end(generateResponse(NOT_FOUND, YET_NOT_IMPLEMENTED_URN).toString());
            });

    /* Print the deployed endpoints */
    printDeployedEndpoints(router);
    LOGGER.info("API server deployed on :" + serverOptions.getPort());
  }

  private void printDeployedEndpoints(Router router) {
    for (Route route : router.getRoutes()) {
      if (route.getPath() != null) {
        LOGGER.info("API Endpoints deployed : " + route.methods() + " : " + route.getPath());
      }
    }
  }

  @Override
  public void stop() throws Exception {
    LOGGER.info("Stopping the API server");
  }
}
