package com.example.unimarket.data.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class TimeUtils {
    private TimeUtils() {
    }

    public static String nowIsoUtc() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    public static boolean isIsoUtcExpired(String isoUtc) {
        if (isoUtc == null || isoUtc.trim().isEmpty()) {
            return false;
        }
        Date parsed = parseIsoUtc(isoUtc);
        return parsed != null && parsed.before(new Date());
    }

    public static Date parseIsoUtc(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                return format.parse(value);
            } catch (Exception ignored) {
                // Try the next supported shape.
            }
        }
        return null;
    }
}
