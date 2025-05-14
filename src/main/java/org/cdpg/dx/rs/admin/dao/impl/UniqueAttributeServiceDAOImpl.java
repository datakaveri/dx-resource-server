package org.cdpg.dx.rs.admin.dao.impl;

import static org.cdpg.dx.rs.admin.util.Constants.UNIQUE_ATTR_TABLE;

import org.cdpg.dx.database.postgres.base.dao.AbstractBaseDAO;
import org.cdpg.dx.database.postgres.service.PostgresService;
import org.cdpg.dx.rs.admin.dao.UniqueAttributeServiceDAO;
import org.cdpg.dx.rs.admin.model.UniqueAttributeDTO;

public class UniqueAttributeServiceDAOImpl extends AbstractBaseDAO<UniqueAttributeDTO>
    implements UniqueAttributeServiceDAO {
  public UniqueAttributeServiceDAOImpl(PostgresService postgresService) {
    super(postgresService, UNIQUE_ATTR_TABLE, "resource_id", UniqueAttributeDTO::fromJson);
  }
}
