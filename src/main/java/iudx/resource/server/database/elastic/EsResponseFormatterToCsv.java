package iudx.resource.server.database.elastic;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EsResponseFormatterToCsv extends AbstractEsSearchResponseFormatter {
  static JsonFlatten jsonFlatten;
  static LinkedHashMap<String, Object> map;
  private final Logger LOGGER = LogManager.getLogger(EsResponseFormatterToCsv.class);
  FileWriter fileWriter;

  /**
   * Converts JSON records from Elasticsearch batch response to CSV format and writes it into a CSV
   * file
   *
   * @param file File to write csv records
   */
  public EsResponseFormatterToCsv(File file) {
    super(file);
    try {
      this.fileWriter = new FileWriter(file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Flattens each record from Elastic search response and appends it to the file
   *
   * @param searchHits ElasticSearch response searchHits
   */
  public void flattenRecord(List<Hit<ObjectNode>> searchHits) {
    for (Hit hit : searchHits) {
      jsonFlatten = new JsonFlatten((JsonNode) hit.source());
      map = jsonFlatten.flatten();
      Set<String> header = map.keySet();
      appendToCsvFile(map, header);
    }
  }

  /**
   * Produces set of headers from the first record or row of the data
   *
   * @param searchHits Elastic search scroll response
   */
  public void getHeader(List<Hit<ObjectNode>> searchHits) {
    Hit<ObjectNode> firstHit = searchHits.get(0);
    if (jsonFlatten == null) {
      jsonFlatten = new JsonFlatten(firstHit.source());
    }
    map = jsonFlatten.flatten();
    Set<String> header = map.keySet();
    simpleFileWriter(header);
  }

  /**
   * Appends the values from the records to the csv file according to the header
   *
   * @param map Data to be appended
   * @param header Set of headers
   */
  public void appendToCsvFile(Map<String, Object> map, Set<String> header) {

    StringBuilder stringBuilder = new StringBuilder();

    for (String field : header) {
      Object cell = map.get(field);
      if (cell == null) {
        stringBuilder.append("NAN").append(",");
      } else {
        stringBuilder.append(cell).append(",");
      }
    }

    String row = stringBuilder.substring(0, stringBuilder.length() - 1);
    try {
      fileWriter.append(row).append("\n");
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Writes the column names or the header for the csv file
   *
   * @param header Set of headers to be written
   */
  private void simpleFileWriter(Set<String> header) {
    StringBuilder stringBuilder = new StringBuilder();
    for (String obj : header) {
      stringBuilder.append(obj).append(",");
    }
    String data = stringBuilder.substring(0, stringBuilder.length() - 1);
    try {
      fileWriter.write(data + "\n");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void write(List<Hit<ObjectNode>> searchHits) {
    this.getHeader(searchHits);
  }

  @Override
  public void finish() {
    try {
      fileWriter.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void append(List<Hit<ObjectNode>> searchHits) {
    this.flattenRecord(searchHits);
  }

  @Override
  public void append(List<Hit<ObjectNode>> searchHits, boolean appendComma) {
    this.flattenRecord(searchHits);
  }
}
