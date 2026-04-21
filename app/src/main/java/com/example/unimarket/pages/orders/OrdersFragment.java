package com.example.unimarket.pages.orders;

import android.os.Bundle;
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
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class OrdersFragment extends Fragment {

    private RecyclerView rvOrders;
    private LinearLayout layoutEmpty;
    private TabLayout tabOrderStatus;

    private OrdersViewModel viewModel;
    private OrderAdapter adapter;

    // Tab thứ tự: 0=all, 1=pending, 2=shipping, 3=done
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

        rvOrders = view.findViewById(R.id.rvOrders);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        tabOrderStatus = view.findViewById(R.id.tabOrderStatus);

        viewModel = new ViewModelProvider(this).get(OrdersViewModel.class);

        adapter = new OrderAdapter(order -> {
            Toast.makeText(requireContext(),
                    "Chi tiết đơn: " + order.getProduct_title(), Toast.LENGTH_SHORT).show();
        });
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

    private void observeViewModel() {
        viewModel.getAllOrders().observe(getViewLifecycleOwner(), orders -> {
            // Khi dữ liệu mới về, re-render tab đang chọn
            filterAndDisplay(tabOrderStatus.getSelectedTabPosition());
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty())
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private void filterAndDisplay(int tabPosition) {
        String statusFilter = (tabPosition >= 0 && tabPosition < STATUS_FILTERS.length)
                ? STATUS_FILTERS[tabPosition] : null;
        List<Order> filtered = viewModel.filterByStatus(statusFilter);
        adapter.submitList(filtered);

        boolean empty = filtered.isEmpty();
        rvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
}
