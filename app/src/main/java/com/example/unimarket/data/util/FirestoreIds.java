package com.example.unimarket.data.util;

import android.text.TextUtils;

public final class FirestoreIds {
    private FirestoreIds() {
    }

    public static String stableDocId(String... parts) {
        StringBuilder builder = new StringBuilder();
        if (parts != null) {
            for (String part : parts) {
                if (builder.length() > 0) {
                    builder.append('_');
                }
                builder.append(!TextUtils.isEmpty(part) ? part : "item");
            }
        }
        return builder.toString().replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
