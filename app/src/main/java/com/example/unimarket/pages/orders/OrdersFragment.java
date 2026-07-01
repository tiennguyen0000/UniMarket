package com.example.unimarket.pages.orders;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
        prepareStatusButtons();

        btnModeBuy.setOnClickListener(v -> selectMode(false));
        btnModeSell.setOnClickListener(v -> selectMode(true));

        for (int i = 0; i < statusButtons.size(); i++) {
            final int index = i;
            statusButtons.get(i).setOnClickListener(v -> selectStatus(index));
        }

        selectMode(false);
        selectStatus(0);
    }

    private void prepareStatusButtons() {
        for (TextView button : statusButtons) {
            ViewGroup.LayoutParams params = button.getLayoutParams();
            if (params != null) {
                params.height = dpToPx(56);
                button.setLayoutParams(params);
            }
            button.setGravity(android.view.Gravity.CENTER);
            button.setBackgroundColor(Color.TRANSPARENT);
            button.setCompoundDrawablePadding(dpToPx(4));
            button.setPadding(dpToPx(14), 0, dpToPx(14), 0);
        }
        if (statusButtons.size() > 3) {
            statusButtons.get(3).setText("Đang giao");
            statusButtons.get(3).setMinWidth(dpToPx(76));
        }
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
        int color = selected ? 0xFF21409A : 0xFF667085;
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setTextColor(color);
        view.setCompoundDrawables(null, tintedStatusIcon(view, color), null, null);
    }

    private Drawable tintedStatusIcon(TextView view, int color) {
        Drawable drawable = requireContext().getDrawable(statusIconRes(view.getId()));
        if (drawable == null) {
            return null;
        }
        drawable = drawable.mutate();
        drawable.setTint(color);
        drawable.setBounds(0, 0, dpToPx(22), dpToPx(22));
        return drawable;
    }

    private int statusIconRes(int viewId) {
        if (viewId == R.id.btnStatusAll) return R.drawable.all;
        if (viewId == R.id.btnStatusPending) return R.drawable.pending;
        if (viewId == R.id.btnStatusConfirmed) return R.drawable.checkinbox;
        if (viewId == R.id.btnStatusShipping) return R.drawable.shipping;
        if (viewId == R.id.btnStatusDone) return R.drawable.get;
        if (viewId == R.id.btnStatusCancelled) return R.drawable.cancel;
        return R.drawable.order;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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
            case 3: return "Đang giao";
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
        else if ("confirmed".equals(status)) showUpdateOrderDialog(order);
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
        message.append("Tạm tính: ").append(money(
                order.getSubtotal_price() != null ? order.getSubtotal_price() : safeDouble(order.getUnit_price()) * (order.getQuantity() != null ? order.getQuantity() : 1)
        )).append("\n");
        message.append("Phí ship: ").append(money(safeDouble(order.getShipping_fee()))).append("\n");
        message.append("Phương thức ship: ").append(shippingLabel(order.getShipping_method())).append("\n");
        if (!TextUtils.isEmpty(order.getBuyer_phone())) {
            message.append("SĐT nhận hàng: ").append(order.getBuyer_phone()).append("\n");
        }
        if (!TextUtils.isEmpty(order.getDelivery_location())) {
            message.append("Vị trí nhận hàng: ").append(order.getDelivery_location()).append("\n");
        }
        if (!TextUtils.isEmpty(order.getBuyer_note())) {
            message.append("Lời nhắc: ").append(order.getBuyer_note()).append("\n");
        }
        if (!TextUtils.isEmpty(order.getDiscount_code()) || safeDouble(order.getDiscount_amount()) > 0) {
            message.append("Mã giảm: ")
                    .append(!TextUtils.isEmpty(order.getDiscount_code()) ? order.getDiscount_code() : "Đã áp dụng")
                    .append("\n");
            message.append("Giảm: ").append(money(safeDouble(order.getDiscount_amount()))).append("\n");
        }
        if (sellerMode) {
            double sellerAmount = order.getSeller_amount() != null
                    ? order.getSeller_amount()
                    : safeDouble(order.getSubtotal_price());
            message.append("Người bán nhận: ").append(money(sellerAmount)).append("\n");
        }
        message.append("Tổng thanh toán: ").append(money(safeDouble(order.getTotal_price())));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chi tiết " + shortOrderId(order))
                .setMessage(message.toString())
                .setNegativeButton("Xem tin", (dialog, which) -> openProductInSearch(order))
                .setPositiveButton("Liên hệ", (dialog, which) -> openOrderChat(order));

        String status = safeStatus(order);
        if (sellerMode && "pending".equals(status)) {
            builder.setNeutralButton("Từ chối", (dialog, which) -> updateOrderStatus(order, "cancelled"));
        } else if (sellerMode && "confirmed".equals(status)) {
            builder.setNeutralButton("Hủy đơn", (dialog, which) -> updateOrderStatus(order, "cancelled"));
        } else if (!sellerMode && canBuyerUpdate(order)) {
            builder.setNeutralButton("Cập nhật", (dialog, which) -> showUpdateOrderDialog(order));
        } else if (!sellerMode && "cancelled".equals(status)) {
            builder.setNeutralButton("Mua lại", (dialog, which) -> buyAgain(order));
        }
        builder.show();
    }

    private boolean canBuyerUpdate(Order order) {
        if (order == null) return false;
        String status = safeStatus(order);
        return "pending".equals(status) || "confirmed".equals(status);
    }

    private void showUpdateOrderDialog(Order order) {
        if (!canBuyerUpdate(order)) {
            Toast.makeText(requireContext(), "Đơn đã vào bước giao, không thể cập nhật.", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_order_update, null, false);
        sheet.setContentView(content);

        EditText phoneInput = content.findViewById(R.id.etOrderUpdatePhone);
        EditText locationInput = content.findViewById(R.id.etOrderUpdateLocation);
        EditText noteInput = content.findViewById(R.id.etOrderUpdateNote);
        View standard = content.findViewById(R.id.btnOrderUpdateStandard);
        View express = content.findViewById(R.id.btnOrderUpdateExpress);
        View close = content.findViewById(R.id.btnOrderUpdateClose);
        View save = content.findViewById(R.id.btnOrderUpdateSave);
        String[] selectedShipping = {
                "express".equalsIgnoreCase(order.getShipping_method()) ? "express" : "standard"
        };

        phoneInput.setText(!TextUtils.isEmpty(order.getBuyer_phone()) ? order.getBuyer_phone() : "");
        locationInput.setText(!TextUtils.isEmpty(order.getDelivery_location()) ? order.getDelivery_location() : "");
        noteInput.setText(!TextUtils.isEmpty(order.getBuyer_note()) ? order.getBuyer_note() : "");
        bindOrderUpdateShipping(content, selectedShipping[0]);
        standard.setOnClickListener(v -> {
            selectedShipping[0] = "standard";
            bindOrderUpdateShipping(content, selectedShipping[0]);
        });
        express.setOnClickListener(v -> {
            selectedShipping[0] = "express";
            bindOrderUpdateShipping(content, selectedShipping[0]);
        });
        close.setOnClickListener(v -> sheet.dismiss());
        save.setOnClickListener(v -> {
            saveBuyerOrderUpdate(
                    order,
                    phoneInput.getText() != null ? phoneInput.getText().toString().trim() : "",
                    locationInput.getText() != null ? locationInput.getText().toString().trim() : "",
                    selectedShipping[0],
                    noteInput.getText() != null ? noteInput.getText().toString().trim() : "");
            sheet.dismiss();
        });

        sheet.show();
    }
    private void bindOrderUpdateShipping(View root, String selectedMethod) {
        boolean standardSelected = !"express".equalsIgnoreCase(selectedMethod);
        bindOrderUpdateShippingOption(
                root.findViewById(R.id.ivOrderUpdateStandardIcon),
                root.findViewById(R.id.tvOrderUpdateStandardTitle),
                root.findViewById(R.id.tvOrderUpdateStandardDetail),
                root.findViewById(R.id.tvOrderUpdateStandardFee),
                standardSelected
        );
        bindOrderUpdateShippingOption(
                root.findViewById(R.id.ivOrderUpdateExpressIcon),
                root.findViewById(R.id.tvOrderUpdateExpressTitle),
                root.findViewById(R.id.tvOrderUpdateExpressDetail),
                root.findViewById(R.id.tvOrderUpdateExpressFee),
                !standardSelected
        );
    }

    private void bindOrderUpdateShippingOption(ImageView icon, TextView title, TextView detail,
                                               TextView fee, boolean selected) {
        int color = selected ? 0xFF21409A : 0xFF667085;
        icon.setColorFilter(color);
        title.setTextColor(color);
        detail.setTextColor(color);
        fee.setTextColor(color);
    }

    private void saveBuyerOrderUpdate(Order order, String phone, String location,
                                      String shippingMethod, String note) {
        if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(location)) {
            Toast.makeText(requireContext(), "Vui lòng nhập đủ SĐT và vị trí nhận hàng.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canBuyerUpdate(order)) {
            Toast.makeText(requireContext(), "Đơn đã vào bước giao, không thể cập nhật.", Toast.LENGTH_SHORT).show();
            return;
        }

        double newShippingFee = "express".equals(shippingMethod) ? 25000d : 10000d;
        double subtotal = order.getSubtotal_price() != null
                ? order.getSubtotal_price()
                : safeDouble(order.getUnit_price()) * (order.getQuantity() != null ? order.getQuantity() : 1);
        double discount = safeDouble(order.getDiscount_amount());

        order.setBuyer_phone(phone);
        order.setDelivery_location(location);
        order.setShipping_method(shippingMethod);
        order.setShipping_fee(newShippingFee);
        order.setBuyer_note(note);
        order.setTotal_price(Math.max(0d, subtotal + newShippingFee - discount));
        order.setUpdated_at(nowIsoUtc());

        orderService.save(order, result -> {
            if (!isAdded()) return;
            if (result.isSuccess()) {
                Toast.makeText(requireContext(), "Đã cập nhật đơn hàng.", Toast.LENGTH_SHORT).show();
                reloadOrders();
            } else {
                Toast.makeText(requireContext(), "Không thể cập nhật đơn hàng.", Toast.LENGTH_SHORT).show();
            }
        });
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
        if ("done".equals(newStatus) && !"done".equals(oldStatus)) {
            decrementProductQuantity(order);
            return;
        }
        reloadOrders();
    }

    private void decrementProductQuantity(Order order) {
        fetchProduct(order, new ProductCallback() {
            @Override
            public void onSuccess(Product product) {
                int currentQuantity = product.getQuantity() != null ? Math.max(0, product.getQuantity()) : 1;
                int orderedQuantity = order.getQuantity() != null ? Math.max(1, order.getQuantity()) : 1;
                int remaining = Math.max(0, currentQuantity - orderedQuantity);
                product.setQuantity(remaining);
                product.setStatus(remaining > 0 ? "active" : "hidden");
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

    private String shippingLabel(String method) {
        if ("express".equalsIgnoreCase(method)) return "Hỏa tốc";
        if ("standard".equalsIgnoreCase(method)) return "Giao thường";
        return TextUtils.isEmpty(method) ? "Chưa ghi nhận" : method;
    }

    private String money(double amount) {
        return amount <= 0d ? "0đ" : HomeUiUtils.formatPrice(amount);
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
