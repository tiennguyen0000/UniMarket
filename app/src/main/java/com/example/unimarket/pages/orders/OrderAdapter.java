package com.example.unimarket.pages.orders;

import android.content.res.ColorStateList;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface Listener {
        void onOpen(Order order);
        void onPrimary(Order order);
        void onContact(Order order);
    }

    private final List<Order> items = new ArrayList<>();
    private final Listener listener;
    private boolean sellerMode;

    public OrderAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Order> orders) {
        items.clear();
        if (orders != null) items.addAll(orders);
        notifyDataSetChanged();
    }

    public void setSellerMode(boolean sellerMode) {
        this.sellerMode = sellerMode;
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
    public int getItemCount() {
        return items.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOrderId;
        private final TextView tvOrderStatus;
        private final TextView tvOrderRole;
        private final TextView tvOrderProductTitle;
        private final TextView tvOrderMeta;
        private final TextView tvOrderHint;
        private final TextView tvOrderPrice;
        private final MaterialButton btnOrderPrimary;
        private final ShapeableImageView ivOrderProductImage;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderRole = itemView.findViewById(R.id.tvOrderRole);
            tvOrderProductTitle = itemView.findViewById(R.id.tvOrderProductTitle);
            tvOrderMeta = itemView.findViewById(R.id.tvOrderMeta);
            tvOrderHint = itemView.findViewById(R.id.tvOrderHint);
            tvOrderPrice = itemView.findViewById(R.id.tvOrderPrice);
            btnOrderPrimary = itemView.findViewById(R.id.btnOrderPrimary);
            ivOrderProductImage = itemView.findViewById(R.id.ivOrderProductImage);
        }

        void bind(Order order) {
            String status = order != null && order.getStatus() != null ? order.getStatus() : "pending";

            tvOrderId.setText("Đơn " + shortOrderId(order));
            tvOrderStatus.setText(statusLabel(status));
            tvOrderStatus.setTextColor(statusTextColor(status));
            tvOrderStatus.setBackgroundTintList(ColorStateList.valueOf(statusBgColor(status)));
            tvOrderRole.setText(sellerMode ? "Bạn là người bán" : "Bạn là người mua");

            tvOrderProductTitle.setText(safeTitle(order));
            tvOrderPrice.setText(HomeUiUtils.formatPrice(order != null ? order.getTotal_price() : 0d));
            tvOrderMeta.setText(metaText(order));
            tvOrderHint.setText(hintText(status));

            if (order != null && !TextUtils.isEmpty(order.getProduct_image_url())) {
                Glide.with(itemView.getContext())
                        .load(order.getProduct_image_url())
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_placeholder)
                        .into(ivOrderProductImage);
            } else {
                ivOrderProductImage.setImageResource(R.drawable.ic_user_placeholder);
            }

            bindPrimaryButton(order, status);
            itemView.setOnClickListener(null);
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onOpen(order);
                }
                return true;
            });
        }

        private void bindPrimaryButton(Order order, String status) {
            String label = primaryLabel(status);
            btnOrderPrimary.setVisibility(TextUtils.isEmpty(label) ? View.GONE : View.VISIBLE);
            btnOrderPrimary.setText(label);
            btnOrderPrimary.setOnClickListener(v -> {
                if (listener != null) listener.onPrimary(order);
            });
        }

        private String primaryLabel(String status) {
            String normalized = status.toLowerCase(Locale.ROOT);
            if (sellerMode) {
                if ("pending".equals(normalized)) return "Xác nhận";
                if ("confirmed".equals(normalized)) return "Giao hàng";
                return "";
            }
            if ("pending".equals(normalized)) return "Hủy đơn";
            if ("confirmed".equals(normalized)) return "Cập nhật";
            if ("shipping".equals(normalized)) return "Đã nhận";
            if ("cancelled".equals(normalized)) return "Mua lại";
            return "";
        }

        private String metaText(Order order) {
            int quantity = order != null && order.getQuantity() != null ? order.getQuantity() : 1;
            double unitPrice = order != null && order.getUnit_price() != null ? order.getUnit_price() : 0d;
            return quantity + " sản phẩm"
                    + (unitPrice > 0 ? " · " + HomeUiUtils.formatPrice(unitPrice) + "/món" : "");
        }

        private String hintText(String status) {
            String normalized = status.toLowerCase(Locale.ROOT);
            if (sellerMode) {
                switch (normalized) {
                    case "pending": return "Người mua đang chờ bạn xác nhận đơn.";
                    case "confirmed": return "Chuẩn bị hàng và bắt đầu giao khi sẵn sàng.";
                    case "shipping": return "Đơn đang giao, chờ người mua xác nhận đã nhận.";
                    case "done": return "Đơn đã hoàn tất.";
                    case "cancelled": return "Đơn đã hủy.";
                    default: return "";
                }
            }
            switch (normalized) {
                case "pending": return "Đơn đang chờ người bán xác nhận.";
                case "confirmed": return "Người bán đã nhận đơn và đang chuẩn bị.";
                case "shipping": return "Hãy xác nhận khi bạn đã nhận được hàng.";
                case "done": return "Giao dịch đã hoàn tất.";
                case "cancelled": return "Bạn có thể mua lại nếu sản phẩm còn bán.";
                default: return "";
            }
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

        private int statusTextColor(String status) {
            switch (status.toLowerCase(Locale.ROOT)) {
                case "done": return 0xFF027A48;
                case "shipping": return 0xFF175CD3;
                case "cancelled": return 0xFFB42318;
                case "confirmed": return 0xFF854A0E;
                default: return 0xFF344054;
            }
        }

        private int statusBgColor(String status) {
            switch (status.toLowerCase(Locale.ROOT)) {
                case "done": return 0xFFECFDF3;
                case "shipping": return 0xFFEFF8FF;
                case "cancelled": return 0xFFFEF3F2;
                case "confirmed": return 0xFFFFF6E5;
                default: return 0xFFF2F4F7;
            }
        }
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

    private String safeTitle(Order order) {
        return order != null && !TextUtils.isEmpty(order.getProduct_title())
                ? order.getProduct_title()
                : "Sản phẩm UniMarket";
    }
}
