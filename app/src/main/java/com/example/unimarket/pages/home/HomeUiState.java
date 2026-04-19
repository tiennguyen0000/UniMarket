package com.example.unimarket.pages.home;

import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeUiState {
    private final boolean loading;
    private final List<Category> categories;
    private final List<Product> products;
    private final Map<String, String> productImages;

    public HomeUiState(boolean loading, List<Category> categories, List<Product> products, Map<String, String> productImages) {
        this.loading = loading;
        this.categories = categories != null ? categories : new ArrayList<>();
        this.products = products != null ? products : new ArrayList<>();
        this.productImages = productImages != null ? productImages : new HashMap<>();
    }

    public static HomeUiState initial() {
        return new HomeUiState(false, new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    }

    public boolean isLoading() {
        return loading;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<Product> getProducts() {
        return products;
    }

    public Map<String, String> getProductImages() {
        return productImages;
    }
}
