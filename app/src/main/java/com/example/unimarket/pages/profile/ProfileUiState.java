package com.example.unimarket.pages.profile;

import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.User;

import java.util.Collections;
import java.util.List;

public class ProfileUiState {
    private final User profile;
    private final boolean loading;
    private final boolean saving;
    private final List<Order> orders;
    private final List<Product> posts;
    private final List<Product> savedProducts;
    private final double ratingAverage;
    private final int ratingCount;

    public ProfileUiState(User profile, boolean loading, boolean saving,
                          List<Order> orders, List<Product> posts, List<Product> savedProducts,
                          double ratingAverage, int ratingCount) {
        this.profile = profile;
        this.loading = loading;
        this.saving = saving;
        this.orders = orders != null ? orders : Collections.emptyList();
        this.posts = posts != null ? posts : Collections.emptyList();
        this.savedProducts = savedProducts != null ? savedProducts : Collections.emptyList();
        this.ratingAverage = ratingAverage;
        this.ratingCount = ratingCount;
    }

    public static ProfileUiState initial() {
        return new ProfileUiState(null, false, false,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0, 0);
    }

    public User getProfile() { return profile; }
    public boolean isLoading() { return loading; }
    public boolean isSaving() { return saving; }
    public List<Order> getOrders() { return orders; }
    public List<Product> getPosts() { return posts; }
    public List<Product> getSavedProducts() { return savedProducts; }
    public double getRatingAverage() { return ratingAverage; }
    public int getRatingCount() { return ratingCount; }
}
