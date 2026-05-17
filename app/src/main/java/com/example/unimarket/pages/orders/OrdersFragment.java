package com.example.unimarket.pages.orders;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.pages.home.HomeUiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Locale;

public class OrdersFragment extends Fragment {

    private RecyclerView rvOrders;
    private LinearLayout layoutEmpty;
    private View layoutOrdersLoading;
    private TabLayout tabOrderStatus;
    private boolean ordersLoading = false;

    private OrdersViewModel viewModel;
    private OrderAdapter adapter;

    private static final String[] STATUS_FILTERS = { null, "pending", "shipping", "done" };

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
        tabOrderStatus = view.findViewById(R.id.tabOrderStatus);

        viewModel = new ViewModelProvider(this).get(OrdersViewModel.class);

        adapter = new OrderAdapter(this::showOrderDetailDialog);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrders.setAdapter(adapter);
        rvOrders.setNestedScrollingEnabled(false);

        observeViewModel();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) viewModel.loadOrders(user.getUid());

        tabOrderStatus.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterAndDisplay(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
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

    private void observeViewModel() {
        viewModel.getAllOrders().observe(getViewLifecycleOwner(), orders ->
                filterAndDisplay(tabOrderStatus.getSelectedTabPosition()));

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            ordersLoading = Boolean.TRUE.equals(loading);
            filterAndDisplay(tabOrderStatus.getSelectedTabPosition());
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterAndDisplay(int tabPosition) {
        String statusFilter = (tabPosition >= 0 && tabPosition < STATUS_FILTERS.length)
                ? STATUS_FILTERS[tabPosition] : null;
        List<Order> filtered = viewModel.filterByStatus(statusFilter);
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
        message.append("Trạng thái: ").append(statusLabel(order.getStatus())).append("\n");
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

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chi tiết đơn " + orderId)
                .setMessage(message.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }

    private String statusLabel(String status) {
        if (TextUtils.isEmpty(status)) {
            return "Chờ xác nhận";
        }
        switch (status.toLowerCase(Locale.ROOT)) {
            case "pending": return "Chờ xác nhận";
            case "confirmed": return "Đã xác nhận";
            case "shipping": return "Đang giao";
            case "done": return "Hoàn thành";
            case "cancelled": return "Đã hủy";
            default: return status;
        }
    }
}