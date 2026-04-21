package com.example.unimarket.pages.post;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.service.CategoryService;
import com.example.unimarket.data.service.ProductService;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PostListingViewModel extends ViewModel {
    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private final MutableLiveData<List<String>> selectedImages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> postSuccess = new MutableLiveData<>(false);

    public LiveData<List<String>> getSelectedImages() { return selectedImages; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getPostSuccess() { return postSuccess; }

    public void loadCategories() {
        categoryService.fetchAll(result -> {
            if (result.isSuccess() && result.getData() != null && !result.getData().isEmpty()) {
                categories.setValue(result.getData());
            } else {
                categories.setValue(buildFallbackCategories());
            }
        });
    }

    public void addImages(List<String> uris) {
        List<String> current = new ArrayList<>(
                selectedImages.getValue() != null ? selectedImages.getValue() : Collections.emptyList());
        for (String uri : uris) {
            if (current.size() < 6) current.add(uri);
        }
        selectedImages.setValue(current);
    }

    public void removeImage(String uri) {
        List<String> current = new ArrayList<>(
                selectedImages.getValue() != null ? selectedImages.getValue() : Collections.emptyList());
        current.remove(uri);
        selectedImages.setValue(current);
    }

    public void submitProduct(Product product) {
        isLoading.setValue(true);
        List<String> localUris = selectedImages.getValue();
        if (localUris == null || localUris.isEmpty()) {
            saveProductToFirestore(product);
        } else {
            uploadImagesAndSave(product, new ArrayList<>(localUris));
        }
    }

    private void uploadImagesAndSave(Product product, List<String> localUris) {
        // Dùng synchronizedList để tránh race condition khi nhiều callback cùng add
        List<String> downloadUrls = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger uploadCount = new AtomicInteger(0);

        for (String uriString : localUris) {
            Uri fileUri = Uri.parse(uriString);
            String fileName = "products/" + UUID.randomUUID() + ".jpg";
            StorageReference ref = storage.getReference().child(fileName);

            ref.putFile(fileUri).continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return ref.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    downloadUrls.add(task.getResult().toString());
                }
                if (uploadCount.incrementAndGet() == localUris.size()) {
                    product.setImage_urls(new ArrayList<>(downloadUrls));
                    saveProductToFirestore(product);
                }
            }).addOnFailureListener(e -> {
                isLoading.setValue(false);
                errorMessage.setValue("Lỗi upload ảnh: " + e.getMessage());
            });
        }
    }

    private void saveProductToFirestore(Product product) {
        productService.save(product, result -> {
            isLoading.setValue(false);
            if (result.isSuccess()) {
                postSuccess.setValue(true);
            } else {
                errorMessage.setValue("Đăng tin thất bại: " + result.getError());
            }
        });
    }

    private List<Category> buildFallbackCategories() {
        List<Category> fb = new ArrayList<>();
        fb.add(new Category("cat_books",       "Giáo trình & Sách",          null));
        fb.add(new Category("cat_stationery",  "Dụng cụ học tập",            null));
        fb.add(new Category("cat_laptop",      "Laptop & Máy tính",          null));
        fb.add(new Category("cat_phone",       "Điện thoại & Máy tính bảng", null));
        fb.add(new Category("cat_accessories", "Phụ kiện công nghệ",         null));
        fb.add(new Category("cat_dorm",        "Đồ dùng phòng trọ",          null));
        fb.add(new Category("cat_fashion",     "Thời trang sinh viên",       null));
        fb.add(new Category("cat_sport",       "Thể thao & Giải trí",        null));
        fb.add(new Category("cat_transport",   "Phương tiện di chuyển",      null));
        fb.add(new Category("cat_free",        "Góc 0 đồng / Cho tặng",      null));
        return fb;
    }
}
