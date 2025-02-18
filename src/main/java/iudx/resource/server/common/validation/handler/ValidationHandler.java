package iudx.resource.server.common.validation.handler;

import static iudx.resource.server.apiserver.util.Constants.HEADER_PUBLIC_KEY;
import static iudx.resource.server.apiserver.util.Constants.HEADER_RESPONSE_FILE_FORMAT;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import iudx.resource.server.common.RequestType;
import iudx.resource.server.common.validation.ValidationHandlerFactory;
import iudx.resource.server.common.validation.types.Validator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ValidationHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(ValidationHandler.class);

  private RequestType requestType;
  private Vertx vertx;

  public ValidationHandler(Vertx vertx, RequestType apiRequestType) {
    this.vertx = vertx;
    this.requestType = apiRequestType;
  }

  @Override
  public void handle(RoutingContext context) {
    LOGGER.debug("inside validation");
    MultiMap parameters = context.request().params();

    RequestBody requestBody = context.body();
    JsonObject body = null;
    if (requestBody != null && requestBody.asJsonObject() != null) {
      body = requestBody.asJsonObject().copy();
    }
    Map<String, String> pathParams = context.pathParams();
    parameters.set(HEADER_PUBLIC_KEY, context.request().getHeader(HEADER_PUBLIC_KEY));
    parameters.set(
        HEADER_RESPONSE_FILE_FORMAT, context.request().getHeader(HEADER_RESPONSE_FILE_FORMAT));
    parameters.addAll(pathParams);
    ValidationHandlerFactory validationFactory = new ValidationHandlerFactory();
    MultiMap headers = context.request().headers();
    List<Validator> validations =
        validationFactory.build(vertx, requestType, parameters, headers, body);
    for (Validator validator : Optional.ofNullable(validations).orElse(Collections.emptyList())) {
      LOGGER.debug("validator :" + validator.getClass().getName());
      validator.isValid();
    }
    context.next();
  }
}
