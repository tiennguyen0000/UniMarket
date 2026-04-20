package com.example.unimarket.pages.post;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.service.ProductService;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PostListingViewModel extends ViewModel {
    private final ProductService productService = new ProductService();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    
    // Lưu danh sách Uri tạm thời (ảnh local)
    private final MutableLiveData<List<String>> selectedImages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> postSuccess = new MutableLiveData<>(false);

    public LiveData<List<String>> getSelectedImages() { return selectedImages; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getPostSuccess() { return postSuccess; }

    public void addImages(List<String> uris) {
        List<String> current = selectedImages.getValue();
        if (current != null) {
            for (String uri : uris) {
                if (current.size() < 6) {
                    current.add(uri);
                }
            }
            selectedImages.setValue(current);
        }
    }

    public void removeImage(String uri) {
        List<String> current = selectedImages.getValue();
        if (current != null) {
            current.remove(uri);
            selectedImages.setValue(current);
        }
    }

    public void submitProduct(Product product) {
        isLoading.setValue(true);
        List<String> localUris = selectedImages.getValue();

        // Nếu không có ảnh, lưu trực tiếp thông tin text
        if (localUris == null || localUris.isEmpty()) {
            saveProductToFirestore(product);
            return;
        }

        // Nếu có ảnh, thực hiện quy trình: Upload ảnh -> Lấy link -> Lưu Firestore
        uploadImagesAndSave(product, localUris);
    }

    private void uploadImagesAndSave(Product product, List<String> localUris) {
        List<String> downloadUrls = new ArrayList<>();
        AtomicInteger uploadCount = new AtomicInteger(0);

        for (String uriString : localUris) {
            Uri fileUri = Uri.parse(uriString);
            String fileName = "products/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference ref = storage.getReference().child(fileName);

            ref.putFile(fileUri).continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return ref.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    downloadUrls.add(task.getResult().toString());
                }

                // Khi tất cả ảnh đã upload xong
                if (uploadCount.incrementAndGet() == localUris.size()) {
                    product.setImage_urls(downloadUrls);
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
                errorMessage.setValue(result.getError());
            }
        });
    }
}