package org.cdpg.dx.rs.apiserver;

import static org.cdpg.dx.util.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.TimeoutHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import io.vertx.serviceproxy.HelperUtils;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.auth.authentication.client.SecretKeyClientImpl;
import org.cdpg.dx.auth.authentication.handler.TokenAuthenticationHandler;
import org.cdpg.dx.common.FailureHandler;
import org.cdpg.dx.common.HttpStatusCode;

public class ApiServerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);
  private int port;
  private HttpServer server;
  private Router router;

  public static String errorResponse(HttpStatusCode code) {
    return new JsonObject()
        .put("type", code.getUrn())
        .put("title", code.getDescription())
        .put("detail", code.getDescription())
        .toString();
  }

  @Override
  public void start() {
    port = config().getInteger("httpPort", 8443);
    String dxApiBasePath = config().getString("dxApiBasePath");
    boolean isTimeLimitEnabled = config().getBoolean("isTimeLimitEnabled", false);

    SecretKeyClientImpl secretKeyClient = new SecretKeyClientImpl(config(), vertx);
    TokenAuthenticationHandler authenticatorHandler =
        new TokenAuthenticationHandler(config(), secretKeyClient, vertx);

    List<ApiController> controllers = ControllerFactory.createControllers(vertx, config());

    RouterBuilder.create(vertx, "docs/openapi.yaml")
        .onSuccess(
            routerBuilder -> {
              try {

                LOGGER.debug("Adding platform handlers...");
                int timeout = config().getInteger("timeout", 100000); // Configurable timeout
                routerBuilder.rootHandler(TimeoutHandler.create(timeout, 408));
                routerBuilder.rootHandler(BodyHandler.create().setHandleFileUploads(false));

                LOGGER.debug("Registering controllers...");
                RouterBuilderOptions factoryOptions =
                    new RouterBuilderOptions().setMountResponseContentTypeHandler(true);
                routerBuilder.setOptions(factoryOptions);
                routerBuilder.securityHandler("authorization", authenticatorHandler);

                controllers.forEach(controller -> controller.register(routerBuilder));

                LOGGER.debug("Creating router...");
                router = routerBuilder.createRouter();

                LOGGER.debug("Configuring CORS and error handlers...");
                configureCorsHandler(routerBuilder);
                putCommonResponseHeaders();
                configureErrorHandlers(router);
                configureFailureHandler(router);
                LOGGER.debug("Starting HTTP server...");
                HttpServerOptions serverOptions = new HttpServerOptions();

                /* Documentation routes */
                router
                    .get(ROUTE_STATIC_SPEC)
                    .produces(APPLICATION_JSON)
                    .handler(
                        routingContext -> {
                          HttpServerResponse response = routingContext.response();
                          response.sendFile("docs/openapi.yaml");
                        });
                router
                    .get(ROUTE_DOC)
                    .produces("text/html")
                    .handler(
                        routingContext -> {
                          HttpServerResponse response = routingContext.response();
                          response.sendFile("docs/apidoc.html");
                        });
                setServerOptions(serverOptions);
                server = vertx.createHttpServer(serverOptions);
                server
                    .requestHandler(router)
                    .listen(
                        port,
                        http -> {
                          if (http.succeeded()) {
                            printDeployedEndpoints(router);
                            LOGGER.info("ApiServerVerticle  deployed on port: {}", port);
                          } else {
                            LOGGER.error(
                                "HTTP server failed to start: {}",
                                http.cause().getMessage(),
                                http.cause());
                          }
                        });
              } catch (Exception e) {
                LOGGER.error(
                    "Error during router creation or server startup: {}", e.getMessage(), e);
              }
            })
        .onFailure(
            failure -> {
              LOGGER.error(
                  "Failed to create RouterBuilder from OpenAPI spec: {}",
                  failure.getMessage(),
                  failure);
            });
  }

  private void configureCorsHandler(RouterBuilder routerBuilder) {
    routerBuilder.rootHandler(
        CorsHandler.create().allowedHeaders(ALLOWED_HEADERS).allowedMethods(ALLOWED_METHODS));
  }

  private void putCommonResponseHeaders() {
    router
        .route()
        .handler(
            ctx -> {
              ctx.response()
                  .putHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0")
                  .putHeader("Pragma", "no-cache")
                  .putHeader("Expires", "0")
                  .putHeader("X-Content-Type-Options", "nosniff");
              ctx.next();
            });
  }

  private void configureErrorHandlers(Router router) {
    for (HttpStatusCode code : HttpStatusCode.values()) {
      router.errorHandler(
          code.getValue(),
          ctx -> {
            HttpServerResponse response = ctx.response();
            if (response.headWritten()) {
              try {
                response.reset();
              } catch (RuntimeException e) {
                LOGGER.error(
                    "Failed to reset response: {}", HelperUtils.convertStackTrace(e).encode());
              }
              return;
            }
            response
                .setStatusCode(code.getValue())
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .end(errorResponse(code));
          });
    }
  }

  private void setServerOptions(HttpServerOptions serverOptions) {
    boolean isSsl = config().getBoolean("ssl");
    LOGGER.info("isSsl : {}", isSsl);
    if (isSsl) {
      LOGGER.info("Info: Starting HTTPs server");
      validateConfig();
      String keystore = config().getString("keystore");
      String keystorePassword = config().getString("keystorePassword");
      serverOptions
          .setSsl(true)
          .setKeyCertOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));
    } else {
      LOGGER.info("Info: Starting HTTP server");
      serverOptions.setSsl(false);
    }
  }

  private void printDeployedEndpoints(Router router) {
    for (Route route : router.getRoutes()) {
      if (route.getPath() != null) {
        LOGGER.info("Deployed endpoint [{}] {}", route.methods(), route.getPath());
      }
    }
  }

  private void validateConfig() {
    if (config().getString("keystore") == null || config().getString("keystorePassword") == null) {
      throw new IllegalArgumentException("Keystore and password must be configured for SSL.");
    }
  }
    private void configureFailureHandler(Router router) {
        router.route().failureHandler(FailureHandler::handle);
    }
  @Override
  public void stop() {
    if (server != null) {
      server.close(
          ar -> {
            if (ar.succeeded()) {
              LOGGER.info("HTTP server stopped successfully.");
            } else {
              LOGGER.error("Failed to stop HTTP server: {}", ar.cause().getMessage());
            }
          });
    }
  }
}
