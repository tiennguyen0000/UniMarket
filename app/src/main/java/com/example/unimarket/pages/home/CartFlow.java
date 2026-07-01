package com.example.unimarket.pages.home;

import android.text.TextUtils;

import com.example.unimarket.data.model.Cart;
import com.example.unimarket.data.model.CartItem;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.service.CartItemService;
import com.example.unimarket.data.service.CartService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CartFlow {
    public interface Callback {
        void onSuccess();
        void onError(String message);
    }

    private final CartService cartService = new CartService();
    private final CartItemService cartItemService = new CartItemService();

    public void add(Product product, int quantity, Callback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Vui lòng đăng nhập để thêm vào giỏ.");
            return;
        }
        if (product == null || TextUtils.isEmpty(product.getId())) {
            callback.onError("Sản phẩm không hợp lệ.");
            return;
        }
        if (user.getUid().equals(product.getSeller_id())) {
            callback.onError("Bạn không thể mua tin của chính mình.");
            return;
        }

        String status = product.getStatus() != null ? product.getStatus().toLowerCase(Locale.ROOT) : "";
        if (!status.isEmpty() && !status.equals("active") && !status.equals("available")) {
            callback.onError("Sản phẩm này hiện không còn bán.");
            return;
        }
        int available = product.getQuantity() != null ? Math.max(0, product.getQuantity()) : 1;
        if (available <= 0) {
            callback.onError("Sản phẩm này hiện đã hết hàng.");
            return;
        }
        if (quantity > available) {
            callback.onError("Số lượng còn lại chỉ còn " + available + ".");
            return;
        }

        getOrCreateCart(user.getUid(), new CartCallback() {
            @Override
            public void onSuccess(Cart cart) {
                upsertItem(cart, product.getId(), Math.max(1, quantity), available, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void getOrCreateCart(String userId, CartCallback callback) {
        cartService.getWithFilter("user_id", userId, new AsyncCrudService.ListCallback<Cart>() {
            @Override
            public void onSuccess(List<Cart> data) {
                if (data != null && !data.isEmpty()) {
                    callback.onSuccess(data.get(0));
                    return;
                }

                Cart cart = new Cart();
                cart.setId(stableDocId("cart", userId));
                cart.setUser_id(userId);
                cart.setCreated_at(nowIsoUtc());
                cartService.save(cart, result -> {
                    if (result.isSuccess() && result.getData() != null) {
                        callback.onSuccess(result.getData());
                    } else {
                        callback.onError(result.getError());
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void upsertItem(Cart cart, String productId, int quantity, int available, Callback callback) {
        if (cart == null || TextUtils.isEmpty(cart.getId())) {
            callback.onError("Không thể tạo giỏ hàng.");
            return;
        }

        cartItemService.getCartItemsByCartId(cart.getId(), new AsyncCrudService.ListCallback<CartItem>() {
            @Override
            public void onSuccess(List<CartItem> data) {
                CartItem item = findItem(data, productId);
                int existingQuantity = item != null && item.getQuantity() != null ? item.getQuantity() : 0;
                if (item == null) {
                    item = new CartItem();
                    item.setId(stableDocId("cart_item", cart.getId(), productId));
                    item.setCart_id(cart.getId());
                    item.setProduct_id(productId);
                }
                if (existingQuantity + quantity > available) {
                    callback.onError("Số lượng còn lại chỉ còn " + available + ".");
                    return;
                }
                item.setQuantity(existingQuantity + quantity);
                cartItemService.save(item, result -> {
                    if (result.isSuccess()) {
                        callback.onSuccess();
                    } else {
                        callback.onError(result.getError());
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private CartItem findItem(List<CartItem> items, String productId) {
        if (items == null) return null;
        for (CartItem item : items) {
            if (item != null && productId.equals(item.getProduct_id())) return item;
        }
        return null;
    }

    private interface CartCallback {
        void onSuccess(Cart cart);
        void onError(String error);
    }

    public static String nowIsoUtc() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    public static String stableDocId(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) builder.append('_');
            builder.append(part != null ? part : "item");
        }
        return builder.toString().replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
