package com.example.unimarket.pages.home;

import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.ProductImage;
import com.example.unimarket.data.service.CategoryService;
import com.example.unimarket.data.service.ProductImageService;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.UserService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";
    private static final int REQUEST_COUNT = 3;

    private final CategoryService categoryService = new CategoryService();
    private final ProductService productService = new ProductService();
    private final ProductImageService productImageService = new ProductImageService();
    private final UserService userService = new UserService();

    private final MutableLiveData<HomeUiState> uiState = new MutableLiveData<>(HomeUiState.initial());
    private final MutableLiveData<HomeUiEvent> uiEvent = new MutableLiveData<>();
    private int pendingRequests = 0;

    public LiveData<HomeUiState> getUiState() { return uiState; }
    public LiveData<HomeUiEvent> getUiEvent() { return uiEvent; }

    public void loadHomeData() {
        pendingRequests = REQUEST_COUNT;
        updateState(true, null, null, null, null);
        loadCategories();
        loadProducts();
        loadProductImages();
    }

    private void loadCategories() {
        categoryService.fetchAll(result -> {
            List<Category> data = result.isSuccess() ? result.getData() : null;
            if (data == null || data.isEmpty()) {
                updateState(null, buildFallbackCategories(), null, null, null);
            } else {
                updateState(null, data, null, null, null);
            }
            finishRequest();
        });
    }

    private void loadProducts() {
        productService.fetchAll(result -> {
            List<Product> data = result.isSuccess() ? result.getData() : null;
            if (data == null || data.isEmpty()) {
                updateState(null, null, buildFallbackProducts(), buildFallbackProductImages(), null);
                finishRequest();
                return;
            }

            List<Product> sortedProducts = new ArrayList<>(data);
            sortedProducts.sort(Comparator.comparing((Product p) ->
                    p != null && p.getId() != null ? p.getId() : "").reversed());

            int maxItems = Math.min(sortedProducts.size(), 12);
            List<Product> topProducts = new ArrayList<>();
            for (int i = 0; i < maxItems; i++) topProducts.add(sortedProducts.get(i));

            Map<String, String> extractedImages = extractImagesFromProducts(topProducts);
            updateState(null, null, topProducts, extractedImages, null);
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
            // Dùng fetchById (trả Result<T>) thay vì getById (trả ItemCallback<T>)
            userService.fetchById(sellerId, result -> {
                if (result.isSuccess() && result.getData() != null
                        && !TextUtils.isEmpty(result.getData().getAvatar_url())) {
                    avatarMap.put(sellerId, result.getData().getAvatar_url());
                }
                if (count.incrementAndGet() == sellerIds.size()) {
                    updateState(null, null, null, null, avatarMap);
                }
            });
        }
    }

    private void loadProductImages() {
        productImageService.fetchAll(result -> {
            Map<String, String> imageMap = new HashMap<>();
            List<ProductImage> data = result.isSuccess() ? result.getData() : null;
            if (data != null) {
                for (ProductImage img : data) {
                    if (img != null && img.getProduct_id() != null && !TextUtils.isEmpty(img.getImage_url())) {
                        if (!imageMap.containsKey(img.getProduct_id())) {
                            imageMap.put(img.getProduct_id(), img.getImage_url());
                        }
                    }
                }
            }
            if (!result.isSuccess() && !TextUtils.isEmpty(result.getError())) {
                Log.d(TAG, result.getError());
            }
            mergeProductImages(imageMap);
            finishRequest();
        });
    }

    private void mergeProductImages(Map<String, String> newImages) {
        HomeUiState current = uiState.getValue() != null ? uiState.getValue() : HomeUiState.initial();
        Map<String, String> merged = new HashMap<>(current.getProductImages());
        if (newImages != null) merged.putAll(newImages);
        updateState(null, null, null, merged, null);
    }

    private void finishRequest() {
        pendingRequests--;
        if (pendingRequests <= 0) updateState(false, null, null, null, null);
    }

    private void updateState(Boolean loading, List<Category> categories, List<Product> products,
                             Map<String, String> images, Map<String, String> avatars) {
        HomeUiState current = uiState.getValue() != null ? uiState.getValue() : HomeUiState.initial();

        Map<String, String> updatedImages = current.getProductImages();
        if (images != null) {
            updatedImages = new HashMap<>(current.getProductImages());
            updatedImages.putAll(images);
        }

        Map<String, String> updatedAvatars = current.getSellerAvatars();
        if (avatars != null) {
            updatedAvatars = new HashMap<>(current.getSellerAvatars());
            updatedAvatars.putAll(avatars);
        }

        uiState.setValue(new HomeUiState(
                loading != null ? loading : current.isLoading(),
                categories != null ? categories : current.getCategories(),
                products != null ? products : current.getProducts(),
                updatedImages,
                updatedAvatars
        ));
    }

    private Map<String, String> extractImagesFromProducts(List<Product> products) {
        Map<String, String> imageMap = new HashMap<>();
        if (products == null) return imageMap;
        for (Product p : products) {
            if (p != null && p.getId() != null && p.getImage_urls() != null && !p.getImage_urls().isEmpty()) {
                String first = p.getImage_urls().get(0);
                if (!TextUtils.isEmpty(first)) imageMap.put(p.getId(), first);
            }
        }
        return imageMap;
    }

    // ─── Fallback data khi không có network ───────────────────────────────────

    private List<Category> buildFallbackCategories() {
        List<Category> fb = new ArrayList<>();
        fb.add(new Category("cat_laptop",      "Laptop & Máy tính",               null));
        fb.add(new Category("cat_phone",       "Điện thoại & Máy tính bảng",      null));
        fb.add(new Category("cat_books",       "Giáo trình & Sách",               null));
        fb.add(new Category("cat_accessories", "Phụ kiện công nghệ",              null));
        fb.add(new Category("cat_stationery",  "Dụng cụ học tập",                 null));
        fb.add(new Category("cat_dorm",        "Đồ dùng phòng trọ",               null));
        fb.add(new Category("cat_fashion",     "Thời trang sinh viên",            null));
        fb.add(new Category("cat_sport",       "Thể thao & Giải trí",             null));
        return fb;
    }

    private List<Product> buildFallbackProducts() {
        List<Product> fb = new ArrayList<>();
        fb.add(makeFallback("fb-1", "cat_laptop",      "MacBook Air M1 8GB/256GB",     14_500_000d, "used"));
        fb.add(makeFallback("fb-2", "cat_books",       "Giáo trình Giải tích 1",          45_000d, "used"));
        fb.add(makeFallback("fb-3", "cat_accessories", "Tai nghe Sony WH-1000XM4",      890_000d, "used"));
        fb.add(makeFallback("fb-4", "cat_dorm",        "Nồi cơm mini 1.2L",             280_000d, "used"));
        fb.add(makeFallback("fb-5", "cat_accessories", "Bàn phím cơ Keychron K2",       650_000d, "good"));
        fb.add(makeFallback("fb-6", "cat_phone",       "Samsung Galaxy A54 5G 128GB",  4_200_000d, "used"));
        fb.add(makeFallback("fb-7", "cat_dorm",        "Quạt sạc điện Sunhouse",        320_000d, "good"));
        fb.add(makeFallback("fb-8", "cat_sport",       "Vợt cầu lông Yonex Astrox",     650_000d, "used"));
        return fb;
    }

    private Map<String, String> buildFallbackProductImages() {
        Map<String, String> imgs = new HashMap<>();
        imgs.put("fb-1", "https://picsum.photos/seed/laptop42/400/300");
        imgs.put("fb-2", "https://picsum.photos/seed/book11/400/300");
        imgs.put("fb-3", "https://picsum.photos/seed/audio22/400/300");
        imgs.put("fb-4", "https://picsum.photos/seed/rice33/400/300");
        imgs.put("fb-5", "https://picsum.photos/seed/keyboard44/400/300");
        imgs.put("fb-6", "https://picsum.photos/seed/phone55/400/300");
        imgs.put("fb-7", "https://picsum.photos/seed/fan66/400/300");
        imgs.put("fb-8", "https://picsum.photos/seed/sport77/400/300");
        return imgs;
    }

    private Product makeFallback(String id, String catId, String title, double price, String condition) {
        Product p = new Product();
        p.setId(id); p.setCategory_id(catId); p.setTitle(title);
        p.setPrice(price); p.setCondition(condition); p.setStatus("active");
        return p;
    }
}
