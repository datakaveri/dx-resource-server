package iudx.resource.server.authenticator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Constants {

  public static final String AUTH_CERTIFICATE_PATH = "/cert";
  public static final List<String> OPEN_ENDPOINTS =
      List.of(
          "/temporal/entities",
          "/entities",
          "/entityOperations/query",
          "/temporal/entityOperations/query",
          "/async/status",
          "/consumer/audit",
          "/async/search",
          "/subscription",
          "/user/resetPassword",
          "/overview",
          "/summary");

  public static final String JSON_USERID = "userid";
  public static final String JSON_IID = "iid";
  public static final String JSON_EXPIRY = "expiry";
  public static final String ROLE = "role";
  public static final String DRL = "drl";
  public static final String DID = "did";
  public static final String ACCESSIBLE_ATTRS = "accessibleAttrs";
  public static final String CAT_SEARCH_PATH = "/search";
  public static final String REVOKED_CLIENT_SQL = "SELECT * FROM revoked_tokens WHERE _id='$1'";
  public static final int JWT_LEEWAY_TIME = 30;

  public static final Map<String, String> ACCESS_MAP =
      Stream.of(
              new Object[][] {
                {"/ngsi-ld/v1/ingestion", "other"},
                {"/ngsi-ld/v1/async/status", "async"},
                {"/ngsi-ld/v1/async/search", "async"},
                {"/ngsi-ld/v1/subscription", "sub"},
                {"/ngsi-ld/v1/entities", "api"},
                {"/ngsi-ld/v1/temporal/entities", "api"},
                {"/ngsi-ld/v1/temporal/entityOperations/query", "api"},
                {"/ngsi-ld/v1/entityOperations/query", "api"}
              })
          .collect(Collectors.toMap(data -> (String) data[0], data -> (String) data[1]));

  public static final String GET_QUERY_FROM_S3_TABLE =
      "select query from s3_upload_url where search_id ='$1'";

  public static final String ASYNC_SEARCH_RGX = "(.*)async/status(.*)";

}
