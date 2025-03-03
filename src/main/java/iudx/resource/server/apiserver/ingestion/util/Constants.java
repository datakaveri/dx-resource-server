package iudx.resource.server.apiserver.ingestion.util;

public class Constants {
  public static final String CREATE_INGESTION_SQL =
      "INSERT INTO "
          + "adaptors_details(exchange_name,resource_id,dataset_name,dataset_details_json,user_id,providerid) "
          + "VALUES('$1','$2','$3','$4','$5','$6') "+ "ON CONFLICT (exchange_name) DO NOTHING";

  public static final String DELETE_INGESTION_SQL =
      "DELETE from adaptors_details where exchange_name='$0'";
  public static final String PROVIDER_ID = "providerID";
  public static final String SELECT_INGESTION_SQL =
          "SELECT * from adaptors_details where providerid = '$0'";
  public static final String ID = "id";
}
