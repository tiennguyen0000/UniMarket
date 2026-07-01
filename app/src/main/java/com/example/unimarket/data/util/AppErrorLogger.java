package com.example.unimarket.data.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;

public final class AppErrorLogger {
    public static final String FILE_NAME = "unimarket_error_log.txt";
    private static final String TAG = "UniMarketErrorLog";

    private AppErrorLogger() {
    }

    public static void append(Context context, String area, String message) {
        if (context == null) {
            return;
        }
        File file = new File(context.getFilesDir(), FILE_NAME);
        String line = Instant.now() + " [" + safe(area) + "] " + safe(message) + "\n";
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(line);
        } catch (IOException e) {
            Log.e(TAG, "Cannot append app error log", e);
        }
    }

    public static File file(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    private static String safe(String value) {
        return value != null ? value.replace('\n', ' ').replace('\r', ' ') : "";
    }
}
