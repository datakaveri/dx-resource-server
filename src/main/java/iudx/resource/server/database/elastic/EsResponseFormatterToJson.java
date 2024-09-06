package iudx.resource.server.database.elastic;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class EsResponseFormatterToJson extends AbstractEsSearchResponseFormatter {
  FileWriter fileWriter;

  /**
   * Writes ElasticSearch response batch response into a JSON File
   *
   * @param file File to write JSON response
   */
  public EsResponseFormatterToJson(File file) {
    super(file);
    try {
      this.fileWriter = new FileWriter(file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void write(List<Hit<ObjectNode>> searchHits) {
    try {
      fileWriter.write('[');
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void finish() {
    try {
      fileWriter.write(']');
      fileWriter.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void append(List<Hit<ObjectNode>> searchHits) {}

  @Override
  public void append(List<Hit<ObjectNode>> searchHits, boolean appendComma) {
    try {
      for (Hit<ObjectNode> sh : searchHits) {
        assert sh.source() != null;
        if (appendComma) {
          fileWriter.write(",\n");
          fileWriter.write(String.valueOf(sh.source()));
        } else {
          fileWriter.write(sh.source().toString());
        }
        appendComma = true;
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
