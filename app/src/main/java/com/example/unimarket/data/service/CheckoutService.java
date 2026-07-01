package com.example.unimarket.data.service;

import android.text.TextUtils;

import com.example.unimarket.data.DomainConstants;
import com.example.unimarket.data.model.DiscountCode;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.OrderItem;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.service.base.Result;
import com.example.unimarket.data.service.base.ResultCallback;
import com.example.unimarket.data.util.FirestoreIds;
import com.example.unimarket.data.util.TimeUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CheckoutService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final DiscountCodeService discountCodeService = new DiscountCodeService();

    public void addToCart(AddToCartRequest request, ResultCallback<Boolean> callback) {
        String validationError = validateCartRequest(request);
        if (validationError != null) {
            if (callback != null) callback.onResult(Result.error(validationError));
            return;
        }

        String cartId = FirestoreIds.stableDocId("cart", request.buyerId);
        String itemId = FirestoreIds.stableDocId("cart_item", cartId, request.productId);
        DocumentReference cartRef = db.collection(DomainConstants.Collections.CARTS).document(cartId);
        DocumentReference itemRef = db.collection(DomainConstants.Collections.CART_ITEMS).document(itemId);
        DocumentReference productRef = db.collection(DomainConstants.Collections.PRODUCTS).document(request.productId);

        db.runTransaction(transaction -> {
            DocumentSnapshot productSnapshot = transaction.get(productRef);
            Product product = productSnapshot.toObject(Product.class);
            if (!productSnapshot.exists() || product == null) {
                throw new IllegalStateException("Sản phẩm không tồn tại.");
            }
            product.setId(productSnapshot.getId());
            validatePurchasableProduct(product, request.buyerId);
            int available = product.getQuantity() != null ? Math.max(0, product.getQuantity()) : 1;
            DocumentSnapshot existingItem = transaction.get(itemRef);
            Number existingQuantityNumber = existingItem.exists()
                    ? (Number) existingItem.get("quantity")
                    : null;
            int existingQuantity = existingQuantityNumber != null ? existingQuantityNumber.intValue() : 0;
            int requestedQuantity = Math.max(1, request.quantity);
            if (existingQuantity + requestedQuantity > available) {
                throw new IllegalStateException("Số lượng còn lại chỉ còn " + available + ".");
            }

            String now = TimeUtils.nowIsoUtc();
            Map<String, Object> cartData = new HashMap<>();
            cartData.put("id", cartId);
            cartData.put("user_id", request.buyerId);
            cartData.put("created_at", now);
            transaction.set(cartRef, cartData, SetOptions.merge());

            Map<String, Object> itemData = new HashMap<>();
            itemData.put("id", itemId);
            itemData.put("cart_id", cartId);
            itemData.put("product_id", request.productId);
            itemData.put("quantity", FieldValue.increment(requestedQuantity));
            transaction.set(itemRef, itemData, SetOptions.merge());
            return true;
        }).addOnSuccessListener(success -> {
            if (callback != null) callback.onResult(Result.success(true));
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onResult(Result.error(readableError(e)));
        });
    }

    public void createOrder(CheckoutRequest request, ResultCallback<Order> callback) {
        String validationError = validateCheckoutRequest(request);
        if (validationError != null) {
            if (callback != null) callback.onResult(Result.error(validationError));
            return;
        }

        DocumentReference productRef = db.collection(DomainConstants.Collections.PRODUCTS).document(request.productId);
        DocumentReference orderRef = db.collection(DomainConstants.Collections.ORDERS).document();
        DocumentReference orderItemRef = db.collection(DomainConstants.Collections.ORDER_ITEMS)
                .document(FirestoreIds.stableDocId("order_item", orderRef.getId(), request.productId));

        db.runTransaction(transaction -> {
            DocumentSnapshot productSnapshot = transaction.get(productRef);
            Product product = productSnapshot.toObject(Product.class);
            if (!productSnapshot.exists() || product == null) {
                throw new IllegalStateException("Sản phẩm không tồn tại.");
            }
            product.setId(productSnapshot.getId());
            validatePurchasableProduct(product, request.buyerId);
            int available = product.getQuantity() != null ? Math.max(0, product.getQuantity()) : 1;
            int requestedQuantity = Math.max(1, request.quantity);
            if (requestedQuantity > available) {
                throw new IllegalStateException("Số lượng còn lại chỉ còn " + available + ".");
            }

            double unitPrice = product.getPrice() != null ? product.getPrice() : 0d;
            double subtotal = unitPrice * requestedQuantity;
            double discountAmount = 0d;
            String discountCode = null;

            if (!TextUtils.isEmpty(request.discountCodeId)) {
                DocumentReference discountRef = db.collection(DomainConstants.Collections.DISCOUNT_CODES)
                        .document(request.discountCodeId);
                DocumentSnapshot discountSnapshot = transaction.get(discountRef);
                DiscountCode code = discountSnapshot.toObject(DiscountCode.class);
                if (code == null) {
                    throw new IllegalStateException("Mã giảm giá không hợp lệ.");
                }
                code.setId(discountSnapshot.getId());
                DiscountCodeService.Validation validation = discountCodeService.validate(code, subtotal);
                if (!validation.isValid()) {
                    throw new IllegalStateException(validation.getMessage());
                }
                discountAmount = validation.getAmount();
                discountCode = code.getCode();
                transaction.update(discountRef, "used_count", FieldValue.increment(1));
            }

            String now = TimeUtils.nowIsoUtc();
            Order order = new Order();
            order.setId(orderRef.getId());
            order.setBuyer_id(request.buyerId);
            order.setSeller_id(product.getSeller_id());
            order.setProduct_id(product.getId());
            order.setProduct_title(product.getTitle());
            order.setProduct_image_url(request.productImageUrl);
            order.setQuantity(requestedQuantity);
            order.setUnit_price(unitPrice);
            order.setSubtotal_price(subtotal);
            order.setShipping_fee(0d);
            order.setSeller_amount(subtotal);
            order.setDiscount_code(discountCode);
            order.setDiscount_amount(discountAmount);
            order.setTotal_price(Math.max(0d, subtotal - discountAmount));
            order.setStatus(DomainConstants.OrderStatus.PENDING);
            order.setCreated_at(now);
            order.setUpdated_at(now);

            OrderItem item = new OrderItem();
            item.setId(orderItemRef.getId());
            item.setOrder_id(order.getId());
            item.setProduct_id(product.getId());
            item.setSeller_id(product.getSeller_id());
            item.setPrice(unitPrice);
            item.setQuantity(order.getQuantity());

            transaction.set(orderRef, order);
            transaction.set(orderItemRef, item);
            setOrderNotification(transaction, product.getSeller_id(), order.getId(),
                    "Có đơn hàng mới",
                    "Một người mua vừa đặt " + safeProductTitle(product) + ".",
                    now);
            return order;
        }).addOnSuccessListener(order -> {
            if (callback != null) callback.onResult(Result.success(order));
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onResult(Result.error(readableError(e)));
        });
    }

    public void updateOrderStatus(String orderId, String actorId, String nextStatus,
                                  ResultCallback<Order> callback) {
        if (TextUtils.isEmpty(orderId) || TextUtils.isEmpty(actorId) || TextUtils.isEmpty(nextStatus)) {
            if (callback != null) callback.onResult(Result.error("Thiếu thông tin cập nhật đơn hàng."));
            return;
        }

        DocumentReference orderRef = db.collection(DomainConstants.Collections.ORDERS).document(orderId);
        db.runTransaction(transaction -> {
            DocumentSnapshot orderSnapshot = transaction.get(orderRef);
            Order order = orderSnapshot.toObject(Order.class);
            if (!orderSnapshot.exists() || order == null) {
                throw new IllegalStateException("Đơn hàng không tồn tại.");
            }
            order.setId(orderSnapshot.getId());
            boolean isBuyer = actorId.equals(order.getBuyer_id());
            boolean isSeller = actorId.equals(order.getSeller_id());
            if (!isBuyer && !isSeller) {
                throw new IllegalStateException("Bạn không có quyền cập nhật đơn hàng này.");
            }

            String normalizedStatus = nextStatus.trim().toLowerCase(Locale.ROOT);
            if (!isAllowedTransition(order.getStatus(), normalizedStatus, isBuyer, isSeller)) {
                throw new IllegalStateException("Trạng thái đơn hàng không hợp lệ.");
            }

            String now = TimeUtils.nowIsoUtc();
            transaction.update(orderRef, "status", normalizedStatus, "updated_at", now);
            order.setStatus(normalizedStatus);
            order.setUpdated_at(now);

            if (!TextUtils.isEmpty(order.getProduct_id())) {
                DocumentReference productRef = db.collection(DomainConstants.Collections.PRODUCTS)
                        .document(order.getProduct_id());
                if (DomainConstants.OrderStatus.DONE.equals(normalizedStatus)) {
                    DocumentSnapshot productSnapshot = transaction.get(productRef);
                    Product product = productSnapshot.toObject(Product.class);
                    int currentQuantity = product != null && product.getQuantity() != null
                            ? Math.max(0, product.getQuantity())
                            : 1;
                    int orderQuantity = order.getQuantity() != null ? Math.max(1, order.getQuantity()) : 1;
                    int remaining = Math.max(0, currentQuantity - orderQuantity);
                    transaction.update(productRef,
                            "quantity", remaining,
                            "status", remaining > 0 ? DomainConstants.ProductStatus.ACTIVE : DomainConstants.ProductStatus.HIDDEN,
                            "updated_at", now);
                }
            }

            String targetUser = isSeller ? order.getBuyer_id() : order.getSeller_id();
            setOrderNotification(transaction, targetUser, order.getId(),
                    "Cập nhật đơn hàng",
                    "Đơn " + shortOrderId(order.getId()) + " đã chuyển sang " + normalizedStatus + ".",
                    now);
            return order;
        }).addOnSuccessListener(order -> {
            if (callback != null) callback.onResult(Result.success(order));
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onResult(Result.error(readableError(e)));
        });
    }

    private void setOrderNotification(com.google.firebase.firestore.Transaction transaction,
                                      String userId, String orderId, String title,
                                      String content, String now) {
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        String id = FirestoreIds.stableDocId("notification", userId, orderId, String.valueOf(System.nanoTime()));
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("user_id", userId);
        data.put("title", title);
        data.put("content", content);
        data.put("type", DomainConstants.NotificationType.ORDER);
        data.put("target_id", orderId);
        data.put("is_read", false);
        data.put("created_at", now);
        transaction.set(db.collection(DomainConstants.Collections.NOTIFICATIONS).document(id), data);
    }

    private String validateCartRequest(AddToCartRequest request) {
        if (request == null || TextUtils.isEmpty(request.buyerId) || TextUtils.isEmpty(request.productId)) {
            return "Thiếu thông tin giỏ hàng.";
        }
        return null;
    }

    private String validateCheckoutRequest(CheckoutRequest request) {
        if (request == null || TextUtils.isEmpty(request.buyerId) || TextUtils.isEmpty(request.productId)) {
            return "Thiếu thông tin đặt hàng.";
        }
        return null;
    }

    private void validatePurchasableProduct(Product product, String buyerId) {
        if (product == null || TextUtils.isEmpty(product.getId())) {
            throw new IllegalStateException("Sản phẩm không hợp lệ.");
        }
        if (buyerId != null && buyerId.equals(product.getSeller_id())) {
            throw new IllegalStateException("Bạn không thể mua tin của chính mình.");
        }
        String status = product.getStatus() != null
                ? product.getStatus().trim().toLowerCase(Locale.ROOT)
                : DomainConstants.ProductStatus.ACTIVE;
        if (!DomainConstants.ProductStatus.ACTIVE.equals(status) && !"available".equals(status)) {
            throw new IllegalStateException("Sản phẩm này hiện không còn bán.");
        }
        int available = product.getQuantity() != null ? Math.max(0, product.getQuantity()) : 1;
        if (available <= 0) {
            throw new IllegalStateException("Sản phẩm này hiện đã hết hàng.");
        }
    }

    private boolean isAllowedTransition(String currentStatus, String nextStatus,
                                        boolean isBuyer, boolean isSeller) {
        String current = currentStatus != null
                ? currentStatus.trim().toLowerCase(Locale.ROOT)
                : DomainConstants.OrderStatus.PENDING;

        if (DomainConstants.OrderStatus.CANCELLED.equals(nextStatus)) {
            return DomainConstants.OrderStatus.PENDING.equals(current)
                    || DomainConstants.OrderStatus.CONFIRMED.equals(current);
        }
        if (!isSeller) {
            return false;
        }
        if (DomainConstants.OrderStatus.CONFIRMED.equals(nextStatus)) {
            return DomainConstants.OrderStatus.PENDING.equals(current);
        }
        if (DomainConstants.OrderStatus.SHIPPING.equals(nextStatus)) {
            return DomainConstants.OrderStatus.CONFIRMED.equals(current);
        }
        if (DomainConstants.OrderStatus.DONE.equals(nextStatus)) {
            return DomainConstants.OrderStatus.SHIPPING.equals(current);
        }
        return false;
    }

    private String safeProductTitle(Product product) {
        return product != null && !TextUtils.isEmpty(product.getTitle()) ? product.getTitle() : "sản phẩm";
    }

    private String shortOrderId(String id) {
        if (TextUtils.isEmpty(id)) {
            return "#UM";
        }
        return "#UM" + id.substring(0, Math.min(6, id.length())).toUpperCase(Locale.ROOT);
    }

    private String readableError(Exception e) {
        return e != null && e.getMessage() != null ? e.getMessage() : "Thao tác thất bại.";
    }

    public static final class AddToCartRequest {
        public final String buyerId;
        public final String productId;
        public final int quantity;

        public AddToCartRequest(String buyerId, String productId, int quantity) {
            this.buyerId = buyerId;
            this.productId = productId;
            this.quantity = quantity;
        }
    }

    public static final class CheckoutRequest {
        public final String buyerId;
        public final String productId;
        public final String productImageUrl;
        public final int quantity;
        public final String discountCodeId;

        public CheckoutRequest(String buyerId, String productId, String productImageUrl,
                               int quantity, String discountCodeId) {
            this.buyerId = buyerId;
            this.productId = productId;
            this.productImageUrl = productImageUrl;
            this.quantity = quantity;
            this.discountCodeId = discountCodeId;
        }
    }
}
