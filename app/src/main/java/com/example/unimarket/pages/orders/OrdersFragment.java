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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.DomainConstants;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.pages.home.HomeUiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Locale;

public class OrdersFragment extends Fragment {

    private RecyclerView rvOrders;
    private LinearLayout layoutEmpty;
    private View layoutOrdersLoading;
    private TextView chipAll, chipPending, chipShipping, chipDone, chipCancelled;
    private TextView tvPendingCount, tvShippingCount, tvDoneCount, tvCancelledCount;
    private TextView tvOrdersEmptyTitle, tvOrdersEmptyMessage;
    private boolean ordersLoading = false;
    private String selectedFilter = OrderUiFormatter.FILTER_ALL;

    private OrdersViewModel viewModel;
    private OrderAdapter adapter;

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
        chipAll = view.findViewById(R.id.chipAll);
        chipPending = view.findViewById(R.id.chipPending);
        chipShipping = view.findViewById(R.id.chipShipping);
        chipDone = view.findViewById(R.id.chipDone);
        chipCancelled = view.findViewById(R.id.chipCancelled);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvShippingCount = view.findViewById(R.id.tvShippingCount);
        tvDoneCount = view.findViewById(R.id.tvDoneCount);
        tvCancelledCount = view.findViewById(R.id.tvCancelledCount);
        tvOrdersEmptyTitle = view.findViewById(R.id.tvOrdersEmptyTitle);
        tvOrdersEmptyMessage = view.findViewById(R.id.tvOrdersEmptyMessage);

        viewModel = new ViewModelProvider(this).get(OrdersViewModel.class);

        adapter = new OrderAdapter(this::showOrderDetailDialog);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrders.setAdapter(adapter);
        rvOrders.setNestedScrollingEnabled(false);

