package org.cdpg.dx.database.postgres.base.dao;

import io.vertx.core.Future;
import org.cdpg.dx.database.postgres.base.enitty.BaseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public interface BaseDAO<T extends BaseEntity<T>> {

  Future<T> create(T entity);

  Future<Boolean> delete(UUID id);

  Future<List<T>> getAll();

  Future<List<T>> getAllWithFilters(Map<String, Object> filters);

  Future<Boolean> update(Map<String, Object> conditionMap, Map<String, Object> updateMap);

  Future<T> get(UUID id);
}