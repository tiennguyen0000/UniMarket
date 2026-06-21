package com.example.unimarket.pages.search;

import android.text.TextUtils;

import com.example.unimarket.data.DomainConstants;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.SavedSearch;

import java.util.Locale;

public final class SavedSearchMatcher {
    private SavedSearchMatcher() {
    }

    public static boolean matches(SavedSearch savedSearch, Product product, boolean savedOnly) {
        if (savedSearch == null || product == null || !isVisibleProduct(product)) {
            return false;
        }

        String query = safeLower(savedSearch.getQuery()).trim();
        if (!TextUtils.isEmpty(query) && !matchesQuery(product, query)) {
            return false;
        }

        if (!TextUtils.isEmpty(savedSearch.getCategory_id())
                && !TextUtils.equals(savedSearch.getCategory_id(), product.getCategory_id())) {
            return false;
        }

        double price = product.getPrice() != null ? product.getPrice() : 0d;
        double min = savedSearch.getMin_price() != null ? savedSearch.getMin_price() : 0d;
        double max = savedSearch.getMax_price() != null ? savedSearch.getMax_price() : Double.MAX_VALUE;
        if (max <= 0d) {
            max = Double.MAX_VALUE;
        }
        if (price < min || price > max) {
            return false;
        }

        if (savedSearch.isFilter_saved_only() && !savedOnly) {
            return false;
        }

        if (savedSearch.isFilter_new() || savedSearch.isFilter_used()) {
            String condition = safeLower(product.getCondition());
            boolean isNew = DomainConstants.ProductCondition.NEW.equals(condition);
            boolean isUsed = DomainConstants.ProductCondition.USED.equals(condition)
                    || "good".equals(condition);
            if (savedSearch.isFilter_new() && !isNew) {
                return false;
            }
            if (savedSearch.isFilter_used() && !isUsed) {
                return false;
            }
        }

        return true;
    }

    public static boolean isNewMatch(SavedSearch savedSearch, Product product, boolean savedOnly) {
        if (!matches(savedSearch, product, savedOnly)) {
            return false;
        }
        String productCreatedAt = product.getCreated_at();
        String lastSeen = savedSearch.getLast_seen_product_created_at();
        if (TextUtils.isEmpty(productCreatedAt) || TextUtils.isEmpty(lastSeen)) {
            return true;
        }
        return productCreatedAt.compareTo(lastSeen) > 0;
    }

    private static boolean isVisibleProduct(Product product) {
        String status = safeLower(product.getStatus());
        return !status.equals("removed")
                && !status.equals("inactive")
                && !status.equals(DomainConstants.ProductStatus.HIDDEN);
    }

    private static boolean matchesQuery(Product product, String query) {
        return safeLower(product.getTitle()).contains(query)
                || safeLower(product.getDescription()).contains(query);
    }

    private static String safeLower(String value) {
        return value != null ? value.toLowerCase(Locale.ROOT) : "";
    }
}
