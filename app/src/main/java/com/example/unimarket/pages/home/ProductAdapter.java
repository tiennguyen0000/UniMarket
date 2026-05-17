package com.example.unimarket.pages.home;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.Wishlist;
import com.example.unimarket.data.service.WishlistService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.util.FirestoreIds;
import com.example.unimarket.data.util.TimeUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public interface OnProductDetailClickListener {
        void onProductDetailClick(Product product, String imageUrl, String categoryName);
    }

    private final List<Product> items = new ArrayList<>();
    private final Map<String, String> categoryNameMap = new HashMap<>();
    private final Map<String, String> productImageMap = new HashMap<>();
    private final Map<String, String> sellerAvatarMap = new HashMap<>();
    private final Set<String> favoriteIds = new HashSet<>();
    private final OnProductClickListener clickListener;
    private final OnProductDetailClickListener detailClickListener;
    private final WishlistService wishlistService = new WishlistService();

    public ProductAdapter(List<Product> initialItems, Map<String, String> initialCategoryNames,
                          OnProductClickListener clickListener) {
        this(initialItems, initialCategoryNames, clickListener, null);
    }

    public ProductAdapter(List<Product> initialItems, Map<String, String> initialCategoryNames,
                          OnProductClickListener clickListener,
                          OnProductDetailClickListener detailClickListener) {
        if (initialItems != null) {
            items.addAll(initialItems);
        }
        if (initialCategoryNames != null) {
            categoryNameMap.putAll(initialCategoryNames);
        }
        this.clickListener = clickListener;
        this.detailClickListener = detailClickListener;
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

    public void setSellerAvatarMap(Map<String, String> newSellerAvatarMap) {
        sellerAvatarMap.clear();
        if (newSellerAvatarMap != null) {
            sellerAvatarMap.putAll(newSellerAvatarMap);
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
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProductImage;
        private final ImageView ivFallbackIcon;
        private final ImageView ivSellerAvatar;
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
            ivSellerAvatar = itemView.findViewById(R.id.ivSellerAvatar);
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
                    : "Sản phẩm";
            String imageUrl = getImageUrl(product);

            bindProductImage(imageUrl, categoryName);
            bindSellerAvatar(product);

            tvCategory.setText(categoryName.toUpperCase(Locale.ROOT));
            tvName.setText(productTitle);
            tvMeta.setText(HomeUiUtils.formatConditionAndStatus(
                    product != null ? product.getCondition() : null,
                    product != null ? product.getStatus() : null
            ));
            tvPrice.setText(HomeUiUtils.formatPrice(product != null ? product.getPrice() : null));

            boolean isFavorite = product != null && product.getId() != null && favoriteIds.contains(product.getId());
            tvFavorite.setText(isFavorite ? "❤" : "♡");
            tvFavorite.setTextColor(isFavorite ? 0xFFE34F4F : 0xFF98A2B3);

            tvFavorite.setOnClickListener(v -> toggleFavorite(product));
            tvAdd.setOnClickListener(v -> notifyProductDetailClick(product, imageUrl, categoryName));
            itemView.setOnClickListener(v -> notifyProductDetailClick(product, imageUrl, categoryName));
        }

        private void bindProductImage(String imageUrl, String categoryName) {
            ivFallbackIcon.setImageResource(HomeUiUtils.iconResForCategoryName(categoryName));

            if (!TextUtils.isEmpty(imageUrl)) {
                ivProductImage.setVisibility(View.VISIBLE);
                ivFallbackIcon.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.bg_light_grey_rounded)
                        .error(R.drawable.bg_light_grey_rounded)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(GlideException e, Object model,
                                                        Target<Drawable> target, boolean isFirstResource) {
                                ivProductImage.setVisibility(View.GONE);
                                ivFallbackIcon.setVisibility(View.VISIBLE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                                           Target<Drawable> target, DataSource dataSource,
                                                           boolean isFirstResource) {
                                ivProductImage.setVisibility(View.VISIBLE);
                                ivFallbackIcon.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(ivProductImage);
                return;
            }

            Glide.with(itemView.getContext()).clear(ivProductImage);
            ivProductImage.setVisibility(View.GONE);
            ivFallbackIcon.setVisibility(View.VISIBLE);
        }

        private void bindSellerAvatar(Product product) {
            if (product == null) {
                ivSellerAvatar.setImageResource(R.drawable.ic_user_placeholder);
                return;
            }

            String sellerAvatarUrl = sellerAvatarMap.get(product.getSeller_id());
            Glide.with(itemView.getContext())
                    .load(!TextUtils.isEmpty(sellerAvatarUrl) ? sellerAvatarUrl : R.drawable.ic_user_placeholder)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(ivSellerAvatar);
        }

        private String findCategoryName(Product product) {
            if (product != null && product.getCategory_id() != null) {
                String category = categoryNameMap.get(product.getCategory_id());
                if (!TextUtils.isEmpty(category)) {
                    return category;
                }
            }
            return "Danh mục";
        }

        private void toggleFavorite(Product product) {
            if (product == null || product.getId() == null) {
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(itemView.getContext(), "Vui lòng đăng nhập để lưu sản phẩm", Toast.LENGTH_SHORT).show();
                return;
            }

            tvFavorite.setEnabled(false);
            wishlistService.getWithFilter("user_id", user.getUid(), new AsyncCrudService.ListCallback<Wishlist>() {
                @Override
                public void onSuccess(List<Wishlist> data) {
                    Wishlist existing = findWishlistForProduct(data, product.getId());
                    if (existing != null && !TextUtils.isEmpty(existing.getId())) {
                        deleteWishlist(existing, product.getId());
                    } else {
                        saveWishlist(user.getUid(), product.getId());
                    }
                }

                @Override
                public void onError(String error) {
                    tvFavorite.setEnabled(true);
                    Toast.makeText(itemView.getContext(), "Không thể cập nhật lưu tin: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        private Wishlist findWishlistForProduct(List<Wishlist> data, String productId) {
            if (data == null) {
                return null;
            }
            for (Wishlist item : data) {
                if (item != null && productId.equals(item.getProduct_id())) {
                    return item;
                }
            }
            return null;
        }

        private void saveWishlist(String userId, String productId) {
            Wishlist wishlist = new Wishlist();
            wishlist.setId(FirestoreIds.stableDocId("wishlist", userId, productId));
            wishlist.setUser_id(userId);
            wishlist.setProduct_id(productId);
            wishlist.setCreated_at(TimeUtils.nowIsoUtc());
            wishlistService.save(wishlist, result -> {
                tvFavorite.setEnabled(true);
                if (result.isSuccess()) {
                    favoriteIds.add(productId);
                    notifyCurrentItemChanged();
                    Toast.makeText(itemView.getContext(), "Đã lưu sản phẩm", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(itemView.getContext(), "Lưu thất bại: " + result.getError(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void deleteWishlist(Wishlist wishlist, String productId) {
            wishlistService.deleteById(wishlist.getId(), new AsyncCrudService.BooleanCallback() {
                @Override
                public void onSuccess(boolean success) {
                    tvFavorite.setEnabled(true);
                    favoriteIds.remove(productId);
                    notifyCurrentItemChanged();
                    Toast.makeText(itemView.getContext(), "Đã bỏ lưu sản phẩm", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    tvFavorite.setEnabled(true);
                    Toast.makeText(itemView.getContext(), "Bỏ lưu thất bại: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void notifyCurrentItemChanged() {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position);
            }
        }

        private void notifyProductDetailClick(Product product, String imageUrl, String categoryName) {
            if (detailClickListener != null && product != null) {
                detailClickListener.onProductDetailClick(product, imageUrl, categoryName);
                return;
            }
            if (clickListener != null && product != null) {
                clickListener.onProductClick(product);
            }
        }

        private String getImageUrl(Product product) {
            if (product == null) {
                return null;
            }
            if (product.getId() != null && !TextUtils.isEmpty(productImageMap.get(product.getId()))) {
                return productImageMap.get(product.getId());
            }
            if (product.getImage_urls() != null && !product.getImage_urls().isEmpty()) {
                return product.getImage_urls().get(0);
            }
            return null;
        }
    }
}
