package com.example.unimarket.pages.home;

import android.text.TextUtils;

import androidx.annotation.DrawableRes;

import com.example.unimarket.R;

import java.text.Normalizer;
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
        String n = normalize(categoryName);

        if (n.contains("laptop") || n.contains("may tinh")) {
            return R.drawable.ic_category_laptop_24;
        }
        if (n.contains("dien thoai") || n.contains("phone") || n.contains("mobile")) {
            return R.drawable.ic_category_phone_24;
        }
        if (n.contains("sach") || n.contains("book") || n.contains("giao trinh")) {
            return R.drawable.ic_category_book_24;
        }
        if (n.contains("thoi trang") || n.contains("quan ao") || n.contains("ao") || n.contains("giay")) {
            return R.drawable.ic_category_shirt_24;
        }
        if (n.contains("phong tro") || n.contains("nha cua") || n.contains("noi that") || n.contains("do dung")) {
            return R.drawable.ic_category_home_24;
        }
        if (n.contains("the thao")) {
            return R.drawable.ic_category_bolt_24;
        }
        if (n.contains("dien tu") || n.contains("am thanh") || n.contains("tai nghe") || n.contains("phu kien") || n.contains("cong nghe")) {
            return R.drawable.ic_category_accessory_24;
        }

        return R.drawable.ic_category_bag_24;
    }

    public static int colorForCategoryName(String categoryName) {
        String n = normalize(categoryName);

        if (n.contains("laptop") || n.contains("may tinh")) {
            return 0xFFEAF0FF;
        }
        if (n.contains("dien thoai") || n.contains("phone")) {
            return 0xFFFFF0F3;
        }
        if (n.contains("sach") || n.contains("book") || n.contains("giao trinh")) {
            return 0xFFE9F8EE;
        }
        if (n.contains("dien tu") || n.contains("am thanh") || n.contains("tai nghe")) {
            return 0xFFFFF2E8;
        }
        if (n.contains("phu kien") || n.contains("cong nghe")) {
            return 0xFFF5EEFF;
        }
        if (n.contains("thoi trang") || n.contains("quan ao") || n.contains("giay")) {
            return 0xFFFFF0F3;
        }
        if (n.contains("the thao")) {
            return 0xFFFFFBE6;
        }
        if (n.contains("nha cua") || n.contains("phong tro") || n.contains("do dung")) {
            return 0xFFE8F5E9;
        }
        if (n.contains("van phong")) {
            return 0xFFE3F2FD;
        }

        return 0xFFF2F4F7;
    }

    public static String formatPrice(Double price) {
        if (price == null || price <= 0d) {
            return "Liên hệ";
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance(VIETNAMESE);
        numberFormat.setMaximumFractionDigits(0);
        return numberFormat.format(price) + "đ";
    }

    public static String formatConditionAndStatus(String condition, String status) {
        if (!TextUtils.isEmpty(condition)) {
            String normalized = normalize(condition);
            if (normalized.contains("new") || normalized.contains("moi")) {
                return "Hàng mới";
            }
            if (normalized.contains("used") || normalized.contains("da qua") || normalized.contains("cu")) {
                return "Đã qua sử dụng";
            }
            if (normalized.contains("good") || normalized.contains("tot")) {
                return "Tình trạng tốt";
            }
            return condition;
        }

        if (!TextUtils.isEmpty(status)) {
            return status;
        }

        return "Sản phẩm";
    }

    private static String normalize(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }
}