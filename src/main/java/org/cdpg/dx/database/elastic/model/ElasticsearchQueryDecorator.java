package org.cdpg.dx.database.elastic.model;


import org.cdpg.dx.database.elastic.util.QueryType;

import java.util.List;
import java.util.Map;

public interface ElasticsearchQueryDecorator {
  Map<QueryType, List<QueryModel>> add();
}
