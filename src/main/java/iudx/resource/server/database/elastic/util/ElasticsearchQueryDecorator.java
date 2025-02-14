package iudx.resource.server.database.elastic.util;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.List;
import java.util.Map;

public interface ElasticsearchQueryDecorator {
  Map<FilterType, List<Query>> add();
}
