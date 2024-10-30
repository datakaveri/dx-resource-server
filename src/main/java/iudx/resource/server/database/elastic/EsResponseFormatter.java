package iudx.resource.server.database.elastic;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Set;

public interface EsResponseFormatter {
  void write(List<Hit<ObjectNode>> searchHits);

  Set<String> writeToCsv(List<Hit<ObjectNode>> searchHits);

  void finish();

  void append(List<Hit<ObjectNode>> searchHits, boolean appendComma);

  void append(List<Hit<ObjectNode>> searchHits, boolean appendComma, Set<String> headers);
}
