package com.example.unimarket.pages.home;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final List<Category> items = new ArrayList<>();
    private final OnCategoryClickListener clickListener;

    public CategoryAdapter(List<Category> initialItems, OnCategoryClickListener clickListener) {
        if (initialItems != null) {
            items.addAll(initialItems);
        }
        this.clickListener = clickListener;
    }

    public void submitList(List<Category> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = items.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvName;
        private final View iconBackground;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            iconBackground = itemView.findViewById(R.id.iconContainer);
        }

        void bind(Category category) {
            String categoryName = category != null ? category.getName() : null;
            String displayName = categoryName != null && !categoryName.trim().isEmpty()
                    ? categoryName
                    : "Danh muc";

            ivIcon.setImageResource(HomeUiUtils.iconResForCategoryName(displayName));
            tvName.setText(displayName);

            GradientDrawable background = (GradientDrawable) iconBackground.getBackground().mutate();
            background.setColor(HomeUiUtils.colorForCategoryName(displayName));

            itemView.setOnClickListener(v -> {
                if (clickListener != null && category != null) {
                    clickListener.onCategoryClick(category);
                }
            });
        }
    }
}