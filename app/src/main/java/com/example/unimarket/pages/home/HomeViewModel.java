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
import com.example.unimarket.data.service.base.Result;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";
    private static final int REQUEST_COUNT = 3;

    private final CategoryService categoryService = new CategoryService();
    private final ProductService productService = new ProductService();
    private final ProductImageService productImageService = new ProductImageService();

    private final MutableLiveData<HomeUiState> uiState = new MutableLiveData<>(HomeUiState.initial());
    private final MutableLiveData<HomeUiEvent> uiEvent = new MutableLiveData<>();
    private int pendingRequests = 0;

    public LiveData<HomeUiState> getUiState() {
        return uiState;
    }

    public LiveData<HomeUiEvent> getUiEvent() {
        return uiEvent;
    }

    public void loadHomeData() {
        pendingRequests = REQUEST_COUNT;
        updateState(true, null, null, null);
        loadCategories();
        loadProducts();
        loadProductImages();
    }

    private void loadCategories() {
        categoryService.fetchAll(result -> {
            List<Category> data = result.isSuccess() ? result.getData() : null;
            if (data == null || data.isEmpty()) {
                updateState(null, buildFallbackCategories(), null, null);
                if (!result.isSuccess()) {
                    uiEvent.setValue(HomeUiEvent.showMessage("Khong tai duoc danh muc, dang hien thi du lieu mau"));
                }
            } else {
                updateState(null, data, null, null);
            }
            finishRequest();
        });
    }

    private void loadProducts() {
        productService.fetchAll(result -> {
            List<Product> data = result.isSuccess() ? result.getData() : null;
            if (data == null || data.isEmpty()) {
                updateState(null, null, buildFallbackProducts(), buildFallbackProductImages());
                finishRequest();
                return;
            }

            List<Product> sortedProducts = new ArrayList<>(data);
            sortedProducts.sort(Comparator.comparing((Product product) ->
                    product != null && product.getId() != null ? product.getId() : "").reversed());

            int maxItems = Math.min(sortedProducts.size(), 12);
            List<Product> topProducts = new ArrayList<>();
            for (int i = 0; i < maxItems; i++) {
                topProducts.add(sortedProducts.get(i));
            }
            
            // Extract images từ Product.image_urls
            Map<String, String> extractedImages = extractImagesFromProducts(topProducts);
            updateState(null, null, topProducts, extractedImages);
            finishRequest();
        });
    }

    private void loadProductImages() {
        // Fetch từ product_images collection để merge với ảnh từ Product.image_urls
        productImageService.fetchAll(result -> {
            Map<String, String> imageMap = new HashMap<>();
            List<ProductImage> data = result.isSuccess() ? result.getData() : null;

            if (data != null && !data.isEmpty()) {
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
            // Merge với current images thay vì ghi đè
            mergeProductImages(imageMap);
            finishRequest();
        });
    }

    private void mergeProductImages(Map<String, String> newImages) {
        HomeUiState current = uiState.getValue() != null ? uiState.getValue() : HomeUiState.initial();
        Map<String, String> mergedImages = new HashMap<>(current.getProductImages());
        // Merge, ưu tiên ảnh từ product_images collection
        if (newImages != null) {
            mergedImages.putAll(newImages);
        }
        updateState(null, null, null, mergedImages);
    }

    private void finishRequest() {
        pendingRequests--;
        if (pendingRequests <= 0) {
            updateState(false, null, null, null);
        }
    }

    private void updateState(Boolean loading, List<Category> categories, List<Product> products, Map<String, String> images) {
        HomeUiState current = uiState.getValue() != null ? uiState.getValue() : HomeUiState.initial();
        uiState.setValue(new HomeUiState(
                loading != null ? loading : current.isLoading(),
                categories != null ? categories : current.getCategories(),
                products != null ? products : current.getProducts(),
                images != null ? images : current.getProductImages()
        ));
    }

    private List<Category> buildFallbackCategories() {
        List<Category> fallback = new ArrayList<>();
        fallback.add(new Category("fb-cat-1", "Laptop", null));
        fallback.add(new Category("fb-cat-2", "Điện tử", null));
        fallback.add(new Category("fb-cat-3", "Sách", null));
        fallback.add(new Category("fb-cat-4", "Điện thoại", null));
        fallback.add(new Category("fb-cat-5", "Phụ kiện", null));
        fallback.add(new Category("fb-cat-6", "Thể thao", null));
        fallback.add(new Category("fb-cat-7", "Nhà cửa", null));
        fallback.add(new Category("fb-cat-8", "Văn phòng", null));
        return fallback;
    }

    private List<Product> buildFallbackProducts() {
        List<Product> fallback = new ArrayList<>();
        fallback.add(createFallbackProduct("fb-prod-1", "fb-cat-1", "MacBook Pro M1 2020 8GB/256GB", 15_500_000d, "used"));
        fallback.add(createFallbackProduct("fb-prod-2", "fb-cat-3", "Giáo trình Giải tích 1 (Tái bản)", 45_000d, "good"));
        fallback.add(createFallbackProduct("fb-prod-3", "fb-cat-2", "Tai nghe Sony WH-1000XM4", 890_000d, "used"));
        fallback.add(createFallbackProduct("fb-prod-4", "fb-cat-7", "Đèn bàn LED chống cận Xiaomi", 120_000d, "new"));
        fallback.add(createFallbackProduct("fb-prod-5", "fb-cat-5", "Bàn phím cơ Keychron K2 RGB", 650_000d, "used"));
        fallback.add(createFallbackProduct("fb-prod-6", "fb-cat-4", "Samsung Galaxy A54 5G 128GB", 4_200_000d, "used"));
        fallback.add(createFallbackProduct("fb-prod-7", "fb-cat-7", "Bình nước giữ nhiệt 500ml", 95_000d, "new"));
        fallback.add(createFallbackProduct("fb-prod-8", "fb-cat-6", "Vợt cầu lông Yonex Nanoray", 280_000d, "good"));
        return fallback;
    }

    private Map<String, String> buildFallbackProductImages() {
        Map<String, String> images = new HashMap<>();
        images.put("fb-prod-1", "https://picsum.photos/seed/laptop42/400/300");
        images.put("fb-prod-2", "https://picsum.photos/seed/book11/400/300");
        images.put("fb-prod-3", "https://picsum.photos/seed/audio22/400/300");
        images.put("fb-prod-4", "https://picsum.photos/seed/desk33/400/300");
        images.put("fb-prod-5", "https://picsum.photos/seed/keyboard44/400/300");
        images.put("fb-prod-6", "https://picsum.photos/seed/phone55/400/300");
        images.put("fb-prod-7", "https://picsum.photos/seed/bottle66/400/300");
        images.put("fb-prod-8", "https://picsum.photos/seed/sport77/400/300");
        return images;
    }

    private Map<String, String> extractImagesFromProducts(List<Product> products) {
        Map<String, String> imageMap = new HashMap<>();
        if (products != null) {
            for (Product product : products) {
                if (product != null && product.getId() != null && product.getImage_urls() != null) {
                    List<String> imageUrls = product.getImage_urls();
                    if (!imageUrls.isEmpty()) {
                        // Lấy ảnh đầu tiên từ danh sách
                        String firstImageUrl = imageUrls.get(0);
                        if (!TextUtils.isEmpty(firstImageUrl)) {
                            imageMap.put(product.getId(), firstImageUrl);
                        }
                    }
                }
            }
        }
        return imageMap;
    }

    private Product createFallbackProduct(String id, String categoryId, String title, Double price, String condition) {
        Product product = new Product();
        product.setId(id);
        product.setCategory_id(categoryId);
        product.setTitle(title);
        product.setPrice(price);
        product.setCondition(condition);
        product.setStatus("active");
        return product;
    }
}
