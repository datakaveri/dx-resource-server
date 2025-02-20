package iudx.resource.server.apiserver.admin.service;

import io.vertx.core.http.HttpServerResponse;

public interface AdminService {
  public void revokedTokenRequest(String userid, HttpServerResponse response);

  void createUniqueAttribute(String id, String attribute, HttpServerResponse response);

  void updateUniqueAttribute(String id, String attribute, HttpServerResponse response);

  void deleteUniqueAttribute(String id, HttpServerResponse response);
}
