package com.bitmovin.analytics.conviva;

/*package*/ class ObjectUtils {
    public static <T> T defaultIfNull(T object, T defaultValue) {
        return object != null ? object : defaultValue;
    }
}
