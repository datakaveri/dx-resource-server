package org.cdpg.dx.rs.subscription.util;

import java.util.ArrayList;
import java.util.Arrays;

public class Constants {
  public static final String GET_ALL_QUEUE =
      "SELECT queue_name as queueName,entity,dataset_json as catItem "
          + "FROM subscriptions WHERE user_id ='$1'";
  public static final String SELECT_SUB_SQL =
          "SELECT * from subscriptions where queue_name='$1' and entity='$2'";
  public static final String CREATE_SUB_SQL =
          "INSERT INTO subscriptions"
                  + "(_id,_type,queue_name,entity,expiry,dataset_name,dataset_json,user_id,"
                  + "resource_group,provider_id,delegator_id,item_type) "
                  + "VALUES('$1','$2','$3','$4','$5','$6','$7','$8','$9','$a','$b','$c')";

  public static final String UPDATE_SUB_SQL =
          "UPDATE subscriptions SET expiry='$1' where queue_name='$2' and entity='$3'";
  public static final String DELETE_SUB_SQL = "DELETE FROM subscriptions where queue_name='$1'";
  public static final String RESULTS = "results";
  public static final String DATA_WILDCARD_ROUTINGKEY = "/.*";

  public static final String ITEM_TYPE_RESOURCE = "Resource";
  public static final String ITEM_TYPE_RESOURCE_GROUP = "ResourceGroup";
  public static final String ITEM_TYPE_RESOURCE_SERVER = "ResourceServer";
  public static final String ITEM_TYPE_PROVIDER = "Provider";

  public static final ArrayList<String> ITEM_TYPES =
      new ArrayList<String>(
          Arrays.asList(
              ITEM_TYPE_RESOURCE,
              ITEM_TYPE_RESOURCE_GROUP,
              ITEM_TYPE_RESOURCE_SERVER,
              ITEM_TYPE_PROVIDER));

  public static final String ENTITIES = "entities";
  public static final String RESOURCE_GROUP = "resourceGroup";
  public static final String PROVIDER = "provider";
  public static final String ID = "id";
  public static final String QUEUE_ALREADY_EXISTS = "Queue already exists";
  public static final String SUBSCRIPTION_TABLE = "subscriptions";
}
