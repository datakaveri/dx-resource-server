package org.cdpg.dx.common.util;

import com.hazelcast.config.properties.ValidationException;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.ParameterProcessorException;
import io.vertx.ext.web.validation.RequestPredicateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.HttpStatusCode;
import org.cdpg.dx.common.exception.*;
import org.cdpg.dx.common.exception.InvalidColumnNameException;

public class ExceptionHttpStatusMapper {
  private static final Logger LOGGER = LogManager.getLogger(ExceptionHttpStatusMapper.class);

  public static HttpStatusCode map(Throwable throwable) {
    return switch (throwable) {
      case NoRowFoundException e -> {
        LOGGER.debug("Matched: NoRowFoundException");
        yield HttpStatusCode.NOT_FOUND;
      }
      case InvalidColumnNameException e -> {
        LOGGER.debug("Matched: InvalidColumnNameException");
        yield HttpStatusCode.BAD_REQUEST;
      }
      case UniqueConstraintViolationException e -> {
        LOGGER.debug("Matched: UniqueConstraintViolationException");
        yield HttpStatusCode.CONFLICT;
      }
        case DxPgException e -> {
          LOGGER.debug("Matched: DxPgException");
          yield HttpStatusCode.BAD_REQUEST;
        }
      // Subscription related exceptions
      case QueueAlreadyExistsException e -> {
        LOGGER.debug("Matched: QueueAlreadyExistsException");
        yield HttpStatusCode.CONFLICT;
      }

        case ExchangeRegistrationException e ->{
          LOGGER.debug("Matched: ExchangeRegistrationException");
          yield HttpStatusCode.CONFLICT;
        }
        case DxSubscriptionException e -> {
        LOGGER.debug("Matched: DxSubscriptionException");
        yield HttpStatusCode.BAD_REQUEST;
      }
      case ExchangeNotFoundException e ->{
        LOGGER.debug("Matched: ExchangeNotFoundException");
        yield HttpStatusCode.NOT_FOUND;
      }
        case QueueRegistrationFailedException e -> {
        LOGGER.debug("Matched: QueueRegistrationFailedException");
        yield HttpStatusCode.BAD_REQUEST;
      }
      case QueueBindingFailedException e -> {
        LOGGER.debug("Matched: QueueBindingFailedException");
        yield HttpStatusCode.BAD_REQUEST;
      }
      case QueueDeletionException e -> {
        LOGGER.debug("Matched: QueueDeletionException");
        yield HttpStatusCode.NOT_FOUND;
      }
      case QueueNotFoundException e -> {
        LOGGER.debug("Matched: QueueNotFoundException");
        yield HttpStatusCode.NOT_FOUND;
      }
      case DxRabbitMqGeneralException e -> {
        LOGGER.debug("Matched: DxRabbitMqGeneralException");
        yield HttpStatusCode.INTERNAL_SERVER_ERROR;
      }
      case DxRabbitMqException e -> {
        LOGGER.debug("Matched: DxRabbitMqException");
        yield HttpStatusCode.BAD_REQUEST;
      }

      // search exception
      case SearchValidationError e -> {
        LOGGER.debug("Matched: SearchValidationError");
        yield HttpStatusCode.INVALID_PARAM;
      }
        // Redis Exceptions
      case RedisKeyNotFoundException e -> {
        LOGGER.debug("Matched: RedisKeyNotFoundException");
        yield HttpStatusCode.NOT_FOUND;
      }
      case RedisConnectionException e -> {
        LOGGER.debug("Matched: RedisConnectionException");
        yield HttpStatusCode.SERVICE_UNAVAILABLE;
      }
      case InvalidJsonPathException e -> {
        LOGGER.debug("Matched: InvalidJsonPathException");
        yield HttpStatusCode.BAD_REQUEST;
      }
      case RedisOperationException e -> {
        LOGGER.debug("Matched: RedisOperationException");
        yield HttpStatusCode.BAD_REQUEST;
      }
      case DxNotFoundException e->{
        LOGGER.debug("Matched: DxNotFoundException");
        yield HttpStatusCode.NOT_FOUND;
      }
      case DxAuthException e->{
        LOGGER.debug("Matched: DxAuthException");
        yield HttpStatusCode.UNAUTHORIZED;
      }
      case DxBadRequestException e->{
        LOGGER.debug("Matched: DxBadRequestException");
        yield HttpStatusCode.BAD_REQUEST;
      }
      case BodyProcessorException e->{
        LOGGER.debug("Matched: BodyProcessorException");
        yield HttpStatusCode.BAD_REQUEST;
      }
      case ParameterProcessorException e->{
        LOGGER.debug("Matched: ParameterProcessorException");
        yield HttpStatusCode.BAD_REQUEST;
      }case RequestPredicateException e->{
        LOGGER.debug("Matched: RequestPredicateException");
        yield HttpStatusCode.BAD_REQUEST;
      }case ValidationException e->{
        LOGGER.debug("Matched: ValidationException");
        yield HttpStatusCode.BAD_REQUEST;
      }


      case DxInternalServerErrorException e->{
        LOGGER.debug("Matched: DxInternalServerErrorException");
        yield HttpStatusCode.INTERNAL_SERVER_ERROR;
      }
      case BaseDxException e -> {
        LOGGER.debug("Matched: BaseDxException");
        yield HttpStatusCode.BAD_REQUEST;
      }
        default -> {
        LOGGER.debug("Matched: default (unhandled exception type: {})", throwable.getClass().getSimpleName());
        yield HttpStatusCode.INTERNAL_SERVER_ERROR;
      }
    };
  }
}
