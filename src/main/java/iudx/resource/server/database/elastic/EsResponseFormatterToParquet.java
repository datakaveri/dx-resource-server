package iudx.resource.server.database.elastic;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.util.*;

public class EsResponseFormatterToParquet extends AbstractEsSearchResponseFormatter {
  //    private File file;
  //    private static final Logger LOGGER =
  // LogManager.getLogger(ConvertElasticResponseToParquet.class);

  public EsResponseFormatterToParquet(File file) {
    super(file);
    this.file = file;
  }

  @Override
  public void write(List<Hit<ObjectNode>> searchHits) {
    //        Schema arrowSchema = this.getSchema(searchHits);
    //        SchemaMapping parquetSchema = this.getParquetSchema(arrowSchema);
    //        MessageType messageTypeParquetSchema = parquetSchema.getParquetSchema();

    //        LOGGER.debug("message type parquet schema {}", messageTypeParquetSchema);
    //        LOGGER.debug("Whats the parquet schema heree ? : {}",parquetSchema.toString());
    //        LOGGER.debug("\n\n\n\n");
    //        LOGGER.debug("What is the schema : {} ", parquetSchema.getChildren());
    //        LOGGER.debug("\n\n\n\n");

    //        ParquetWriter parquetWriter = this.getParquetWriter(file, messageTypeParquetSchema);
  }

  @Override
  public Set<String> writeToCsv(List<Hit<ObjectNode>> searchHits) {
    return null;
  }

  @Override
  public void finish() {}

  @Override
  public void append(List<Hit<ObjectNode>> searchHits, boolean appendComma) {}

  @Override
  public void append(List<Hit<ObjectNode>> searchHits, boolean appendComma, Set<String> headers) {}

  /*    private SchemaMapping getParquetSchema(Schema arrowSchema) {
      SchemaConverter schemaConverter = new SchemaConverter();
      return schemaConverter.fromArrow(arrowSchema);

  }*/

  /*
      private ParquetWriter getParquetWriter(File file, MessageType parquetSchema) {
          Configuration configuration = new Configuration();
  //        LOGGER.debug("whats the file path : ");
          Path path = new Path(file.getPath());
          try {
              HadoopOutputFile hadoopOutputFile = HadoopOutputFile.fromPath(path,configuration);
              ParquetFileWriter p = new ParquetFileWriter(
                      hadoopOutputFile,
                      parquetSchema,
                      ParquetFileWriter.Mode.CREATE, 1024, 1024, 1024, 1024, true
              );

          } catch (IOException e) {
              throw new RuntimeException(e);
          }

          return null;
      }
  */

  /*    public Schema getSchema(List<Hit<ObjectNode>> searchHits) {
          for (Hit hit : searchHits) {
              JsonObject jsonSource = new JsonObject(hit.source().toString());
              List<Field> fields = new ArrayList<>();
              Set<String> firstHit = jsonSource.fieldNames();
              for(String s : firstHit)
              {
                  var value = jsonSource.getValue(s);
                  LOGGER.debug("value : {} ", value);
                  Field field;
                  FieldType fieldType;
                  fieldType = getFieldType(value);
                  field = new Field(s,fieldType,null);
                  fields.add(field);
              }
              Schema schema = new Schema(fields);
  //            LOGGER.debug("whats the schema ? ::::::: ");
  //            LOGGER.debug("{}", schema);
              return schema;
          }
          return null;
      }*/

  /*    private FieldType getFieldType(Object value) {
      FieldType fieldType;
      if(value instanceof Integer)
      {
          fieldType = new FieldType(true,new ArrowType.Int(32, true),null);
      }else if(value instanceof Long)
      {
          fieldType = new FieldType(true,new ArrowType.Int(64, true),null);

      }
      else if(value instanceof Double)
      {
          fieldType = new FieldType(true,new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE),null);

      }
      else if (value instanceof Boolean)
      {
          fieldType = new FieldType(true,ArrowType.Bool.INSTANCE,null);
      }
      else if (value instanceof String)
      {
          fieldType = new FieldType(true,new ArrowType.Utf8(),null);

      }else {
          fieldType = new FieldType(true,new ArrowType.Binary(),null);
      }
      return fieldType;
  }*/

}