        observeViewModel();
        setupFilterChips();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) viewModel.loadOrders(user.getUid());
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

    private void observeViewModel() {
        viewModel.getAllOrders().observe(getViewLifecycleOwner(), orders ->
                renderOrders());

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            ordersLoading = Boolean.TRUE.equals(loading);
            renderOrders();
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> selectFilter(OrderUiFormatter.FILTER_ALL));
        chipPending.setOnClickListener(v -> selectFilter(OrderUiFormatter.FILTER_PROCESSING));
        chipShipping.setOnClickListener(v -> selectFilter(OrderUiFormatter.FILTER_SHIPPING));
        chipDone.setOnClickListener(v -> selectFilter(OrderUiFormatter.FILTER_DONE));
        chipCancelled.setOnClickListener(v -> selectFilter(OrderUiFormatter.FILTER_CANCELLED));
        updateFilterChips();
    }

    private void selectFilter(String filter) {
        selectedFilter = filter;
        renderOrders();
    }

    private void renderOrders() {
        updateSummaryCounts();
        updateFilterChips();

        List<Order> filtered = viewModel.filterByStatus(selectedFilter);
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
        bindEmptyState(empty);
    }

    private void updateSummaryCounts() {
        int all = viewModel.countByFilter(OrderUiFormatter.FILTER_ALL);
        int processing = viewModel.countByFilter(OrderUiFormatter.FILTER_PROCESSING);
        int shipping = viewModel.countByFilter(OrderUiFormatter.FILTER_SHIPPING);
        int done = viewModel.countByFilter(OrderUiFormatter.FILTER_DONE);
        int cancelled = viewModel.countByFilter(OrderUiFormatter.FILTER_CANCELLED);

        chipAll.setText("Tất cả " + all);
        chipPending.setText("Đang xử lý " + processing);
        chipShipping.setText("Đang giao " + shipping);
        chipDone.setText("Hoàn thành " + done);
        chipCancelled.setText("Đã hủy " + cancelled);

        tvPendingCount.setText(String.valueOf(processing));
        tvShippingCount.setText(String.valueOf(shipping));
        tvDoneCount.setText(String.valueOf(done));
        tvCancelledCount.setText(String.valueOf(cancelled));
    }

    private void updateFilterChips() {
        bindChip(chipAll, OrderUiFormatter.FILTER_ALL);
        bindChip(chipPending, OrderUiFormatter.FILTER_PROCESSING);
        bindChip(chipShipping, OrderUiFormatter.FILTER_SHIPPING);
        bindChip(chipDone, OrderUiFormatter.FILTER_DONE);
        bindChip(chipCancelled, OrderUiFormatter.FILTER_CANCELLED);
    }

    private void bindChip(TextView chip, String filter) {
        boolean selected = filter.equals(selectedFilter);
        chip.setBackgroundResource(selected
                ? R.drawable.bg_order_filter_chip_selected
                : R.drawable.bg_order_filter_chip);
        chip.setTextColor(selected ? 0xFFFFFFFF : 0xFF667085);
    }

    private void bindEmptyState(boolean empty) {
        if (!empty || tvOrdersEmptyTitle == null || tvOrdersEmptyMessage == null) {
            return;
        }

        if (OrderUiFormatter.FILTER_ALL.equals(selectedFilter)) {
            tvOrdersEmptyTitle.setText("Chưa có đơn hàng nào");
            tvOrdersEmptyMessage.setText("Hãy khám phá và đặt mua sản phẩm đầu tiên của bạn!");
        } else if (OrderUiFormatter.FILTER_PROCESSING.equals(selectedFilter)) {
            tvOrdersEmptyTitle.setText("Không có đơn đang xử lý");
            tvOrdersEmptyMessage.setText("Các đơn chờ xác nhận hoặc đã xác nhận sẽ xuất hiện tại đây.");
        } else if (OrderUiFormatter.FILTER_SHIPPING.equals(selectedFilter)) {
            tvOrdersEmptyTitle.setText("Không có đơn đang giao");
            tvOrdersEmptyMessage.setText("Khi người bán bắt đầu giao hàng, đơn sẽ được chuyển vào mục này.");
        } else if (OrderUiFormatter.FILTER_DONE.equals(selectedFilter)) {
            tvOrdersEmptyTitle.setText("Chưa có đơn hoàn thành");
            tvOrdersEmptyMessage.setText("Các giao dịch đã hoàn tất sẽ được lưu lại để bạn xem và đánh giá.");
        } else {
            tvOrdersEmptyTitle.setText("Không có đơn đã hủy");
            tvOrdersEmptyMessage.setText("Những đơn đã hủy sẽ xuất hiện tại đây để bạn dễ kiểm tra lại.");
        }
    }

    private void showOrderDetailDialog(Order order) {
        if (order == null) {
            return;
        }

        String orderId = !TextUtils.isEmpty(order.getId())
                ? "#UM" + order.getId().substring(0, Math.min(6, order.getId().length())).toUpperCase(Locale.ROOT)
                : "#UM";
        int quantity = order.getQuantity() != null ? order.getQuantity() : 1;
        double unitPrice = order.getUnit_price() != null ? order.getUnit_price() : 0;
        double discount = order.getDiscount_amount() != null ? order.getDiscount_amount() : 0;

        StringBuilder message = new StringBuilder();
        message.append("Sản phẩm: ")
                .append(!TextUtils.isEmpty(order.getProduct_title()) ? order.getProduct_title() : "Sản phẩm")
                .append("\n");
        message.append("Mã đơn: ").append(OrderUiFormatter.shortOrderId(order.getId())).append("\n");
        message.append("Ngày tạo: ").append(OrderUiFormatter.formatCreatedAt(order.getCreated_at())).append("\n");
        message.append("Trạng thái: ").append(OrderUiFormatter.statusLabel(order.getStatus())).append("\n");
        message.append("Số lượng: ").append(quantity).append("\n");
        if (unitPrice > 0) {
            message.append("Đơn giá: ").append(HomeUiUtils.formatPrice(unitPrice)).append("\n");
        }
        if (!TextUtils.isEmpty(order.getDiscount_code()) || discount > 0) {
            message.append("Mã giảm giá: ")
                    .append(!TextUtils.isEmpty(order.getDiscount_code()) ? order.getDiscount_code() : "Đã áp dụng")
                    .append("\n");
            message.append("Giảm: ").append(HomeUiUtils.formatPrice(discount)).append("\n");
        }
        message.append("Tổng thanh toán: ").append(HomeUiUtils.formatPrice(order.getTotal_price()));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chi tiết đơn " + orderId)
                .setMessage(message.toString())
                .setNegativeButton("Đóng", null);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String nextStatus = currentUser != null
                ? viewModel.nextActionForUser(order, currentUser.getUid())
                : null;
        if (!TextUtils.isEmpty(nextStatus)) {
            builder.setPositiveButton(actionLabel(nextStatus), (dialog, which) ->
                    viewModel.updateOrderStatus(order.getId(), currentUser.getUid(), nextStatus));
        }
        builder.show();
    }

    private String actionLabel(String nextStatus) {
        if (DomainConstants.OrderStatus.CONFIRMED.equals(nextStatus)) return "Xác nhận";
        if (DomainConstants.OrderStatus.SHIPPING.equals(nextStatus)) return "Bắt đầu giao";
        if (DomainConstants.OrderStatus.DONE.equals(nextStatus)) return "Hoàn thành";
        if (DomainConstants.OrderStatus.CANCELLED.equals(nextStatus)) return "Hủy đơn";
        return "Cập nhật";
    }
}
