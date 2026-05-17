package com.example.unimarket.pages.profile;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrdersInProfileAdapter extends RecyclerView.Adapter<OrdersInProfileAdapter.VH> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    private final List<Order> items = new ArrayList<>();
    private final OnOrderClickListener listener;

    public OrdersInProfileAdapter(OnOrderClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Order> orders) {
        items.clear();
        if (orders != null) items.addAll(orders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class VH extends RecyclerView.ViewHolder {
        final TextView tvOrderId, tvOrderStatus, tvOrderProductTitle, tvOrderPrice;
        final ShapeableImageView ivOrderProductImage;

        VH(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderProductTitle = itemView.findViewById(R.id.tvOrderProductTitle);
            tvOrderPrice = itemView.findViewById(R.id.tvOrderPrice);
            ivOrderProductImage = itemView.findViewById(R.id.ivOrderProductImage);
        }

        void bind(Order order) {
            String shortId = order.getId() != null
                    ? "#UM" + order.getId().substring(0, Math.min(5, order.getId().length())).toUpperCase(Locale.ROOT)
                    : "#UMXXXXX";
            tvOrderId.setText("Đơn hàng " + shortId);
            tvOrderProductTitle.setText(!TextUtils.isEmpty(order.getProduct_title())
                    ? order.getProduct_title() : "Sản phẩm");
            tvOrderPrice.setText(HomeUiUtils.formatPrice(order.getTotal_price()));
            tvOrderStatus.setText(statusLabel(order.getStatus()));

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
            if (status == null) return "Đang xử lý";
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
}