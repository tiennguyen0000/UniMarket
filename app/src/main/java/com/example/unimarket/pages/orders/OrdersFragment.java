package com.example.unimarket.pages.orders;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.pages.chat.ChatBottomSheetFragment;
import com.example.unimarket.pages.home.CartBottomSheetFragment;
import com.example.unimarket.pages.home.CartFlow;
import com.example.unimarket.pages.home.HomeUiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class OrdersFragment extends Fragment {

    private static final String[] STATUS_FILTERS = { null, "pending", "confirmed", "shipping", "done", "cancelled" };

    private RecyclerView rvOrders;
    private LinearLayout layoutEmpty;
    private View layoutOrdersLoading;
    private TextView tvOrdersEmptyTitle;
    private TextView tvOrdersEmptyMessage;
    private View btnOrdersExplore;
    private TextView tvOrderFilterSummary;
    private TextView btnModeBuy;
    private TextView btnModeSell;
    private final List<TextView> statusButtons = new ArrayList<>();

    private OrdersViewModel viewModel;
    private OrderAdapter adapter;
    private boolean ordersLoading;
    private boolean sellerMode;
    private int selectedStatusIndex;

    private final OrderService orderService = new OrderService();
    private final ProductService productService = new ProductService();
    private final UserService userService = new UserService();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupLightSystemBars();

        rvOrders = view.findViewById(R.id.rvOrders);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutOrdersLoading = view.findViewById(R.id.layoutOrdersLoading);
        tvOrdersEmptyTitle = view.findViewById(R.id.tvOrdersEmptyTitle);
        tvOrdersEmptyMessage = view.findViewById(R.id.tvOrdersEmptyMessage);
        btnOrdersExplore = view.findViewById(R.id.btnOrdersExplore);
        tvOrderFilterSummary = view.findViewById(R.id.tvOrderFilterSummary);
        btnModeBuy = view.findViewById(R.id.btnModeBuy);
        btnModeSell = view.findViewById(R.id.btnModeSell);

        statusButtons.add(view.findViewById(R.id.btnStatusAll));
        statusButtons.add(view.findViewById(R.id.btnStatusPending));
        statusButtons.add(view.findViewById(R.id.btnStatusConfirmed));
        statusButtons.add(view.findViewById(R.id.btnStatusShipping));
        statusButtons.add(view.findViewById(R.id.btnStatusDone));
        statusButtons.add(view.findViewById(R.id.btnStatusCancelled));

        viewModel = new ViewModelProvider(this).get(OrdersViewModel.class);
        adapter = new OrderAdapter(new OrderAdapter.Listener() {
            @Override public void onOpen(Order order) { showOrderDetailDialog(order); }
            @Override public void onPrimary(Order order) { handlePrimaryAction(order); }
            @Override public void onContact(Order order) { openOrderChat(order); }
            @Override public void onProduct(Order order) { openProductInSearch(order); }
        });

        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrders.setAdapter(adapter);
        rvOrders.setNestedScrollingEnabled(false);

        setupSelectors();
        observeViewModel();
        reloadOrders();

        btnOrdersExplore.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.searchFragment));
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadOrders();
    }

    private void setupSelectors() {
        btnModeBuy.setOnClickListener(v -> selectMode(false));
        btnModeSell.setOnClickListener(v -> selectMode(true));

        for (int i = 0; i < statusButtons.size(); i++) {
            final int index = i;
            statusButtons.get(i).setOnClickListener(v -> selectStatus(index));
        }

        selectMode(false);
        selectStatus(0);
    }

    private void selectMode(boolean sellerMode) {
        this.sellerMode = sellerMode;
        adapter.setSellerMode(sellerMode);

        styleModeButton(btnModeBuy, !sellerMode);
        styleModeButton(btnModeSell, sellerMode);
        updateFilterSummary();
        filterAndDisplay();
    }

    private void selectStatus(int index) {
        selectedStatusIndex = index;
        for (int i = 0; i < statusButtons.size(); i++) {
            styleStatusButton(statusButtons.get(i), i == index);
        }
        updateFilterSummary();
        filterAndDisplay();
    }

    private void styleModeButton(TextView view, boolean selected) {
        view.setBackgroundResource(selected
                ? R.drawable.bg_orders_mode_selected
                : R.drawable.bg_orders_mode_idle);
        view.setTextColor(selected ? Color.WHITE : 0xFF6B7280);
    }

    private void styleStatusButton(TextView view, boolean selected) {
        view.setBackgroundResource(selected
                ? R.drawable.bg_orders_status_selected
                : R.drawable.bg_orders_status_idle);
        view.setTextColor(selected ? 0xFF1A428A : 0xFF667085);
    }

    private void updateFilterSummary() {
        if (tvOrderFilterSummary == null) return;
        String mode = sellerMode ? "Đơn bán" : "Đơn mua";
        String status = statusLabelForSelector(selectedStatusIndex);
        tvOrderFilterSummary.setText(selectedStatusIndex == 0 ? mode : mode + " · " + status);
    }

    private String statusLabelForSelector(int index) {
        switch (index) {
            case 1: return "Chờ";
            case 2: return "Đã nhận";
            case 3: return "Giao";
            case 4: return "Xong";
            case 5: return "Hủy";
            default: return "Tất cả";
        }
    }

    private void observeViewModel() {
        viewModel.getBuyerOrders().observe(getViewLifecycleOwner(), orders -> filterAndDisplay());
        viewModel.getSellerOrders().observe(getViewLifecycleOwner(), orders -> filterAndDisplay());
        viewModel.getCurrentProfile().observe(getViewLifecycleOwner(), profile -> filterAndDisplay());
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            ordersLoading = Boolean.TRUE.equals(loading);
            filterAndDisplay();
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (!TextUtils.isEmpty(error) && isAdded()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void reloadOrders() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) viewModel.loadOrders(user.getUid());
    }

    private void filterAndDisplay() {
        String statusFilter = selectedStatusIndex >= 0 && selectedStatusIndex < STATUS_FILTERS.length
                ? STATUS_FILTERS[selectedStatusIndex]
                : null;
        List<Order> filtered = viewModel.ordersForMode(sellerMode, statusFilter);
        adapter.setSellerMode(sellerMode);
        adapter.submitList(filtered);

        if (ordersLoading) {
            layoutOrdersLoading.setVisibility(View.VISIBLE);
            rvOrders.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            return;
        }

        layoutOrdersLoading.setVisibility(View.GONE);
        boolean empty = filtered.isEmpty();
        rvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) updateEmptyCopy(statusFilter);
    }

    private void updateEmptyCopy(String statusFilter) {
        if (sellerMode) {
            tvOrdersEmptyTitle.setText(TextUtils.isEmpty(statusFilter)
                    ? "Chưa có đơn bán"
                    : "Không có đơn bán ở mục này");
            tvOrdersEmptyMessage.setText("Khi người mua đặt sản phẩm của bạn, đơn sẽ xuất hiện tại đây.");
            btnOrdersExplore.setVisibility(View.GONE);
        } else {
            tvOrdersEmptyTitle.setText(TextUtils.isEmpty(statusFilter)
                    ? "Chưa có đơn mua"
                    : "Không có đơn mua ở mục này");
            tvOrdersEmptyMessage.setText("Khám phá sản phẩm quanh campus và đặt mua món đầu tiên của bạn.");
            btnOrdersExplore.setVisibility(View.VISIBLE);
        }
    }

    private void handlePrimaryAction(Order order) {
        if (order == null) return;
        String status = safeStatus(order);
        if (sellerMode) {
            if ("pending".equals(status)) updateOrderStatus(order, "confirmed");
            else if ("confirmed".equals(status)) updateOrderStatus(order, "shipping");
            return;
        }

        if ("pending".equals(status)) updateOrderStatus(order, "cancelled");
        else if ("shipping".equals(status)) updateOrderStatus(order, "done");
        else if ("cancelled".equals(status)) buyAgain(order);
    }

    private void showOrderDetailDialog(Order order) {
        if (order == null) return;

        StringBuilder message = new StringBuilder();
        message.append("Sản phẩm: ").append(safeTitle(order)).append("\n");
        message.append("Vai trò: ").append(sellerMode ? "Người bán" : "Người mua").append("\n");
        message.append("Trạng thái: ").append(statusLabel(safeStatus(order))).append("\n");
        message.append("Số lượng: ").append(order.getQuantity() != null ? order.getQuantity() : 1).append("\n");
        if (order.getUnit_price() != null && order.getUnit_price() > 0) {
            message.append("Đơn giá: ").append(HomeUiUtils.formatPrice(order.getUnit_price())).append("\n");
        }
        if (!TextUtils.isEmpty(order.getDiscount_code()) || safeDouble(order.getDiscount_amount()) > 0) {
            message.append("Mã giảm: ")
                    .append(!TextUtils.isEmpty(order.getDiscount_code()) ? order.getDiscount_code() : "Đã áp dụng")
                    .append("\n");
            message.append("Giảm: ").append(HomeUiUtils.formatPrice(order.getDiscount_amount())).append("\n");
        }
        message.append("Tổng thanh toán: ").append(HomeUiUtils.formatPrice(order.getTotal_price()));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chi tiết " + shortOrderId(order))
                .setMessage(message.toString())
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Liên hệ", (dialog, which) -> openOrderChat(order));

        String status = safeStatus(order);
        if (sellerMode && "pending".equals(status)) {
            builder.setNeutralButton("Từ chối", (dialog, which) -> updateOrderStatus(order, "cancelled"));
        } else if (sellerMode && "confirmed".equals(status)) {
            builder.setNeutralButton("Hủy đơn", (dialog, which) -> updateOrderStatus(order, "cancelled"));
        } else if (!sellerMode && "cancelled".equals(status)) {
            builder.setNeutralButton("Mua lại", (dialog, which) -> buyAgain(order));
        }
        builder.show();
    }

    private void updateOrderStatus(Order order, String newStatus) {
        String oldStatus = safeStatus(order);
        order.setStatus(newStatus);
        order.setUpdated_at(nowIsoUtc());
        orderService.save(order, result -> {
            if (!isAdded()) return;
            if (!result.isSuccess()) {
                order.setStatus(oldStatus);
                Toast.makeText(requireContext(), "Không thể cập nhật đơn hàng.", Toast.LENGTH_SHORT).show();
                return;
            }
            afterOrderStatusChanged(order, oldStatus, newStatus);
        });
    }

    private void afterOrderStatusChanged(Order order, String oldStatus, String newStatus) {
        if (sellerMode && "confirmed".equals(newStatus)) {
            setProductStatus(order, "sold");
            return;
        }
        if (sellerMode && "cancelled".equals(newStatus) && !"pending".equals(oldStatus)) {
            setProductStatus(order, "active");
            return;
        }
        reloadOrders();
    }

    private void setProductStatus(Order order, String status) {
        fetchProduct(order, new ProductCallback() {
            @Override
            public void onSuccess(Product product) {
                product.setStatus(status);
                product.setUpdated_at(nowIsoUtc());
                productService.save(product, ignored -> {
                    if (isAdded()) reloadOrders();
                });
            }

            @Override
            public void onError(String message) {
                if (isAdded()) reloadOrders();
            }
        });
    }

    private void buyAgain(Order order) {
        fetchProduct(order, new ProductCallback() {
            @Override
            public void onSuccess(Product product) {
                int quantity = order.getQuantity() != null ? order.getQuantity() : 1;
                new CartFlow().add(product, quantity, new CartFlow.Callback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) return;
                        CartBottomSheetFragment.newInstance()
                                .show(requireActivity().getSupportFragmentManager(), "cart_from_order");
                    }

                    @Override
                    public void onError(String message) {
                        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openOrderChat(Order order) {
        fetchProduct(order, new ProductCallback() {
            @Override
            public void onSuccess(Product product) {
                loadName(order.getBuyer_id(), "Người mua", buyerName ->
                        loadName(order.getSeller_id(), "Người bán", sellerName -> {
                            if (!isAdded()) return;
                            ChatBottomSheetFragment.newProductChat(
                                    product,
                                    firstImage(product, order),
                                    order.getBuyer_id(),
                                    buyerName,
                                    sellerName
                            ).show(getParentFragmentManager(), "order_chat");
                        }));
            }

            @Override
            public void onError(String message) {
                if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openProductInSearch(Order order) {
        if (order == null || TextUtils.isEmpty(order.getProduct_id())) return;
        Bundle args = new Bundle();
        args.putString("product_id", order.getProduct_id());
        NavHostFragment.findNavController(this).navigate(R.id.searchFragment, args);
    }

    private void fetchProduct(Order order, ProductCallback callback) {
        if (order == null || TextUtils.isEmpty(order.getProduct_id())) {
            callback.onError("Không tìm thấy sản phẩm.");
            return;
        }
        productService.getById(order.getProduct_id(), new AsyncCrudService.ItemCallback<Product>() {
            @Override
            public void onSuccess(Product product) {
                if (product == null) callback.onError("Không tìm thấy sản phẩm.");
                else callback.onSuccess(product);
            }

            @Override
            public void onError(String error) {
                callback.onError("Không tìm thấy sản phẩm.");
            }
        });
    }

    private void loadName(String userId, String fallback, NameCallback callback) {
        if (TextUtils.isEmpty(userId)) {
            callback.onName(fallback);
            return;
        }
        userService.getProfileById(userId, new AsyncCrudService.ItemCallback<User>() {
            @Override
            public void onSuccess(User user) {
                String name = user != null && !TextUtils.isEmpty(user.getFull_name())
                        ? user.getFull_name()
                        : fallback;
                callback.onName(name);
            }

            @Override
            public void onError(String error) {
                callback.onName(fallback);
            }
        });
    }

    private void setupLightSystemBars() {
        requireActivity().getWindow().setStatusBarColor(Color.WHITE);
        requireActivity().getWindow().setNavigationBarColor(Color.WHITE);
        int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        requireActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private String safeStatus(Order order) {
        return order != null && !TextUtils.isEmpty(order.getStatus())
                ? order.getStatus().toLowerCase(Locale.ROOT)
                : "pending";
    }

    private String statusLabel(String status) {
        switch (status.toLowerCase(Locale.ROOT)) {
            case "pending": return "Chờ xác nhận";
            case "confirmed": return "Đã xác nhận";
            case "shipping": return "Đang giao";
            case "done": return "Hoàn thành";
            case "cancelled": return "Đã hủy";
            default: return status;
        }
    }

    private String safeTitle(Order order) {
        return order != null && !TextUtils.isEmpty(order.getProduct_title())
                ? order.getProduct_title()
                : "Sản phẩm UniMarket";
    }

    private String shortOrderId(Order order) {
        String id = order != null ? order.getId() : null;
        if (TextUtils.isEmpty(id)) return "#UM";
        String compact = id.replaceAll("[^A-Za-z0-9]", "");
        if (compact.toLowerCase(Locale.ROOT).startsWith("order")) {
            compact = compact.substring(5);
        }
        if (TextUtils.isEmpty(compact)) return "#UM";
        return "#UM" + compact.substring(0, Math.min(6, compact.length())).toUpperCase(Locale.ROOT);
    }

    private double safeDouble(Double value) {
        return value != null ? value : 0d;
    }

    private String firstImage(Product product, Order order) {
        if (product != null && product.getImage_urls() != null && !product.getImage_urls().isEmpty()) {
            return product.getImage_urls().get(0);
        }
        return order != null ? order.getProduct_image_url() : null;
    }

    private String nowIsoUtc() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new java.util.Date());
    }

    private interface ProductCallback {
        void onSuccess(Product product);
        void onError(String message);
    }

    private interface NameCallback {
        void onName(String name);
    }
}
