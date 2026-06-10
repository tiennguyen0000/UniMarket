package com.example.unimarket.pages.home;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.Cart;
import com.example.unimarket.data.model.CartItem;
import com.example.unimarket.data.model.Notification;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.OrderItem;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.service.CartItemService;
import com.example.unimarket.data.service.CartService;
import com.example.unimarket.data.service.NotificationService;
import com.example.unimarket.data.service.OrderItemService;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartBottomSheetFragment extends BottomSheetDialogFragment {
    private RecyclerView rvCartItems;
    private View layoutCartEmpty;
    private View layoutCartFooter;
    private ProgressBar progressCart;
    private TextView tvCartTotal;
    private TextView btnCartCheckout;
    private CartItemAdapter adapter;

    private final CartService cartService = new CartService();
    private final CartItemService cartItemService = new CartItemService();
    private final ProductService productService = new ProductService();
    private final OrderService orderService = new OrderService();
    private final OrderItemService orderItemService = new OrderItemService();
    private final NotificationService notificationService = new NotificationService();

    public static CartBottomSheetFragment newInstance() {
        return new CartBottomSheetFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_cart, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        bottomSheet.setLayoutParams(params);
        BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvCartItems = view.findViewById(R.id.rvCartItems);
        layoutCartEmpty = view.findViewById(R.id.layoutCartEmpty);
        layoutCartFooter = view.findViewById(R.id.layoutCartFooter);
        progressCart = view.findViewById(R.id.progressCart);
        tvCartTotal = view.findViewById(R.id.tvCartTotal);
        btnCartCheckout = view.findViewById(R.id.btnCartCheckout);
        ImageView close = view.findViewById(R.id.ivCartClose);

        adapter = new CartItemAdapter(new CartItemAdapter.Listener() {
            @Override
            public void onQuantityChanged(CartItemAdapter.CartLine line, int quantity) {
                updateQuantity(line, quantity);
            }

            @Override
            public void onRemove(CartItemAdapter.CartLine line) {
                removeLine(line);
            }
        });
        rvCartItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCartItems.setAdapter(adapter);
        close.setOnClickListener(v -> dismiss());
        btnCartCheckout.setOnClickListener(v -> checkout());

        loadCart();
    }

    private void loadCart() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showEmpty();
            return;
        }

        setLoading(true);
        cartService.getWithFilter("user_id", user.getUid(), new AsyncCrudService.ListCallback<Cart>() {
            @Override
            public void onSuccess(List<Cart> data) {
                if (!isAdded()) {
                    return;
                }
                if (data == null || data.isEmpty()) {
                    setLoading(false);
                    showEmpty();
                    return;
                }
                loadItems(data.get(0));
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                showEmpty();
            }
        });
    }

    private void loadItems(Cart cart) {
        cartItemService.getCartItemsByCartId(cart.getId(), new AsyncCrudService.ListCallback<CartItem>() {
            @Override
            public void onSuccess(List<CartItem> data) {
                if (!isAdded()) {
                    return;
                }
                List<CartItem> items = data != null ? data : new ArrayList<>();
                if (items.isEmpty()) {
                    setLoading(false);
                    showEmpty();
                    return;
                }
                loadProducts(items);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                showEmpty();
            }
        });
    }

    private void loadProducts(List<CartItem> items) {
        List<CartItemAdapter.CartLine> lines = new ArrayList<>();
        final int[] pending = {items.size()};
        for (CartItem item : items) {
            productService.getById(item.getProduct_id(), new AsyncCrudService.ItemCallback<Product>() {
                @Override
                public void onSuccess(Product product) {
                    if (isPurchasable(product)) {
                        lines.add(new CartItemAdapter.CartLine(item, product));
                    }
                    finishOne();
                }

                @Override
                public void onError(String error) {
                    finishOne();
                }

                private void finishOne() {
                    pending[0]--;
                    if (pending[0] == 0 && isAdded()) {
                        setLoading(false);
                        adapter.submitList(lines);
                        bindTotal();
                        if (lines.isEmpty()) {
                            showEmpty();
                        } else {
                            showList();
                        }
                    }
                }
            });
        }
    }

    private void updateQuantity(CartItemAdapter.CartLine line, int quantity) {
        if (quantity <= 0) {
            removeLine(line);
            return;
        }
        line.item.setQuantity(quantity);
        cartItemService.save(line.item, result -> loadCart());
    }

    private void removeLine(CartItemAdapter.CartLine line) {
        if (line == null || line.item == null || TextUtils.isEmpty(line.item.getId())) {
            return;
        }
        cartItemService.deleteById(line.item.getId(), new AsyncCrudService.BooleanCallback() {
            @Override
            public void onSuccess(boolean success) {
                loadCart();
            }

            @Override
            public void onError(String error) {
                loadCart();
            }
        });
    }

    private void checkout() {
        List<CartItemAdapter.CartLine> lines = adapter.currentItems();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || lines.isEmpty()) {
            return;
        }

        setCheckoutLoading(true);
        final int[] pending = {lines.size()};
        final boolean[] failed = {false};
        for (CartItemAdapter.CartLine line : lines) {
            saveOrder(user.getUid(), line, new CheckoutCallback() {
                @Override
                public void onComplete(boolean success) {
                    if (!success) {
                        failed[0] = true;
                    }
                    pending[0]--;
                    if (pending[0] == 0 && isAdded()) {
                        setCheckoutLoading(false);
                        if (!failed[0]) {
                            dismiss();
                            Navigation.findNavController(requireActivity(), R.id.controllerNavHost)
                                    .navigate(R.id.ordersFragment);
                        } else {
                            loadCart();
                        }
                    }
                }
            });
        }
    }

    private void saveOrder(String buyerId, CartItemAdapter.CartLine line, CheckoutCallback callback) {
        Product product = line.product;
        double price = product.getPrice() != null ? product.getPrice() : 0;
        Order order = new Order();
        order.setBuyer_id(buyerId);
        order.setSeller_id(product.getSeller_id());
        order.setProduct_id(product.getId());
        order.setProduct_title(product.getTitle());
        order.setProduct_image_url(firstImage(product));
        order.setQuantity(line.quantity());
        order.setUnit_price(price);
        order.setDiscount_amount(0d);
        order.setTotal_price(price * line.quantity());
        order.setStatus("pending");
        String now = CartFlow.nowIsoUtc();
        order.setCreated_at(now);
        order.setUpdated_at(now);

        orderService.save(order, result -> {
            if (!result.isSuccess() || result.getData() == null) {
                callback.onComplete(false);
                return;
            }
            saveOrderItem(result.getData(), line, callback);
        });
    }

    private void saveOrderItem(Order order, CartItemAdapter.CartLine line, CheckoutCallback callback) {
        Product product = line.product;
        OrderItem item = new OrderItem();
        item.setId(CartFlow.stableDocId("order_item", order.getId(), product.getId()));
        item.setOrder_id(order.getId());
        item.setProduct_id(product.getId());
        item.setSeller_id(product.getSeller_id());
        item.setPrice(product.getPrice() != null ? product.getPrice() : 0);
        item.setQuantity(line.quantity());

        orderItemService.save(item, result -> {
            if (!result.isSuccess()) {
                callback.onComplete(false);
                return;
            }
            cartItemService.deleteById(line.item.getId(), new AsyncCrudService.BooleanCallback() {
                @Override
                public void onSuccess(boolean success) {
                    notifySellerNewOrder(order);
                    callback.onComplete(true);
                }

                @Override
                public void onError(String error) {
                    callback.onComplete(false);
                }
            });
        });
    }

    private void notifySellerNewOrder(Order order) {
        if (order == null || TextUtils.isEmpty(order.getSeller_id()) || TextUtils.isEmpty(order.getId())) {
            return;
        }
        FirebaseUser buyer = FirebaseAuth.getInstance().getCurrentUser();
        if (buyer != null && order.getSeller_id().equals(buyer.getUid())) {
            return;
        }

        Notification notification = new Notification();
        notification.setId(CartFlow.stableDocId("notif", order.getSeller_id(), order.getId(), "new_order"));
        notification.setUser_id(order.getSeller_id());
        notification.setTitle("Có đơn hàng mới");
        notification.setContent("Người mua vừa đặt \"" + safeTitle(order) + "\". Vào Đơn hàng để xác nhận.");
        notification.setType("order");
        notification.setTarget_id(order.getId());
        notification.setIs_read(false);
        notification.setCreated_at(CartFlow.nowIsoUtc());
        notificationService.save(notification, ignored -> {});
    }

    private boolean isPurchasable(Product product) {
        if (product == null || TextUtils.isEmpty(product.getId())) {
            return false;
        }
        String status = product.getStatus() != null ? product.getStatus().toLowerCase(Locale.ROOT) : "";
        return status.isEmpty() || status.equals("active") || status.equals("available");
    }

    private String firstImage(Product product) {
        if (product == null || product.getImage_urls() == null || product.getImage_urls().isEmpty()) {
            return null;
        }
        return product.getImage_urls().get(0);
    }

    private String safeTitle(Order order) {
        return order != null && !TextUtils.isEmpty(order.getProduct_title())
                ? order.getProduct_title()
                : "sản phẩm";
    }

    private void bindTotal() {
        double total = 0;
        for (CartItemAdapter.CartLine line : adapter.currentItems()) {
            total += line.total();
        }
        tvCartTotal.setText(HomeUiUtils.formatPrice(total));
    }

    private void showList() {
        rvCartItems.setVisibility(View.VISIBLE);
        layoutCartEmpty.setVisibility(View.GONE);
        layoutCartFooter.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        adapter.submitList(new ArrayList<>());
        tvCartTotal.setText(HomeUiUtils.formatPrice(0d));
        rvCartItems.setVisibility(View.GONE);
        layoutCartEmpty.setVisibility(View.VISIBLE);
        layoutCartFooter.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressCart.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvCartItems.setVisibility(loading ? View.GONE : rvCartItems.getVisibility());
    }

    private void setCheckoutLoading(boolean loading) {
        btnCartCheckout.setEnabled(!loading);
        btnCartCheckout.setAlpha(loading ? 0.65f : 1f);
        btnCartCheckout.setText(loading ? "Đang đặt..." : "Đặt hàng");
    }

    private interface CheckoutCallback {
        void onComplete(boolean success);
    }
}
