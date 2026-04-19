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

        String n = categoryName.toLowerCase(Locale.ROOT);

        if (n.contains("laptop") || n.contains("máy tính") || n.contains("may tinh")) {
            return R.drawable.laptop;
        }
        if (n.contains("điện thoại") || n.contains("dien thoai") || n.contains("phone") || n.contains("mobile")) {
            return R.drawable.phone;
        }
        if (n.contains("sách") || n.contains("sach") || n.contains("book") || n.contains("giáo trình") || n.contains("giao trinh")) {
            return R.drawable.book;
        }
        if (n.contains("điện tử") || n.contains("dien tu") || n.contains("elect") || n.contains("âm thanh") || n.contains("am thanh") || n.contains("tai nghe")) {
            return R.drawable.electronic;
        }
        if (n.contains("phụ kiện") || n.contains("phu kien") || n.contains("thể thao") || n.contains("the thao") || n.contains("nhà cửa") || n.contains("nha cua") || n.contains("văn phòng") || n.contains("van phong") || n.contains("nhựa")) {
            return R.drawable.shopping_cart;
        }

        return R.drawable.shopping_cart;
    }

    public static int colorForCategoryName(String categoryName) {
        if (TextUtils.isEmpty(categoryName)) {
            return 0xFFEAF0FF;
        }

        String n = categoryName.toLowerCase(Locale.ROOT);

        if (n.contains("laptop") || n.contains("máy tính") || n.contains("may tinh")) {
            return 0xFFEAF0FF;
        }
        if (n.contains("điện thoại") || n.contains("dien thoai") || n.contains("phone")) {
            return 0xFFFFF0F3;
        }
        if (n.contains("sách") || n.contains("sach") || n.contains("book") || n.contains("giáo trình") || n.contains("giao trinh")) {
            return 0xFFE9F8EE;
        }
        if (n.contains("điện tử") || n.contains("dien tu") || n.contains("elect") || n.contains("âm thanh") || n.contains("am thanh")) {
            return 0xFFFFF2E8;
        }
        if (n.contains("phụ kiện") || n.contains("phu kien")) {
            return 0xFFF5EEFF;
        }
        if (n.contains("thể thao") || n.contains("the thao")) {
            return 0xFFFFFBE6;
        }
        if (n.contains("nhà cửa") || n.contains("nha cua")) {
            return 0xFFE8F5E9;
        }
        if (n.contains("văn phòng") || n.contains("van phong")) {
            return 0xFFE3F2FD;
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
                return "Hàng mới";
            }
            if (normalized.contains("used")) {
                return "Đã qua sử dụng";
            }
            if (normalized.contains("good")) {
                return "Tình trạng tốt";
            }
            return condition;
        }

        if (!TextUtils.isEmpty(status)) {
            return status;
        }

        return "Sản phẩm";
    }
}