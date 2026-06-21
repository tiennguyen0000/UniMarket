package com.example.unimarket.data.service;

import android.text.TextUtils;

import com.example.unimarket.data.DomainConstants;
import com.example.unimarket.data.model.Notification;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.SavedSearch;
import com.example.unimarket.data.model.Wishlist;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.util.FirestoreIds;
import com.example.unimarket.pages.search.SavedSearchMatcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SavedSearchAlertSync {
    public interface Callback {
        void onComplete();
    }

    private final SavedSearchService savedSearchService = new SavedSearchService();
    private final ProductService productService = new ProductService();
    private final WishlistService wishlistService = new WishlistService();
    private final NotificationService notificationService = new NotificationService();

    public void sync(String userId, Callback callback) {
        if (TextUtils.isEmpty(userId)) {
            if (callback != null) callback.onComplete();
            return;
        }

        savedSearchService.getSavedSearchesByUserId(userId, new AsyncCrudService.ListCallback<SavedSearch>() {
            @Override
            public void onSuccess(List<SavedSearch> data) {
                List<SavedSearch> enabledSearches = new ArrayList<>();
                if (data != null) {
                    for (SavedSearch item : data) {
                        if (item != null && item.isAlerts_enabled()) {
                            enabledSearches.add(item);
                        }
                    }
                }
                if (enabledSearches.isEmpty()) {
                    if (callback != null) callback.onComplete();
                    return;
                }
                loadWishlistIdsAndSync(userId, enabledSearches, callback);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onComplete();
            }
        });
    }

    private void loadWishlistIdsAndSync(String userId, List<SavedSearch> enabledSearches, Callback callback) {
        wishlistService.getWithFilter("user_id", userId, new AsyncCrudService.ListCallback<Wishlist>() {
            @Override
            public void onSuccess(List<Wishlist> data) {
                Set<String> savedProductIds = new HashSet<>();
                if (data != null) {
                    for (Wishlist item : data) {
                        if (item != null && !TextUtils.isEmpty(item.getProduct_id())) {
                            savedProductIds.add(item.getProduct_id());
                        }
                    }
                }
                loadProductsAndSync(userId, enabledSearches, savedProductIds, callback);
            }

            @Override
            public void onError(String error) {
                loadProductsAndSync(userId, enabledSearches, new HashSet<>(), callback);
            }
        });
    }

    private void loadProductsAndSync(String userId, List<SavedSearch> enabledSearches, Set<String> savedProductIds,
                                     Callback callback) {
        productService.getActiveProducts(new AsyncCrudService.ListCallback<Product>() {
            @Override
            public void onSuccess(List<Product> products) {
                process(userId, enabledSearches, savedProductIds, products != null ? products : new ArrayList<>(), callback);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onComplete();
            }
        });
    }

    private void process(String userId, List<SavedSearch> searches, Set<String> savedProductIds,
                         List<Product> products, Callback callback) {
        List<PendingNotification> pendingNotifications = new ArrayList<>();
        List<SavedSearch> changedSearches = new ArrayList<>();

        for (SavedSearch savedSearch : searches) {
            String newestSeen = savedSearch.getLast_seen_product_created_at();
            String newestMatched = newestSeen;

            for (Product product : products) {
                boolean savedOnly = product != null && savedProductIds.contains(product.getId());
                if (!SavedSearchMatcher.isNewMatch(savedSearch, product, savedOnly)) {
                    continue;
                }

                String productId = product.getId();
                String productCreatedAt = product.getCreated_at();
                if (!TextUtils.isEmpty(productCreatedAt)
                        && (TextUtils.isEmpty(newestMatched) || productCreatedAt.compareTo(newestMatched) > 0)) {
                    newestMatched = productCreatedAt;
                }

                pendingNotifications.add(new PendingNotification(
                        FirestoreIds.stableDocId("saved_search_alert", userId, savedSearch.getId(), productId),
                        savedSearch,
                        product
                ));
            }

            if (!TextUtils.equals(newestSeen, newestMatched)) {
                savedSearch.setLast_seen_product_created_at(newestMatched);
                changedSearches.add(savedSearch);
            }
        }

        persist(userId, pendingNotifications, changedSearches, callback);
    }

    private void persist(String userId, List<PendingNotification> notifications,
                         List<SavedSearch> changedSearches, Callback callback) {
        int totalWrites = notifications.size() + changedSearches.size();
        if (totalWrites == 0) {
            if (callback != null) callback.onComplete();
            return;
        }

        final int[] remaining = {totalWrites};
        Runnable finishOne = () -> {
            remaining[0]--;
            if (remaining[0] <= 0 && callback != null) {
                callback.onComplete();
            }
        };

        for (PendingNotification pending : notifications) {
            String title = "Có tin mới cho " + quoteName(pending.savedSearch.getName());
            String content = safeTitle(pending.product) + " vừa xuất hiện trong kết quả bạn đang theo dõi.";
            notificationService.createUniqueNotification(
                    pending.notificationId,
                    userId,
                    title,
                    content,
                    DomainConstants.NotificationType.SAVED_SEARCH,
                    pending.savedSearch.getId(),
                    new ResultCallback<Notification>() {
                        @Override
                        public void onResult(com.example.unimarket.data.service.base.Result<Notification> result) {
                            finishOne.run();
                        }
                    });
        }

        for (SavedSearch savedSearch : changedSearches) {
            savedSearchService.save(savedSearch, result -> finishOne.run());
        }
    }

    private String safeTitle(Product product) {
        return product != null && !TextUtils.isEmpty(product.getTitle())
                ? product.getTitle()
                : "Một sản phẩm mới";
    }

    private String quoteName(String value) {
        String safe = !TextUtils.isEmpty(value) ? value.trim() : "tìm kiếm đã lưu";
        return "\"" + safe + "\"";
    }

    private static final class PendingNotification {
        private final String notificationId;
        private final SavedSearch savedSearch;
        private final Product product;

        private PendingNotification(String notificationId, SavedSearch savedSearch, Product product) {
            this.notificationId = notificationId;
            this.savedSearch = savedSearch;
            this.product = product;
        }
    }
}
