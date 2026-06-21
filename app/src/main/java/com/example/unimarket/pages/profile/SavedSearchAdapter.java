package com.example.unimarket.pages.profile;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.SavedSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SavedSearchAdapter extends RecyclerView.Adapter<SavedSearchAdapter.ViewHolder> {
    interface Listener {
        void onOpen(SavedSearch savedSearch);
        void onToggleAlerts(SavedSearch savedSearch, boolean enabled);
        void onDelete(SavedSearch savedSearch);
    }

    private final List<SavedSearch> items = new ArrayList<>();
    private final Listener listener;

    SavedSearchAdapter(Listener listener) {
        this.listener = listener;
    }

    void submitList(List<SavedSearch> savedSearches) {
        items.clear();
        if (savedSearches != null) {
            items.addAll(savedSearches);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSavedSearchName;
        private final TextView tvSavedSearchSummary;
        private final TextView btnOpenSavedSearch;
        private final TextView btnDeleteSavedSearch;
        private final SwitchCompat switchSavedSearchAlerts;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSavedSearchName = itemView.findViewById(R.id.tvSavedSearchName);
            tvSavedSearchSummary = itemView.findViewById(R.id.tvSavedSearchSummary);
            btnOpenSavedSearch = itemView.findViewById(R.id.btnOpenSavedSearch);
            btnDeleteSavedSearch = itemView.findViewById(R.id.btnDeleteSavedSearch);
            switchSavedSearchAlerts = itemView.findViewById(R.id.switchSavedSearchAlerts);
        }

        void bind(SavedSearch savedSearch) {
            tvSavedSearchName.setText(!TextUtils.isEmpty(savedSearch.getName())
                    ? savedSearch.getName()
                    : "Tìm kiếm đã lưu");
            tvSavedSearchSummary.setText(buildSummary(savedSearch));
            switchSavedSearchAlerts.setOnCheckedChangeListener(null);
            switchSavedSearchAlerts.setChecked(savedSearch.isAlerts_enabled());
            switchSavedSearchAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggleAlerts(savedSearch, isChecked);
                }
            });
            btnOpenSavedSearch.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onOpen(savedSearch);
                }
            });
            btnDeleteSavedSearch.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(savedSearch);
                }
            });
        }
    }

    private String buildSummary(SavedSearch savedSearch) {
        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(savedSearch.getQuery())) {
            parts.add("\"" + savedSearch.getQuery() + "\"");
        }
        if (savedSearch.getMin_price() != null || savedSearch.getMax_price() != null) {
            String min = savedSearch.getMin_price() != null ? formatMoney(savedSearch.getMin_price()) : "0đ";
            String max = savedSearch.getMax_price() != null ? formatMoney(savedSearch.getMax_price()) : "∞";
            parts.add(min + " - " + max);
        }
        if (savedSearch.isFilter_new()) {
            parts.add("Mới");
        }
        if (savedSearch.isFilter_used()) {
            parts.add("Đã dùng");
        }
        if (savedSearch.isFilter_saved_only()) {
            parts.add("Tin đã lưu");
        }
        if (parts.isEmpty()) {
            parts.add("Không có bộ lọc bổ sung");
        }
        return TextUtils.join(" · ", parts);
    }

    private String formatMoney(double value) {
        return String.format(Locale.getDefault(), "%,.0fđ", value);
    }
}
