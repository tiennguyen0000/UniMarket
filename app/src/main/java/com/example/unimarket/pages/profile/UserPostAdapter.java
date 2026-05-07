package com.example.unimarket.pages.profile;

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
import com.example.unimarket.pages.home.HomeUiUtils;

import java.util.ArrayList;
import java.util.List;

public class UserPostAdapter extends RecyclerView.Adapter<UserPostAdapter.PostViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(Product product);
    }

    private final List<Product> items = new ArrayList<>();
    private final OnPostClickListener listener;

    public UserPostAdapter(OnPostClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Product> posts) {
        items.clear();
        if (posts != null) items.addAll(posts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new PostViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProductImage, ivSellerAvatar;
        private final TextView tvProductName, tvProductPrice, tvProductMeta, tvProductCategory;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage    = itemView.findViewById(R.id.ivProductImage);
            ivSellerAvatar    = itemView.findViewById(R.id.ivSellerAvatar);
            tvProductName     = itemView.findViewById(R.id.tvProductName);
            tvProductPrice    = itemView.findViewById(R.id.tvProductPrice);
            tvProductMeta     = itemView.findViewById(R.id.tvProductMeta);
            tvProductCategory = itemView.findViewById(R.id.tvProductCategory);
        }

        void bind(Product product) {
            tvProductName.setText(product.getTitle() != null ? product.getTitle() : "");
            tvProductPrice.setText(HomeUiUtils.formatPrice(product.getPrice()));
            tvProductMeta.setText(HomeUiUtils.formatConditionAndStatus(product.getCondition(), product.getStatus()));
            tvProductCategory.setText(
                    product.getCategory_id() != null ? product.getCategory_id().replace("cat_", "").toUpperCase() : "");

            // Ảnh đại diện sản phẩm (ưu tiên image_urls[0])
            String imgUrl = null;
            if (product.getImage_urls() != null && !product.getImage_urls().isEmpty()) {
                imgUrl = product.getImage_urls().get(0);
            }
            if (!TextUtils.isEmpty(imgUrl)) {
                Glide.with(itemView.getContext())
                        .load(imgUrl)
                        .centerCrop()
                        .placeholder(R.drawable.bg_light_grey_rounded)
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.bg_light_grey_rounded);
            }

            // Ẩn seller avatar trong context Profile (chính là user đang xem)
            ivSellerAvatar.setVisibility(View.GONE);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onPostClick(product);
            });
        }
    }
}
