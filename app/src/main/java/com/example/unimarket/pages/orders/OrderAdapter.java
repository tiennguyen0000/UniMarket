package com.example.unimarket.pages.orders;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
        private final TextView tvOrderId, tvOrderDate, tvOrderStatus, tvOrderProductTitle;
        private final TextView tvOrderQuantity, tvOrderUnitPrice, tvOrderDiscount, tvOrderPrice, tvOrderCta;
        private final LinearLayout layoutOrderDiscount;
        private final ShapeableImageView ivOrderProductImage;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderProductTitle = itemView.findViewById(R.id.tvOrderProductTitle);
            tvOrderQuantity = itemView.findViewById(R.id.tvOrderQuantity);
            tvOrderUnitPrice = itemView.findViewById(R.id.tvOrderUnitPrice);
            layoutOrderDiscount = itemView.findViewById(R.id.layoutOrderDiscount);
            tvOrderDiscount = itemView.findViewById(R.id.tvOrderDiscount);
            tvOrderPrice = itemView.findViewById(R.id.tvOrderPrice);
            tvOrderCta = itemView.findViewById(R.id.tvOrderCta);
            ivOrderProductImage = itemView.findViewById(R.id.ivOrderProductImage);
        }

        void bind(Order order) {
            tvOrderId.setText("Đơn hàng " + OrderUiFormatter.shortOrderId(order.getId()));
            tvOrderDate.setText(OrderUiFormatter.formatCreatedAt(order.getCreated_at()));
            tvOrderProductTitle.setText(!TextUtils.isEmpty(order.getProduct_title())
                    ? order.getProduct_title() : "Sản phẩm");
            tvOrderPrice.setText(HomeUiUtils.formatPrice(order.getTotal_price()));
            tvOrderQuantity.setText("Số lượng: " + safeQuantity(order));
            tvOrderUnitPrice.setText("Đơn giá: " + HomeUiUtils.formatPrice(order.getUnit_price()));

            double discount = order.getDiscount_amount() != null ? order.getDiscount_amount() : 0;
            if (discount > 0 || !TextUtils.isEmpty(order.getDiscount_code())) {
                layoutOrderDiscount.setVisibility(View.VISIBLE);
                String discountText = discount > 0 ? "-" + HomeUiUtils.formatPrice(discount) : "Đã áp dụng";
                if (!TextUtils.isEmpty(order.getDiscount_code())) {
                    discountText = order.getDiscount_code() + " · " + discountText;
                }
                tvOrderDiscount.setText(discountText);
            } else {
                layoutOrderDiscount.setVisibility(View.GONE);
            }

            String status = order.getStatus() != null ? order.getStatus() : "pending";
            tvOrderStatus.setText(OrderUiFormatter.statusLabel(status));
            tvOrderStatus.setTextColor(OrderUiFormatter.statusTextColor(status));
            tvOrderStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(OrderUiFormatter.statusBgColor(status)));
            tvOrderCta.setText(OrderUiFormatter.ctaLabel(status));

            if (!TextUtils.isEmpty(order.getProduct_image_url())) {
                Glide.with(itemView.getContext())
                        .load(order.getProduct_image_url())
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_placeholder)
                        .error(R.drawable.ic_user_placeholder)
                        .into(ivOrderProductImage);
            } else {
                ivOrderProductImage.setImageResource(R.drawable.ic_user_placeholder);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOrderClick(order);
            });
            tvOrderCta.setOnClickListener(v -> {
                if (listener != null) listener.onOrderClick(order);
            });
        }

        private int safeQuantity(Order order) {
            return order.getQuantity() != null ? Math.max(1, order.getQuantity()) : 1;
        }
    }
}
