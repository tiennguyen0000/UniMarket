package com.example.unimarket.pages.home;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.service.CategoryService;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.UserService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeViewModel extends ViewModel {
    private static final int REQUEST_COUNT = 2;
    private final CategoryService categoryService = new CategoryService();
    private final ProductService productService = new ProductService();
    private final UserService userService = new UserService();

    private final MutableLiveData<HomeUiState> uiState = new MutableLiveData<>(HomeUiState.initial());
    private final MutableLiveData<HomeUiEvent> uiEvent = new MutableLiveData<>();
    private int pendingRequests = 0;
    private boolean homeLoaded = false;
    private boolean catalogLoaded = false;
    private boolean homeRequestInFlight = false;
    private boolean catalogRequestInFlight = false;

    public LiveData<HomeUiState> getUiState() { return uiState; }
    public LiveData<HomeUiEvent> getUiEvent() { return uiEvent; }

    public void loadHomeData() {
        loadHomeData(false);
    }

    public void refreshHomeData() {
        loadHomeData(true);
    }

    public void refreshCatalogData() {
        loadData(null, true);
    }

    private void loadHomeData(boolean forceRefresh) {
        HomeUiState current = uiState.getValue();
        if (!forceRefresh && (homeLoaded || (current != null && !current.getCategories().isEmpty()))) {
            updateState(false, null, null, null);
            return;
        }
        if (homeRequestInFlight || catalogRequestInFlight) {
            return;
        }
        homeRequestInFlight = true;
        pendingRequests = 1;
        updateState(true, null, null, null);
        loadCategories();
    }

    public void loadCatalogData() {
        loadData(null, false);
    }

    private void loadData(Integer productLimit, boolean forceRefresh) {
        if (!forceRefresh && catalogLoaded) {
            updateState(false, null, null, null);
            return;
        }
        if (catalogRequestInFlight) {
            return;
        }
        catalogRequestInFlight = true;
        if (homeRequestInFlight) {
            pendingRequests = Math.max(pendingRequests, 1) + 1;
            updateState(true, null, null, null);
            loadProducts(productLimit);
            return;
        }
        pendingRequests = REQUEST_COUNT;
        updateState(true, null, null, null);
        loadCategories();
        loadProducts(productLimit);
    }

    private void loadCategories() {
        categoryService.fetchAll(result -> {
            List<Category> data = result.isSuccess() ? result.getData() : null;
            updateState(null, data != null ? data : new ArrayList<>(), null, null);
            finishRequest();
        });
    }

    private void loadProducts(Integer productLimit) {
        productService.fetchAll(result -> {
            List<Product> data = result.isSuccess() ? result.getData() : null;
            if (data == null || data.isEmpty()) {
                updateState(null, null, new ArrayList<>(), null);
                finishRequest();
                return;
            }

            List<Product> sortedProducts = new ArrayList<>();
            for (Product product : data) {
                if (product != null) {
                    sortedProducts.add(product);
                }
            }
            sortedProducts.sort(Comparator.comparing(this::buildSortKey).reversed());

            int maxItems = productLimit != null
                    ? Math.min(sortedProducts.size(), Math.max(productLimit, 0))
                    : sortedProducts.size();
            List<Product> topProducts = new ArrayList<>();
            for (int i = 0; i < maxItems; i++) {
                topProducts.add(sortedProducts.get(i));
            }

            updateState(null, null, topProducts, null);
            loadSellerAvatars(topProducts);
            finishRequest();
        });
    }

    private void loadSellerAvatars(List<Product> products) {
        if (products == null || products.isEmpty()) return;

        List<String> sellerIds = new ArrayList<>();
        for (Product p : products) {
            if (p.getSeller_id() != null && !sellerIds.contains(p.getSeller_id())) {
                sellerIds.add(p.getSeller_id());
            }
        }
        if (sellerIds.isEmpty()) return;

        Map<String, String> avatarMap = new HashMap<>();
        AtomicInteger count = new AtomicInteger(0);

        for (String sellerId : sellerIds) {
            userService.fetchById(sellerId, result -> {
                if (result.isSuccess() && result.getData() != null
                        && !TextUtils.isEmpty(result.getData().getAvatar_url())) {
                    avatarMap.put(sellerId, result.getData().getAvatar_url());
                }
                if (count.incrementAndGet() == sellerIds.size()) {
                    updateState(null, null, null, avatarMap);
                }
            });
        }
    }

    private void finishRequest() {
        pendingRequests--;
        if (pendingRequests <= 0) {
            homeLoaded = true;
            homeRequestInFlight = false;
            if (catalogRequestInFlight) {
                catalogLoaded = true;
                catalogRequestInFlight = false;
            }
            updateState(false, null, null, null);
        }
    }

    private void updateState(Boolean loading, List<Category> categories, List<Product> products,
                             Map<String, String> avatars) {
        HomeUiState current = uiState.getValue() != null ? uiState.getValue() : HomeUiState.initial();

        Map<String, String> updatedAvatars = current.getSellerAvatars();
        if (avatars != null) {
            updatedAvatars = new java.util.HashMap<>(current.getSellerAvatars());
            updatedAvatars.putAll(avatars);
        }

        uiState.setValue(new HomeUiState(
                loading != null ? loading : current.isLoading(),
                categories != null ? categories : current.getCategories(),
                products != null ? products : current.getProducts(),
                updatedAvatars
        ));
    }

    private String buildSortKey(Product product) {
        if (product == null) {
            return "";
        }
        if (!TextUtils.isEmpty(product.getCreated_at())) {
            return product.getCreated_at();
        }
        return product.getId() != null ? product.getId() : "";
    }

}
