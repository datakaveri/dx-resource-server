package org.cdpg.dx.rs.admin.service;

import static org.cdpg.dx.common.ErrorCode.ERROR_INTERNAL_SERVER;
import static org.cdpg.dx.common.ErrorMessage.INTERNAL_SERVER_ERROR;
import static org.cdpg.dx.rs.admin.util.Constants.TOKEN_INVALID_EX;
import static org.cdpg.dx.rs.admin.util.Constants.TOKEN_INVALID_EX_ROUTING_KEY;
import static org.cdpg.dx.rs.admin.util.Constants.UNIQUE_ATTR_EX;
import static org.cdpg.dx.rs.admin.util.Constants.UNIQUE_ATTR_EX_ROUTING_KEY;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cdpg.dx.common.util.DateTimeHelper;
import org.cdpg.dx.database.postgres.models.*;
import org.cdpg.dx.databroker.service.DataBrokerService;
import org.cdpg.dx.rs.admin.dao.RevokedTokenServiceDAO;
import org.cdpg.dx.rs.admin.dao.UniqueAttributeServiceDAO;
import org.cdpg.dx.rs.admin.model.RevokedTokenDTO;
import org.cdpg.dx.rs.admin.model.UniqueAttributeDTO;
import org.cdpg.dx.rs.admin.util.BroadcastEventType;

public class AdminServiceImpl implements AdminService {
  private static final Logger LOGGER = LogManager.getLogger(AdminServiceImpl.class);
  private final DataBrokerService dataBrokerService;
  private final RevokedTokenServiceDAO revokedTokenServiceDAO;
  private final UniqueAttributeServiceDAO uniqueAttributeServiceDAO;

  public AdminServiceImpl(
      RevokedTokenServiceDAO revokedTokenServiceDAO,
      UniqueAttributeServiceDAO uniqueAttributeServiceDAO,
      DataBrokerService dataBrokerService) {
    this.revokedTokenServiceDAO = revokedTokenServiceDAO;
    this.uniqueAttributeServiceDAO = uniqueAttributeServiceDAO;
    this.dataBrokerService = dataBrokerService;
  }

  @Override
  public Future<Void> revokedTokenRequest(String userId) {
    LOGGER.debug("Inside revoked Token request");
    Promise<Void> promise = Promise.promise();
    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("sub", userId);
    rmqMessage.put("expiry", LocalDateTime.now().toString());
    revokedTokenServiceDAO
        .getRevokedTokensByUserId(userId)
        .compose(
            pgSuccess -> {
              if (pgSuccess.isEmpty()) {
                return revokedTokenServiceDAO
                    .insertRevokedToken(
                        new RevokedTokenDTO(
                            UUID.fromString(userId), LocalDateTime.now(), null, null))
                    .onSuccess(
                        success -> {
                          LOGGER.info("inserted successfully");
                        });
              } else {
                return revokedTokenServiceDAO
                    .updateRevokedToken(userId)
                    .onSuccess(
                        success -> {
                          LOGGER.info("updated successfully");
                        });
              }
            })
        .compose(
            pgHandler -> {
              LOGGER.debug("postgres success");
              return dataBrokerService.publishMessageInternal(
                  rmqMessage, TOKEN_INVALID_EX, TOKEN_INVALID_EX_ROUTING_KEY);
            })
        .onSuccess(
            success -> {
              LOGGER.info("Successfully revoked");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error("Failed to query" + failure);
              promise.fail(new ServiceException(ERROR_INTERNAL_SERVER, INTERNAL_SERVER_ERROR));
            });

    return promise.future();
  }

  @Override
  public Future<Void> createUniqueAttribute(String id, String attribute) {
    Promise<Void> promise = Promise.promise();
    uniqueAttributeServiceDAO
        .create(new UniqueAttributeDTO(null, attribute, id, null, null))
        .compose(
            pgHandler -> {
              JsonObject rmqMessage = new JsonObject();
              rmqMessage.put("id", id);
              rmqMessage.put("unique-attribute", attribute);
              rmqMessage.put("eventType", BroadcastEventType.CREATE);
              return dataBrokerService.publishMessageInternal(
                  rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              LOGGER.info("Successfully created");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });
    return promise.future();
  }

  @Override
  public Future<Void> updateUniqueAttribute(String id, String attribute) {
    Promise<Void> promise = Promise.promise();
    uniqueAttributeServiceDAO
        .update(Map.of("resource_id", id), Map.of("unique_attribute", attribute))
        .compose(
            pgHandler -> {
              JsonObject rmqMessage = new JsonObject();
              rmqMessage.put("id", id);
              rmqMessage.put("unique-attribute", attribute);
              rmqMessage.put("eventType", BroadcastEventType.UPDATE);
              return dataBrokerService.publishMessageInternal(
                  rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              LOGGER.info("Successfully updated");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });
    return promise.future();
  }

  @Override
  public Future<Void> deleteUniqueAttribute(String id) {
    Promise<Void> promise = Promise.promise();
    uniqueAttributeServiceDAO
        .delete(UUID.fromString(id))
        .compose(
            pgHandler -> {
              JsonObject rmqMessage = new JsonObject();
              rmqMessage.put("id", id);
              rmqMessage.put("unique-attribute", "dummy_attribute");
              rmqMessage.put("eventType", BroadcastEventType.DELETE);
              return dataBrokerService.publishMessageInternal(
                  rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              LOGGER.info("Successfully deleted");
              promise.complete();
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure);
              promise.fail(failure);
            });
    return promise.future();
  }
}
