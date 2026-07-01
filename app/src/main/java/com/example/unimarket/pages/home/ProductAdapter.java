package com.example.unimarket.pages.home;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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

    public interface OnProductDetailClickListener {
        void onProductDetailClick(Product product, String imageUrl, String categoryName);
    }

    public interface OnAddToCartClickListener {
        void onAddToCartClick(Product product);
    }

    public interface OnAdminRemoveClickListener {
        void onAdminRemoveClick(Product product);
    }

    public interface OnFavoriteChangedListener {
        void onFavoriteChanged(String productId, boolean saved);
    }

    public interface OnRestockClickListener {
        void onRestockClick(Product product);
    }

    public interface OnSellerEditClickListener {
        void onSellerEditClick(Product product);
    }

    public interface OnSellerRemoveClickListener {
        void onSellerRemoveClick(Product product);
    }

    private final List<Product> items = new ArrayList<>();
    private final Map<String, String> categoryNameMap = new HashMap<>();
    private final Map<String, String> sellerAvatarMap = new HashMap<>();
    private final Set<String> favoriteIds = new HashSet<>();
    private final OnProductClickListener clickListener;
    private final OnProductDetailClickListener detailClickListener;
    private final OnAddToCartClickListener addToCartClickListener;
    private final WishlistService wishlistService = new WishlistService();
    private boolean adminModerationEnabled;
    private String currentUserId;
    private OnAdminRemoveClickListener adminRemoveClickListener;
    private OnFavoriteChangedListener favoriteChangedListener;
    private OnRestockClickListener restockClickListener;
    private OnSellerEditClickListener sellerEditClickListener;
    private OnSellerRemoveClickListener sellerRemoveClickListener;

    public ProductAdapter(List<Product> initialItems, Map<String, String> initialCategoryNames,
                          OnProductClickListener clickListener) {
        this(initialItems, initialCategoryNames, clickListener, null);
    }

    public ProductAdapter(List<Product> initialItems, Map<String, String> initialCategoryNames,
                          OnProductClickListener clickListener,
                          OnProductDetailClickListener detailClickListener) {
        this(initialItems, initialCategoryNames, clickListener, detailClickListener, null);
    }

    public ProductAdapter(List<Product> initialItems, Map<String, String> initialCategoryNames,
                          OnProductClickListener clickListener,
                          OnProductDetailClickListener detailClickListener,
                          OnAddToCartClickListener addToCartClickListener) {
        if (initialItems != null) {
            items.addAll(initialItems);
        }
        if (initialCategoryNames != null) {
            categoryNameMap.putAll(initialCategoryNames);
        }
        this.clickListener = clickListener;
        this.detailClickListener = detailClickListener;
        this.addToCartClickListener = addToCartClickListener;
    }

    public void submitList(List<Product> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void appendItems(List<Product> newItems) {
        if (newItems == null || newItems.isEmpty()) {
            return;
        }
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    public void setCategoryNameMap(Map<String, String> newCategoryNameMap) {
        categoryNameMap.clear();
        if (newCategoryNameMap != null) {
            categoryNameMap.putAll(newCategoryNameMap);
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

    public void setAdminModeration(boolean enabled, String currentUserId,
                                   OnAdminRemoveClickListener listener) {
        this.adminModerationEnabled = enabled;
        this.currentUserId = currentUserId;
        this.adminRemoveClickListener = listener;
        notifyDataSetChanged();
    }

    public void setFavoriteIds(Set<String> ids) {
        favoriteIds.clear();
        if (ids != null) {
            favoriteIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    public void setOnFavoriteChangedListener(OnFavoriteChangedListener listener) {
        this.favoriteChangedListener = listener;
    }

    public void setOnRestockClickListener(OnRestockClickListener listener) {
        this.restockClickListener = listener;
    }

    public void setOnSellerEditClickListener(OnSellerEditClickListener listener) {
        this.sellerEditClickListener = listener;
    }

    public void setOnSellerRemoveClickListener(OnSellerRemoveClickListener listener) {
        this.sellerRemoveClickListener = listener;
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
        private final TextView tvDescription;
        private final TextView tvMeta;
        private final TextView tvPrice;
        private final TextView tvAdd;
        private final ImageView ivProductMenu;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            ivFallbackIcon = itemView.findViewById(R.id.ivProductFallbackIcon);
            ivSellerAvatar = itemView.findViewById(R.id.ivSellerAvatar);
            tvCategory = itemView.findViewById(R.id.tvProductCategory);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvDescription = itemView.findViewById(R.id.tvProductDescription);
            tvMeta = itemView.findViewById(R.id.tvProductMeta);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvAdd = itemView.findViewById(R.id.tvAddProduct);
            ivProductMenu = itemView.findViewById(R.id.ivProductMenu);
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
            bindDescription(product);
            tvMeta.setText(HomeUiUtils.formatConditionAndStatus(
                    product != null ? product.getCondition() : null,
                    product != null ? product.getStatus() : null
            ));
            tvPrice.setText(HomeUiUtils.formatPrice(product != null ? product.getPrice() : null));

            bindProductMenu(product);
            tvAdd.setOnClickListener(v -> {
                if (addToCartClickListener != null && product != null) {
                    addToCartClickListener.onAddToCartClick(product);
                } else {
                    notifyProductDetailClick(product, imageUrl, categoryName);
                }
            });
            itemView.setOnClickListener(v -> notifyProductDetailClick(product, imageUrl, categoryName));
        }

        private void bindDescription(Product product) {
            String description = product != null ? product.getDescription() : null;
            if (TextUtils.isEmpty(description)) {
                tvDescription.setVisibility(View.GONE);
                tvDescription.setText("");
                return;
            }
            String trimmed = description.trim().replaceAll("\\s+", " ");
            if (trimmed.length() < 18) {
                tvDescription.setVisibility(View.GONE);
                tvDescription.setText("");
                return;
            }
            tvDescription.setText(trimmed);
            tvDescription.setVisibility(View.VISIBLE);
        }

        private void bindProductMenu(Product product) {
            boolean show = product != null && !TextUtils.isEmpty(product.getId());
            ivProductMenu.setVisibility(show ? View.VISIBLE : View.GONE);
            ivProductMenu.setOnClickListener(show ? v -> showProductMenu(product) : null);
        }

        private boolean canAdminRemove(Product product) {
            return adminModerationEnabled
                    && adminRemoveClickListener != null
                    && product != null
                    && !TextUtils.isEmpty(product.getId())
                    && !TextUtils.isEmpty(product.getSeller_id())
                    && !product.getSeller_id().equals(currentUserId)
                    && isActiveProduct(product);
        }

        private void showProductMenu(Product product) {
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            LinearLayout content = (LinearLayout) inflater.inflate(R.layout.popup_product_menu, null, false);
            PopupWindow popupWindow = new PopupWindow(
                    content,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );
            popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            popupWindow.setOutsideTouchable(true);
            popupWindow.setElevation(dpToPx(10));

            addMenuAction(
                    inflater,
                    content,
                    isFavoriteProduct(product) ? "Bỏ lưu" : "Lưu tin",
                    v -> {
                        popupWindow.dismiss();
                        toggleFavorite(product);
                    }
            );
            if (canEditOwnProduct(product)) {
                addMenuAction(
                        inflater,
                        content,
                        "Cập nhật tin",
                        v -> {
                            popupWindow.dismiss();
                            sellerEditClickListener.onSellerEditClick(product);
                        }
                );
                addMenuAction(
                        inflater,
                        content,
                        "Gỡ bài",
                        v -> {
                            popupWindow.dismiss();
                            sellerRemoveClickListener.onSellerRemoveClick(product);
                        }
                );
            }
            if (canRestockProduct(product)) {
                addMenuAction(
                        inflater,
                        content,
                        "Cập nhật số lượng",
                        v -> {
                            popupWindow.dismiss();
                            restockClickListener.onRestockClick(product);
                        }
                );
            }
            if (canAdminRemove(product)) {
                addMenuAction(
                        inflater,
                        content,
                        "Gỡ bài",
                        v -> {
                            popupWindow.dismiss();
                            adminRemoveClickListener.onAdminRemoveClick(product);
                        }
                );
            }

            content.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            int xOff = ivProductMenu.getWidth() - content.getMeasuredWidth();
            popupWindow.showAsDropDown(ivProductMenu, xOff, dpToPx(2));
        }

        private void addMenuAction(LayoutInflater inflater, LinearLayout container,
                                   String title, View.OnClickListener listener) {
            TextView actionView = (TextView) inflater.inflate(
                    R.layout.item_product_menu_action,
                    container,
                    false
            );
            actionView.setText(title);
            actionView.setOnClickListener(listener);
            container.addView(actionView);
        }

        private boolean isFavoriteProduct(Product product) {
            return product != null
                    && !TextUtils.isEmpty(product.getId())
                    && favoriteIds.contains(product.getId());
        }

        private boolean canRestockProduct(Product product) {
            return restockClickListener != null
                    && product != null
                    && !TextUtils.isEmpty(product.getId())
                    && !TextUtils.isEmpty(product.getSeller_id())
                    && product.getSeller_id().equals(currentUserId);
        }

        private boolean canEditOwnProduct(Product product) {
            return sellerEditClickListener != null
                    && sellerRemoveClickListener != null
                    && product != null
                    && !TextUtils.isEmpty(product.getId())
                    && !TextUtils.isEmpty(product.getSeller_id())
                    && product.getSeller_id().equals(currentUserId);
        }

        private boolean isActiveProduct(Product product) {
            String status = product.getStatus() != null
                    ? product.getStatus().trim().toLowerCase(Locale.ROOT)
                    : "active";
            return status.isEmpty()
                    || status.equals("active")
                    || status.equals("available")
                    || status.equals("pending");
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

            ivProductMenu.setEnabled(false);
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
                    ivProductMenu.setEnabled(true);
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
            wishlist.setId(stableDocId("wishlist", userId, productId));
            wishlist.setUser_id(userId);
            wishlist.setProduct_id(productId);
            wishlist.setCreated_at(nowIsoUtc());
            wishlistService.save(wishlist, result -> {
                ivProductMenu.setEnabled(true);
                if (result.isSuccess()) {
                    favoriteIds.add(productId);
                    notifyFavoriteChanged(productId, true);
                    notifyCurrentItemChanged();
                } else {
                    Toast.makeText(itemView.getContext(), "Lưu thất bại: " + result.getError(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void deleteWishlist(Wishlist wishlist, String productId) {
            wishlistService.deleteById(wishlist.getId(), new AsyncCrudService.BooleanCallback() {
                @Override
                public void onSuccess(boolean success) {
                    ivProductMenu.setEnabled(true);
                    favoriteIds.remove(productId);
                    notifyFavoriteChanged(productId, false);
                    notifyCurrentItemChanged();
                }

                @Override
                public void onError(String error) {
                    ivProductMenu.setEnabled(true);
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

        private void notifyFavoriteChanged(String productId, boolean saved) {
            if (favoriteChangedListener != null && !TextUtils.isEmpty(productId)) {
                favoriteChangedListener.onFavoriteChanged(productId, saved);
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
            if (product.getImage_urls() != null && !product.getImage_urls().isEmpty()) {
                return product.getImage_urls().get(0);
            }
            return null;
        }
    }

    private String nowIsoUtc() {
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return format.format(new java.util.Date());
    }

    private String stableDocId(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append('_');
            }
            builder.append(part != null ? part : "item");
        }
        return builder.toString().replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private int dpToPx(int dp) {
        return Math.round(dp * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }
}
