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
import com.example.unimarket.data.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    private final List<Product> items = new ArrayList<>();
    private final Map<String, String> categoryNameMap = new HashMap<>();
    private final Map<String, String> productImageMap = new HashMap<>();
    private final Set<String> favoriteIds = new HashSet<>();
    private final OnProductClickListener clickListener;

    public ProductAdapter(List<Product> initialItems, Map<String, String> initialCategoryNames, OnProductClickListener clickListener) {
        if (initialItems != null) {
            items.addAll(initialItems);
        }
        if (initialCategoryNames != null) {
            categoryNameMap.putAll(initialCategoryNames);
        }
        this.clickListener = clickListener;
    }

    public void submitList(List<Product> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setCategoryNameMap(Map<String, String> newCategoryNameMap) {
        categoryNameMap.clear();
        if (newCategoryNameMap != null) {
            categoryNameMap.putAll(newCategoryNameMap);
        }
        notifyDataSetChanged();
    }

    public void setProductImageMap(Map<String, String> newProductImageMap) {
        productImageMap.clear();
        if (newProductImageMap != null) {
            productImageMap.putAll(newProductImageMap);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = items.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProductImage;
        private final ImageView ivFallbackIcon;
        private final TextView tvCategory;
        private final TextView tvName;
        private final TextView tvMeta;
        private final TextView tvPrice;
        private final TextView tvFavorite;
        private final TextView tvAdd;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            ivFallbackIcon = itemView.findViewById(R.id.ivProductFallbackIcon);
            tvCategory = itemView.findViewById(R.id.tvProductCategory);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvMeta = itemView.findViewById(R.id.tvProductMeta);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvFavorite = itemView.findViewById(R.id.tvProductFavorite);
            tvAdd = itemView.findViewById(R.id.tvAddProduct);
        }

        void bind(Product product) {
            String categoryName = findCategoryName(product);
            String productTitle = !TextUtils.isEmpty(product != null ? product.getTitle() : null)
                    ? product.getTitle()
                    : "San pham";

            if (product != null && product.getId() != null) {
                String imageUrl = productImageMap.get(product.getId());

                if (!TextUtils.isEmpty(imageUrl)) {
                    ivProductImage.setVisibility(View.VISIBLE);
                    ivFallbackIcon.setVisibility(View.GONE);

                    Glide.with(itemView.getContext())
                            .load(imageUrl)
                            .centerCrop()
                            .into(ivProductImage);
                } else {
                    ivProductImage.setVisibility(View.GONE);
                    ivFallbackIcon.setVisibility(View.VISIBLE);
                    ivFallbackIcon.setImageResource(HomeUiUtils.iconResForCategoryName(categoryName));
                }
            } else {
                ivProductImage.setVisibility(View.GONE);
                ivFallbackIcon.setVisibility(View.VISIBLE);
                ivFallbackIcon.setImageResource(HomeUiUtils.iconResForCategoryName(categoryName));
            }

            tvCategory.setText(categoryName.toUpperCase(Locale.ROOT));
            tvName.setText(productTitle);
            tvMeta.setText(HomeUiUtils.formatConditionAndStatus(
                    product != null ? product.getCondition() : null,
                    product != null ? product.getStatus() : null
            ));
            tvPrice.setText(HomeUiUtils.formatPrice(product != null ? product.getPrice() : null));

            boolean isFavorite = product != null && product.getId() != null && favoriteIds.contains(product.getId());
            tvFavorite.setText(isFavorite ? "❤" : "♡");
            tvFavorite.setTextColor(isFavorite ? 0xFFE34F4F : 0xFFB8BFCC);

            tvFavorite.setOnClickListener(v -> toggleFavorite(product));
            tvAdd.setOnClickListener(v -> notifyProductClick(product));
            itemView.setOnClickListener(v -> notifyProductClick(product));
        }

        private String findCategoryName(Product product) {
            if (product != null && product.getCategory_id() != null) {
                String category = categoryNameMap.get(product.getCategory_id());
                if (!TextUtils.isEmpty(category)) {
                    return category;
                }
            }
            return "Danh muc";
        }

        private void toggleFavorite(Product product) {
            if (product == null || product.getId() == null) {
                return;
            }

            String product_id = product.getId();
            if (favoriteIds.contains(product_id)) {
                favoriteIds.remove(product_id);
            } else {
                favoriteIds.add(product_id);
            }

            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position);
            }
        }

        private void notifyProductClick(Product product) {
            if (clickListener != null && product != null) {
                clickListener.onProductClick(product);
            }
        }
    }
}
