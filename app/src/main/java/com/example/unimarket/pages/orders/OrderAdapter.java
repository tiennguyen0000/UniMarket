package com.example.unimarket.pages.orders;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.pages.home.HomeUiUtils;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    private final List<Order> items = new ArrayList<>();
    private final OnOrderClickListener listener;

    public OrderAdapter(OnOrderClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Order> orders) {
        items.clear();
        if (orders != null) items.addAll(orders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOrderId, tvOrderStatus, tvOrderProductTitle, tvOrderPrice;
        private final ShapeableImageView ivOrderProductImage;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderProductTitle = itemView.findViewById(R.id.tvOrderProductTitle);
            tvOrderPrice = itemView.findViewById(R.id.tvOrderPrice);
            ivOrderProductImage = itemView.findViewById(R.id.ivOrderProductImage);
        }

        void bind(Order order) {
            String shortId = order.getId() != null
                    ? "#UM" + order.getId().substring(0, Math.min(5, order.getId().length())).toUpperCase()
                    : "#UMXXXXX";
            tvOrderId.setText("Đơn hàng " + shortId);
            tvOrderProductTitle.setText(!TextUtils.isEmpty(order.getProduct_title())
                    ? order.getProduct_title() : "Sản phẩm");
            tvOrderPrice.setText(HomeUiUtils.formatPrice(order.getTotal_price()));

            // Status chip
            String status = order.getStatus() != null ? order.getStatus() : "pending";
            tvOrderStatus.setText(statusLabel(status));
            tvOrderStatus.setTextColor(statusTextColor(status));
            tvOrderStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(statusBgColor(status)));

            // Product image
            if (!TextUtils.isEmpty(order.getProduct_image_url())) {
                Glide.with(itemView.getContext())
                        .load(order.getProduct_image_url())
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_placeholder)
                        .into(ivOrderProductImage);
            } else {
                ivOrderProductImage.setImageResource(R.drawable.ic_user_placeholder);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOrderClick(order);
            });
        }

        private String statusLabel(String status) {
            switch (status.toLowerCase()) {
                case "pending":    return "Chờ xác nhận";
                case "confirmed":  return "Đã xác nhận";
                case "shipping":   return "Đang giao";
                case "done":       return "Hoàn thành";
                case "cancelled":  return "Đã hủy";
                default:           return status;
            }
        }

        private int statusTextColor(String status) {
            switch (status.toLowerCase()) {
                case "done":       return 0xFF027A48;
                case "shipping":   return 0xFF175CD3;
                case "cancelled":  return 0xFFB42318;
                default:           return 0xFF344054;
            }
        }

        private int statusBgColor(String status) {
            switch (status.toLowerCase()) {
                case "done":       return 0xFFECFDF3;
                case "shipping":   return 0xFFEFF8FF;
                case "cancelled":  return 0xFFFEF3F2;
                default:           return 0xFFF2F4F7;
            }
        }
    }
}
