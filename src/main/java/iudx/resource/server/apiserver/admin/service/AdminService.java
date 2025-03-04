package iudx.resource.server.apiserver.admin.service;

import io.vertx.core.Future;
import iudx.resource.server.apiserver.admin.model.ResultModel;

public interface AdminService {
  Future<ResultModel> revokedTokenRequest(String userid);

  Future<ResultModel> createUniqueAttribute(String id, String attribute);

  Future<ResultModel> updateUniqueAttribute(String id, String attribute);

  Future<ResultModel> deleteUniqueAttribute(String id);
}
