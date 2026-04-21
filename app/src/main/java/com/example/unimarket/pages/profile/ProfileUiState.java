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

    public ProfileUiState(User profile, boolean loading, boolean saving,
                          List<Order> orders, List<Product> posts) {
        this.profile = profile;
        this.loading = loading;
        this.saving = saving;
        this.orders = orders != null ? orders : Collections.emptyList();
        this.posts = posts != null ? posts : Collections.emptyList();
    }

    public static ProfileUiState initial() {
        return new ProfileUiState(null, false, false,
                Collections.emptyList(), Collections.emptyList());
    }

    public User getProfile() { return profile; }
    public boolean isLoading() { return loading; }
    public boolean isSaving() { return saving; }
    public List<Order> getOrders() { return orders; }
    public List<Product> getPosts() { return posts; }
}
