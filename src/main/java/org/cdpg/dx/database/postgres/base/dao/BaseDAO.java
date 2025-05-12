package org.cdpg.dx.database.postgres.base.dao;

import io.vertx.core.Future;
import java.util.List;
import java.util.UUID;
import org.cdpg.dx.database.postgres.base.enitty.BaseEntity;

public interface BaseDAO<T extends BaseEntity<T>> {
  Future<T> create(T entity);

  Future<Boolean> delete(UUID id);

  Future<List<T>> getAll();

  Future<T> get(UUID id);
}
