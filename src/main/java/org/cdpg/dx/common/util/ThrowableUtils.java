package org.cdpg.dx.common.util;

import org.cdpg.dx.common.exception.BaseDxException;
import org.cdpg.dx.common.exception.DxBadRequestException;
import org.cdpg.dx.common.exception.DxRedisException;
import org.cdpg.dx.common.exception.DxSubscriptionException;

public class ThrowableUtils {

    private ThrowableUtils() {
        // Utility class, prevent instantiation
    }

    public static boolean isSafeToExpose(Throwable throwable) {
        return throwable instanceof IllegalArgumentException || throwable instanceof DxSubscriptionException || throwable instanceof DxBadRequestException;}
}
