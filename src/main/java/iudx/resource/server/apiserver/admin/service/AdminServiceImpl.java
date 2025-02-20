package iudx.resource.server.apiserver.admin.service;

import static iudx.resource.server.apiserver.admin.util.Constants.*;
import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.resource.server.common.HttpStatusCode.*;
import static iudx.resource.server.common.ResponseUrn.*;
import static iudx.resource.server.common.ResponseUrn.fromCode;
import static iudx.resource.server.common.ResponseUtil.generateResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.postgres.service.PostgresService;
import iudx.resource.server.databroker.service.DataBrokerService;
import iudx.resource.server.databroker.util.BroadcastEventType;
import java.time.LocalDateTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdminServiceImpl implements AdminService {
  private static final Logger LOGGER = LogManager.getLogger(AdminServiceImpl.class);
  private final PostgresService postgresService;
  private final DataBrokerService dataBrokerService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  JsonObject result = new JsonObject();

  public AdminServiceImpl(PostgresService postgresService, DataBrokerService dataBrokerService) {
    this.postgresService = postgresService;
    this.dataBrokerService = dataBrokerService;
  }

  @Override
  public void revokedTokenRequest(String userid, HttpServerResponse response) {
    StringBuilder query =
        new StringBuilder(
            INSERT_REVOKE_TOKEN_SQL
                .replace("$1", userid)
                .replace("$2", LocalDateTime.now().toString()));

    LOGGER.trace("query : " + query);

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("sub", userid);
    rmqMessage.put("expiry", LocalDateTime.now().toString());

    postgresService
        .executeQuery(query.toString())
        .compose(
            pgHandler -> {
              return dataBrokerService.publishMessage(
                  rmqMessage, TOKEN_INVALID_EX, TOKEN_INVALID_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              /*result =
              getResponseJson(
                  SUCCESS.getUrn(),
                  SUCCESS.getValue(),
                  SUCCESS.getDescription(),
                  SUCCESS.getDescription());*/
              /*Future.future(fu -> updateAuditTable(context));*/
              handleResponse(response, SUCCESS, SUCCESS_URN);
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure.getMessage());
              try {
                Response resp = objectMapper.readValue(failure.getMessage(), Response.class);
                /*result = getResponseJson(BAD_REQUEST.getUrn(), BAD_REQUEST.getValue(),BAD_REQUEST.getDescription(),resp.toString());*/
                handleResponse(response, resp);
              } catch (JsonProcessingException e) {
                LOGGER.error("Failure message not in format [type,title,detail]");
                handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
                /*result = getResponseJson(BAD_REQUEST.getUrn(), BAD_REQUEST.getValue(),BAD_REQUEST.getDescription(),BAD_REQUEST.getDescription());*/
              }
            });
  }

  @Override
  public void createUniqueAttribute(String id, String attribute, HttpServerResponse response) {
    if (id == null || attribute == null) {
      handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
      return;
    }

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", attribute);
    rmqMessage.put("eventType", BroadcastEventType.CREATE);

    StringBuilder query =
        new StringBuilder(INSERT_UNIQUE_ATTR_SQL.replace("$1", id).replace("$2", attribute));

    LOGGER.debug("query : " + query);

    postgresService
        .executeQuery(query.toString())
        .compose(
            pgHandler -> {
              return dataBrokerService.publishMessage(
                  rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              /*Future.future(fu -> updateAuditTable(context));*/
              handleResponse(response, SUCCESS, SUCCESS_URN);
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure.getMessage());
              try {
                Response resp = objectMapper.readValue(failure.getMessage(), Response.class);
                handleResponse(response, resp);
              } catch (JsonProcessingException e) {
                LOGGER.error("Failure message not in format [type,title,detail]");
                handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
              }
            });
  }

  @Override
  public void updateUniqueAttribute(String id, String attribute, HttpServerResponse response) {
    if (id == null || attribute == null) {
      handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
      return;
    }
    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", attribute);
    rmqMessage.put("eventType", BroadcastEventType.UPDATE);
    String query = UPDATE_UNIQUE_ATTR_SQL.replace("$2", id).replace("$1", attribute);
    LOGGER.debug("query : " + query);

    postgresService
        .executeQuery(query.toString())
        .compose(
            pgHandler -> {
              return dataBrokerService.publishMessage(
                  rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              /*Future.future(fu -> updateAuditTable(context));*/
              handleResponse(response, SUCCESS, SUCCESS_URN);
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure.getMessage());
              try {
                Response resp = objectMapper.readValue(failure.getMessage(), Response.class);
                handleResponse(response, resp);
              } catch (JsonProcessingException e) {
                LOGGER.error("Failure message not in format [type,title,detail]");
                handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
              }
            });
  }

  @Override
  public void deleteUniqueAttribute(String id, HttpServerResponse response) {
    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", "dummy_attribute");
    rmqMessage.put("eventType", BroadcastEventType.DELETE);

    String query = DELETE_UNIQUE_ATTR_SQL.replace("$1", id);
    LOGGER.trace("query : " + query);

    JsonObject queryparams = new JsonObject();
    postgresService
        .executePreparedQuery(query, queryparams)
        .compose(
            pgHandler -> {
              return dataBrokerService.publishMessage(
                  rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY);
            })
        .onSuccess(
            dataBrokerHandler -> {
              /*Future.future(fu -> updateAuditTable(context));*/
              handleResponse(response, SUCCESS, SUCCESS_URN);
            })
        .onFailure(
            failure -> {
              LOGGER.error(failure.getMessage());
              try {
                Response resp = objectMapper.readValue(failure.getMessage(), Response.class);
                handleResponse(response, resp);
              } catch (JsonProcessingException e) {
                LOGGER.error("Failure message not in format [type,title,detail]");
                handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
              }
            });
  }

  private void handleResponse(HttpServerResponse response, Response respObject) {
    ResponseUrn urn = fromCode(respObject.getType());
    handleResponse(response, respObject, urn.getMessage());
  }

  private void handleResponse(HttpServerResponse response, Response respObject, String message) {
    HttpStatusCode httpCode = getByValue(respObject.getStatus());
    ResponseUrn urn = fromCode(respObject.getType());
    handleResponse(response, httpCode, urn, message);
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
}
