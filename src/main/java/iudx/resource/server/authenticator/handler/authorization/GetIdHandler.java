package iudx.resource.server.authenticator.handler.authorization;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.*;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.RoutingContextHelper;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetIdHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(GetIdHandler.class);
  private Api api;

  public GetIdHandler(Api apis) {
    api = apis;
  }

  /**
   * @param context
   */
  @Override
  public void handle(RoutingContext context) {
    LOGGER.debug("Info : path {}", RoutingContextHelper.getRequestPath(context));
    context.next();
  }

  public Handler<RoutingContext> withNormalisedPath(String path) {
    return context -> handleWithConstraints(context, path);
  }

  private void handleWithConstraints(RoutingContext context, String normalisedPath) {
    /*Set endpoint in the routingContextHelper */
    RoutingContextHelper.setEndPoint(context, normalisedPath);

    RequestBody requestBody = context.body();
    if (RoutingContextHelper.getRequestPath(context).contains(api.getIngestionPathEntities())) {

      try {
        JsonArray requestJsonArray = requestBody.asJsonArray();
        Set<String> entityIds = new HashSet<>();

        for (int i = 0; i < requestJsonArray.size(); i++) {
          JsonObject entity = requestJsonArray.getJsonObject(i);
          entityIds.add(entity.getString("entities"));
        }

        if (entityIds.size() == 1) {
          LOGGER.debug("All entity IDs match: " + entityIds.iterator().next());
        } else {
          LOGGER.error("Entity IDs do not match: " + entityIds);
          processAuthFailure(context, "Entity IDs do not match");
          return;
        }
      } catch (Exception e) {
        processAuthFailure(context, "Error processing the request body");
      }
    }

    final String path = normalisedPath;
    final String method = RoutingContextHelper.getMethod(context);

    LOGGER.debug("Info :" + context.request().path());
    String id = getId(context, path, method);
    RoutingContextHelper.setId(context, id);
    context.next();
  }

  /**
   * extract id from request (path/query or body )
   *
   * @param context current routing context
   * @param path endpoint called for
   * @return id extracted from path if present
   */
  private String getId(RoutingContext context, String path, String method) {

    String pathId = getId4rmPath(context);
    String paramId = getId4rmRequest(context);
    String bodyId = getId4rmBody(context, path);
    String id;
    if (pathId != null && !pathId.isBlank()) {
      id = pathId;
    } else {
      if (paramId != null && !paramId.isBlank()) {
        id = paramId;
      } else {
        id = bodyId;
      }
    }
    if (path.matches(api.getSubscriptionUrl())
        && (!method.equalsIgnoreCase("GET") || !method.equalsIgnoreCase("DELETE"))) {
      id = bodyId;
    }
    return id;
  }

  private String getId4rmPath(RoutingContext context) {
    StringBuilder id = null;
    Map<String, String> pathParams = context.pathParams();
    LOGGER.debug("path params :" + pathParams);
    if (pathParams != null && !pathParams.isEmpty()) {
      if (pathParams.containsKey("*")) {
        // for /ingestion/{id} API that contains * =
        id = new StringBuilder(pathParams.get("*"));
        LOGGER.info(
            "API is : {} and path param is : {}",
            RoutingContextHelper.getRequestPath(context),
            pathParams);
        LOGGER.debug("id :" + id);
      } else if (pathParams.containsKey(USER_ID) && pathParams.containsKey(JSON_ALIAS)) {
        /* this part is not being used now */
        id = new StringBuilder();
        LOGGER.info(
            "User id and alias name are present : {}, {}", context.request().path(), pathParams);
        id.append(pathParams.get(USER_ID)).append("/").append(pathParams.get(JSON_ALIAS));
      }
    }
    return id != null ? id.toString() : null;
  }

  private String getId4rmRequest(RoutingContext routingContext) {
    return routingContext.request().getParam(ID);
  }

  private String getId4rmBody(RoutingContext context, String endpoint) {
    JsonObject body;
    if (endpoint != null && endpoint.equalsIgnoreCase(api.getIngestionPathEntities())) {
      body = context.body().asJsonArray().getJsonObject(0);
    } else {
      body = context.body().asJsonObject();
    }

    String id = null;
    if (body != null) {
      JsonArray array = body.getJsonArray(JSON_ENTITIES);
      if (array != null) {
        boolean isValidEndpoint = endpoint != null;
        boolean isIngestionOrSubscriptionApi =
            isValidEndpoint
                && (endpoint.contains(api.getIngestionPath())
                    || endpoint.contains(api.getSubscriptionUrl()));
        if (isIngestionOrSubscriptionApi) {
          id = array.getString(0); // only first UUID is is fetched in subscription
        } else {
          JsonObject json = array.getJsonObject(0);
          if (json != null) {
            id = json.getString(ID);
            /*for something like this : "entities": [
            {
            "id": "UUID"
            }
            ]*/
          }
        }
      }
    }
    return id;
  }

  private void processAuthFailure(RoutingContext ctx, String result) {
    if (result.contains("Not Found")) {
      LOGGER.error("Error : Item Not Found");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(404);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(RESOURCE_NOT_FOUND_URN, statusCode).toString());
    } else if (result.contains("Entity IDs do not match")
        || result.contains("Error processing the request body")) {
      LOGGER.error("Entity IDs do not match");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(400);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(BAD_REQUEST_URN, statusCode).toString());
    } else {
      LOGGER.error("Error : Authentication Failure");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(INVALID_TOKEN_URN, statusCode).toString());
    }
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }
}
