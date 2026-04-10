package com.example.unimarket.pages.home;

import android.text.TextUtils;

import androidx.annotation.DrawableRes;

import com.example.unimarket.R;

import java.text.NumberFormat;
import java.util.Locale;

public final class HomeUiUtils {
    private static final Locale VIETNAMESE = new Locale("vi", "VN");

    private HomeUiUtils() {
    }

    public static String extractInitial(String name) {
        if (TextUtils.isEmpty(name)) {
            return "U";
        }

        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "U";
        }

        return String.valueOf(Character.toUpperCase(trimmed.charAt(0)));
    }

    @DrawableRes
    public static int iconResForCategoryName(String categoryName) {
        if (TextUtils.isEmpty(categoryName)) {
            return R.drawable.laptop;
        }

        String normalized = categoryName.toLowerCase(Locale.ROOT);

        if (normalized.contains("laptop")) {
            return R.drawable.laptop;
        }
        if (normalized.contains("dien") || normalized.contains("elect")) {
            return R.drawable.electronic;
        }
        if (normalized.contains("sach") || normalized.contains("book")) {
            return R.drawable.book;
        }
        if (normalized.contains("dienthoai") || normalized.contains("phones")) {
            return R.drawable.phone;
        }


        return R.drawable.laptop;
    }

    public static int colorForCategoryName(String categoryName) {
        if (TextUtils.isEmpty(categoryName)) {
            return 0xFFEAF0FF;
        }

        String normalized = categoryName.toLowerCase(Locale.ROOT);
        if (normalized.contains("laptop")) {
            return 0xFFEAF0FF;
        }
        if (normalized.contains("dien") || normalized.contains("elect")) {
            return 0xFFFFF2E8;
        }
        if (normalized.contains("sach") || normalized.contains("book")) {
            return 0xFFE9F8EE;
        }
        if (normalized.contains("pham") || normalized.contains("nhu yeu")) {
            return 0xFFF5EEFF;
        }
        if (normalized.contains("phu kien")) {
            return 0xFFF2F4F7;
        }
        return 0xFFF2F4F7;
    }

    public static String formatPrice(Double price) {
        if (price == null || price <= 0d) {
            return "Lien he";
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance(VIETNAMESE);
        numberFormat.setMaximumFractionDigits(0);
        return numberFormat.format(price) + "d";
    }

    public static String formatConditionAndStatus(String condition, String status) {
        if (!TextUtils.isEmpty(condition)) {
            String normalized = condition.toLowerCase(Locale.ROOT);
            if (normalized.contains("new")) {
                return "Hang moi";
            }
            if (normalized.contains("used")) {
                return "Da qua su dung";
            }
            if (normalized.contains("good")) {
                return "Tinh trang tot";
            }
            return condition;
        }

        if (!TextUtils.isEmpty(status)) {
            return status;
        }

        return "San pham";
    }
}