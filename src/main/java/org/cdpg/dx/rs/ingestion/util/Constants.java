package org.cdpg.dx.rs.ingestion.util;

import java.util.ArrayList;
import java.util.Arrays;

public class Constants {
    public static final String ITEM_TYPE_RESOURCE = "Resource";
    public static final String ITEM_TYPE_RESOURCE_GROUP = "ResourceGroup";
    public static final String ITEM_TYPE_RESOURCE_SERVER = "ResourceServer";
    public static final String ITEM_TYPE_PROVIDER = "Provider";
    public static final String RESOURCE_GROUP = "resourceGroup";
    public static final ArrayList<String> ITEM_TYPES =
            new ArrayList<String>(
                    Arrays.asList(
                            ITEM_TYPE_RESOURCE,
                            ITEM_TYPE_RESOURCE_GROUP,
                            ITEM_TYPE_RESOURCE_SERVER,
                            ITEM_TYPE_PROVIDER));
    public static final String INGESTION_TABLE = "adaptors_details";
}
