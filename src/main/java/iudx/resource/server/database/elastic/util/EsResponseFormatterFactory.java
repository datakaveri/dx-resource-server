package iudx.resource.server.database.elastic.util;

import static iudx.resource.server.apiserver.util.Constants.HEADER_CSV;
import java.io.File;

public class EsResponseFormatterFactory {
  private AbstractEsSearchResponseFormatter responseToCsv;
  // private AbstractConvertElasticSearchResponse responseToParquet;
  private AbstractEsSearchResponseFormatter responseToJson;
  private String format;

  public EsResponseFormatterFactory(String format, File file) {
    this.format = format;
    responseToCsv = new EsResponseFormatterToCsv(file);
    // responseToParquet = new ConvertElasticResponseToParquet(file);
    responseToJson = new EsResponseFormatterToJson(file);
  }

  public EsResponseFormatter createInstance() {
    switch (format) {
      case HEADER_CSV:
        return responseToCsv;
        //            case HEADER_PARQUET:
        //                return responseToParquet;
      default:
        return responseToJson;
    }
  }
}
