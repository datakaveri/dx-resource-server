package iudx.resource.server.database.postgres;

public class Constants {

  public static String INSERT_REVOKE_TOKEN_SQL =
      "INSERT INTO revoked_tokens (_id, expiry) VALUES('$1','$2') "
          + "ON CONFLICT (_id) "
          + "DO UPDATE SET expiry = '$2'";

  public static String INSERT_UNIQUE_ATTR_SQL =
      "INSERT INTO unique_attributes(resource_id,unique_attribute) VALUES('$1','$2')";

  public static String UPDATE_UNIQUE_ATTR_SQL =
      "UPDATE unique_attributes SET unique_attribute='$1' WHERE resource_id='$2'";

  public static String DELETE_UNIQUE_ATTR_SQL =
      "DELETE FROM unique_attributes WHERE resource_id = '$1'";

  public static String INSERT_S3_PENDING_SQL =
      "INSERT INTO s3_upload_url(_id, search_id, request_id, user_id, status, progress,query,isaudited) "
          + "values('$1','$2','$3','$4','$5', $6, '$7'::JSON,false)";

  public static String UPDATE_S3_URL_SQL =
      "UPDATE s3_upload_url SET s3_url='$1', expiry='$2', status='$3', object_id='$4', "
          + "progress=$5, size='$6',isaudited = false WHERE search_id='$7' and progress<$5";

  public static String UPDATE_STATUS_SQL =
      "UPDATE s3_upload_url SET status='$1' WHERE search_id='$2'";

  public static String SELECT_S3_STATUS_SQL =
      "SELECT status,s3_url,search_id,user_id,expiry,progress,isaudited,size FROM s3_upload_url WHERE search_id='$1'";

  public static String SELECT_S3_SEARCH_SQL =
      "SELECT search_id, status, s3_url, expiry, user_id, object_id,size "
          + "FROM s3_upload_url WHERE request_id='$1' and status='$2'";

  public static String UPDATE_S3_PROGRESS_SQL =
      "UPDATE s3_upload_url SET progress=$1 WHERE search_id='$2'";

  public static String UPDATE_ISAUDITED_SQL =
      "UPDATE s3_upload_url SET isaudited=true WHERE search_id='$1'";
}
