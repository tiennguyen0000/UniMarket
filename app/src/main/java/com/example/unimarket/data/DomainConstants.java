package com.example.unimarket.data;

public final class DomainConstants {
    private DomainConstants() {
    }

    public static final class Collections {
        public static final String CARTS = "carts";
        public static final String CART_ITEMS = "cart_items";
        public static final String DISCOUNT_CODES = "discount_codes";
        public static final String NOTIFICATIONS = "notifications";
        public static final String ORDER_ITEMS = "order_items";
        public static final String ORDERS = "orders";
        public static final String PRODUCTS = "products";
        public static final String PROFILES = "profiles";
        public static final String REVIEWS = "reviews";

        private Collections() {
        }
    }

    public static final class ProductStatus {
        public static final String ACTIVE = "active";
        public static final String PENDING = "pending";
        public static final String SOLD = "sold";
        public static final String HIDDEN = "hidden";

        private ProductStatus() {
        }
    }

    public static final class ProductCondition {
        public static final String NEW = "new";
        public static final String USED = "used";

        private ProductCondition() {
        }
    }

    public static final class OrderStatus {
        public static final String PENDING = "pending";
        public static final String CONFIRMED = "confirmed";
        public static final String SHIPPING = "shipping";
        public static final String DONE = "done";
        public static final String CANCELLED = "cancelled";

        private OrderStatus() {
        }
    }

    public static final class NotificationType {
        public static final String ORDER = "order";
        public static final String CHAT = "chat";
        public static final String REVIEW = "review";

        private NotificationType() {
        }
    }
}
