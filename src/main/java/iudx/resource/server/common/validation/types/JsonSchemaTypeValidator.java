package iudx.resource.server.common.validation.types;

import static iudx.resource.server.common.ResponseUrn.*;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.ValidationException;
import iudx.resource.server.apiserver.exception.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class JsonSchemaTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(JsonSchemaTypeValidator.class);

  private final JsonObject value;
  private final Schema schema;

  public JsonSchemaTypeValidator(final JsonObject value, final Schema schema) {
    this.value = value;
    this.schema = schema;
  }

  @Override
  public boolean isValid() {
    try {
      schema.validateSync(value);
    } catch (ValidationException e) {
      LOGGER.error("Validation error :" + e.getMessage());
      throw new DxRuntimeException(
          failureCode(), INVALID_PAYLOAD_FORMAT_URN, failureMessage(value.toString()));
    } catch (NoSyncValidationException e) {
      LOGGER.error("Validation error :" + e.getMessage());
      throw new DxRuntimeException(
          failureCode(), INVALID_PAYLOAD_FORMAT_URN, failureMessage(value.toString()));
    }
    return true;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_PAYLOAD_FORMAT_URN.getMessage();
  }
}
