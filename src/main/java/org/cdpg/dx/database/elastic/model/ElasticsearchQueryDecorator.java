package iudx.resource.server.database.elastic.model;

import iudx.resource.server.database.elastic.util.QueryType;

import java.util.List;
import java.util.Map;

public interface ElasticsearchQueryDecorator {
  Map<QueryType, List<QueryModel>> add();
}
