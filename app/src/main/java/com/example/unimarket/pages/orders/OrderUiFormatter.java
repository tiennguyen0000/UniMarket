package com.example.unimarket.pages.orders;

import android.text.TextUtils;

import com.example.unimarket.data.model.Order;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class OrderUiFormatter {
    static final String FILTER_ALL = "all";
    static final String FILTER_PROCESSING = "processing";
    static final String FILTER_SHIPPING = "shipping";
    static final String FILTER_DONE = "done";
    static final String FILTER_CANCELLED = "cancelled";

    private OrderUiFormatter() {
    }

    static String statusLabel(String status) {
        switch (normalizeStatus(status)) {
            case "pending":
                return "Chờ xác nhận";
            case "confirmed":
                return "Đã xác nhận";
            case "shipping":
                return "Đang giao";
            case "done":
                return "Hoàn thành";
            case "cancelled":
                return "Đã hủy";
            default:
                return !TextUtils.isEmpty(status) ? status : "Chờ xác nhận";
        }
    }

    static boolean matchesFilter(Order order, String filter) {
        if (order == null) {
            return false;
        }
        if (TextUtils.isEmpty(filter) || FILTER_ALL.equals(filter)) {
            return true;
        }

        String status = normalizeStatus(order.getStatus());
        if (FILTER_PROCESSING.equals(filter)) {
            return "pending".equals(status) || "confirmed".equals(status);
        }
        return filter.equals(status);
    }

    static int statusTextColor(String status) {
        switch (normalizeStatus(status)) {
            case "done":
                return 0xFF027A48;
            case "shipping":
                return 0xFF175CD3;
            case "cancelled":
                return 0xFFB42318;
            case "confirmed":
                return 0xFF6941C6;
            case "pending":
            default:
                return 0xFFB54708;
        }
    }

    static int statusBgColor(String status) {
        switch (normalizeStatus(status)) {
            case "done":
                return 0xFFECFDF3;
            case "shipping":
                return 0xFFEFF8FF;
            case "cancelled":
                return 0xFFFEF3F2;
            case "confirmed":
                return 0xFFF4F3FF;
            case "pending":
            default:
                return 0xFFFFFAEB;
        }
    }

    static String shortOrderId(String id) {
        if (TextUtils.isEmpty(id)) {
            return "#UMXXXXX";
        }
        return "#UM" + id.substring(0, Math.min(6, id.length())).toUpperCase(Locale.ROOT);
    }

    static String ctaLabel(String status) {
        switch (normalizeStatus(status)) {
            case "shipping":
                return "Theo dõi";
            case "done":
                return "Đánh giá";
            case "pending":
            case "confirmed":
            case "cancelled":
            default:
                return "Chi tiết";
        }
    }

    static String formatCreatedAt(String value) {
        if (TextUtils.isEmpty(value)) {
            return "Chưa có ngày";
        }

        Date date = parseIsoUtc(value);
        if (date == null) {
            return value;
        }

        SimpleDateFormat output = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return output.format(date);
    }

    private static Date parseIsoUtc(String value) {
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                return format.parse(value);
            } catch (Exception ignored) {
                // Try the next known timestamp shape.
            }
        }
        return null;
    }

    private static String normalizeStatus(String status) {
        return status != null ? status.trim().toLowerCase(Locale.ROOT) : "pending";
    }
}
