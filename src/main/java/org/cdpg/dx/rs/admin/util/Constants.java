package org.cdpg.dx.rs.admin.util;

public class Constants {
  public static final String ADMIN = "/admin";
  public static final String REVOKE_TOKEN = "/revokeToken";
  public static final String RESOURCE_ATTRIBS = "/resourceattribute";
  public static String TOKEN_INVALID_EX = "invalid-sub";
  public static String TOKEN_INVALID_EX_ROUTING_KEY = "invalid-sub";
  public static String INSERT_REVOKE_TOKEN_SQL =
      "INSERT INTO revoked_tokens (_id, expiry) VALUES('$1','$2') "
          + "ON CONFLICT (_id) "
          + "DO UPDATE SET expiry = '$2'";
  public static String UNIQUE_ATTR_EX = "latest-data-unique-attributes";
  public static String UNIQUE_ATTR_EX_ROUTING_KEY = "unique-attribute";
  public static String INSERT_UNIQUE_ATTR_SQL =
      "INSERT INTO unique_attributes(resource_id,unique_attribute) VALUES('$1','$2')";
  public static String UPDATE_UNIQUE_ATTR_SQL =
      "UPDATE unique_attributes SET unique_attribute='$1' WHERE resource_id='$2'";
  public static String DELETE_UNIQUE_ATTR_SQL =
      "DELETE FROM unique_attributes WHERE resource_id = '$1'";

  public static String REVOKED_TOKEN_TABLE = "revoked_tokens";
  public static String UNIQUE_ATTR_TABLE = "unique_attributes";
}
