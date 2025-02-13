package iudx.resource.server.database.elastic.util;

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

public class EsResponseFormatterToCsv extends AbstractEsSearchResponseFormatter {
  static JsonFlatten jsonFlatten;
  static LinkedHashMap<String, Object> map;
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
  public void flattenRecord(List<Hit<ObjectNode>> searchHits, Set<String> headers) {
    for (Hit hit : searchHits) {
      jsonFlatten = new JsonFlatten((JsonNode) hit.source());
      map = jsonFlatten.flatten();
      /*Set<String> header = map.keySet();*/
      /*appendToCsvFile(map, header);*/
      appendToCsvFile(map, headers);
    }
  }

  /**
   * Produces set of headers from the first record or row of the data
   *
   * @param searchHits Elastic search scroll response
   */
  public Set<String> getHeader(List<Hit<ObjectNode>> searchHits) {
    Hit<ObjectNode> firstHit = searchHits.get(0);
    if (jsonFlatten == null) {
      jsonFlatten = new JsonFlatten((JsonNode) firstHit.source());
    }
    map = jsonFlatten.flatten();
    Set<String> header = map.keySet();
    simpleFileWriter(header);
    return header;
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
        stringBuilder.append("").append(",");
      } else {
        String cellValue = cell.toString();
        if (cellValue.contains(",") || cellValue.contains("\"")) {
          cellValue = "\"" + cellValue.replace("\"", "\"\"") + "\"";
        }
        stringBuilder.append(cellValue).append(",");
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
      stringBuilder.append(obj + ",");
    }
    String data = stringBuilder.substring(0, stringBuilder.length() - 1);
    try {
      fileWriter.write(data + "\n");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void write(List<Hit<ObjectNode>> searchHits) {}

  @Override
  public Set<String> writeToCsv(List<Hit<ObjectNode>> searchHits) {
    return this.getHeader(searchHits);
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
  public void append(List<Hit<ObjectNode>> searchHits, boolean appendComma) {}

  @Override
  public void append(List<Hit<ObjectNode>> searchHits, boolean appendComma, Set<String> headers) {
    this.flattenRecord(searchHits, headers);
  }
}
