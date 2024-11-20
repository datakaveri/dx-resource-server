package iudx.resource.server.dataLimitService.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UniqueKeyUtil {

    // Method to generate a unique key
    public static String generateUniqueKey(String userId, String resourceId, String postfix) {
        // Get the current date in yyyyMMdd format
        String currentDate = new SimpleDateFormat("yyyyMMdd").format(new Date());

        // Combine the elements to create a unique key
        return userId + "" + resourceId + "" + currentDate + "_" + postfix;
    }
}
