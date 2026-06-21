package com.example.unimarket.pages.post;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.service.CategoryService;
import com.example.unimarket.data.service.ProductService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PostListingViewModel extends ViewModel {
    private static final String TAG = "PostListingViewModel";
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
                categories.setValue(new ArrayList<>());
                errorMessage.setValue("Không thể tải danh mục.");
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
        postSuccess.setValue(false);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            rejectSubmit("Vui lòng đăng nhập để đăng tin.");
            return;
        }

        product.setSeller_id(currentUser.getUid());
        List<String> localUris = selectedImages.getValue();
        if (localUris == null || localUris.isEmpty()) {
            saveProductToFirestore(product);
        } else {
            uploadImagesAndSave(product, currentUser.getUid(), new ArrayList<>(localUris));
        }
    }

    private void rejectSubmit(String message) {
        isLoading.setValue(false);
        errorMessage.setValue(message);
    }

    private void uploadImagesAndSave(Product product, String ownerId, List<String> localUris) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            rejectSubmit("Không xác định được chủ tin đăng.");
            return;
        }
        List<String> downloadUrls = new ArrayList<>(Collections.nCopies(localUris.size(), null));
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicBoolean hasUploadFailure = new AtomicBoolean(false);

        for (int i = 0; i < localUris.size(); i++) {
            final int index = i;
            String uriString = localUris.get(i);
            Uri fileUri = Uri.parse(uriString);
            String fileName = "products/" + ownerId + "/" + UUID.randomUUID() + ".jpg";
            StorageReference ref = storage.getReference().child(fileName);
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build();

            ref.putFile(fileUri, metadata).continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return ref.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    downloadUrls.set(index, task.getResult().toString());
                } else {
                    hasUploadFailure.set(true);
                    Log.e(TAG, "Image upload failed: " + uriString, task.getException());
                }

                if (completedCount.incrementAndGet() == localUris.size()) {
                    if (hasUploadFailure.get() || downloadUrls.contains(null)) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Upload ảnh thất bại. Tin chưa được đăng, vui lòng thử lại.");
                        return;
                    }

                    product.setImage_urls(new ArrayList<>(downloadUrls));
                    saveProductToFirestore(product);
                }
            });
        }
    }

    private void saveProductToFirestore(Product product) {
        String now = nowIsoUtc();
        if (product.getCreated_at() == null || product.getCreated_at().trim().isEmpty()) {
            product.setCreated_at(now);
        }
        product.setUpdated_at(now);

        productService.save(product, result -> {
            isLoading.setValue(false);
            if (result.isSuccess()) {
                postSuccess.setValue(true);
            } else {
                errorMessage.setValue("Đăng tin thất bại: " + result.getError());
            }
        });
    }

    private String nowIsoUtc() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }
}
