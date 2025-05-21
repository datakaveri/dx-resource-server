package org.cdpg.dx.common.util;

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

        case DxSubscriptionException e -> {
        LOGGER.debug("Matched: DxSubscriptionException");
        yield HttpStatusCode.BAD_REQUEST;
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
      case DxRabbitMqException e -> {
        LOGGER.debug("Matched: DxRabbitMqException");
        yield HttpStatusCode.BAD_REQUEST;
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
