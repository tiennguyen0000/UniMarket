package com.example.unimarket.pages.home;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.data.model.CartItem;
import com.example.unimarket.data.model.Product;

import java.util.ArrayList;
import java.util.List;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.VH> {
    public interface Listener {
        void onQuantityChanged(CartLine line, int quantity);
        void onRemove(CartLine line);
    }

    public static class CartLine {
        public final CartItem item;
        public final Product product;

        public CartLine(CartItem item, Product product) {
            this.item = item;
            this.product = product;
        }

        public int quantity() {
            return item != null && item.getQuantity() != null ? item.getQuantity() : 1;
        }

        public double total() {
            double price = product != null && product.getPrice() != null ? product.getPrice() : 0;
            return price * quantity();
        }
    }

    private final List<CartLine> items = new ArrayList<>();
    private final Listener listener;

    public CartItemAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<CartLine> lines) {
        items.clear();
        if (lines != null) {
            items.addAll(lines);
        }
        notifyDataSetChanged();
    }

    public List<CartLine> currentItems() {
        return new ArrayList<>(items);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart_item, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class VH extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView title;
        private final TextView price;
        private final TextView quantity;
        private final TextView minus;
        private final TextView plus;
        private final TextView remove;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.ivCartProduct);
            title = itemView.findViewById(R.id.tvCartProductTitle);
            price = itemView.findViewById(R.id.tvCartProductPrice);
            quantity = itemView.findViewById(R.id.tvCartQuantity);
            minus = itemView.findViewById(R.id.btnCartMinus);
            plus = itemView.findViewById(R.id.btnCartPlus);
            remove = itemView.findViewById(R.id.btnCartRemove);
        }

        void bind(CartLine line) {
            Product product = line.product;
            title.setText(product != null && !TextUtils.isEmpty(product.getTitle()) ? product.getTitle() : "Sản phẩm");
            price.setText(HomeUiUtils.formatPrice(product != null ? product.getPrice() : 0d));
            quantity.setText(String.valueOf(line.quantity()));

            String imageUrl = firstImage(product);
            Glide.with(itemView.getContext())
                    .load(!TextUtils.isEmpty(imageUrl) ? imageUrl : R.drawable.ic_category_bag_24)
                    .centerCrop()
                    .placeholder(R.drawable.bg_light_grey_rounded)
                    .error(R.drawable.ic_category_bag_24)
                    .into(image);

            minus.setOnClickListener(v -> listener.onQuantityChanged(line, line.quantity() - 1));
            plus.setOnClickListener(v -> listener.onQuantityChanged(line, line.quantity() + 1));
            remove.setOnClickListener(v -> listener.onRemove(line));
        }

        private String firstImage(Product product) {
            if (product == null || product.getImage_urls() == null || product.getImage_urls().isEmpty()) {
                return null;
            }
            return product.getImage_urls().get(0);
        }
    }
}
