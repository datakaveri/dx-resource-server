package org.cdpg.dx.rs.admin.dao;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import org.cdpg.dx.database.postgres.base.dao.BaseDAO;
import org.cdpg.dx.rs.admin.model.RevokedTokenDTO;


public interface RevokedTokenServiceDAO extends BaseDAO<RevokedTokenDTO> {
    Future<JsonArray> getRevokedTokensByUserId(String userId);
    Future<JsonArray> insertRevokedToken(RevokedTokenDTO revokedTokenDTO);
    Future<JsonArray> updateRevokedToken(String userId);
}
