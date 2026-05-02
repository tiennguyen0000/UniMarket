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
        postSuccess.setValue(false);
        List<String> localUris = selectedImages.getValue();
        if (localUris == null || localUris.isEmpty()) {
            saveProductToFirestore(product);
        } else {
            uploadImagesAndSave(product, new ArrayList<>(localUris));
        }
    }

    private void uploadImagesAndSave(Product product, List<String> localUris) {
        List<String> downloadUrls = new ArrayList<>(Collections.nCopies(localUris.size(), null));
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicBoolean hasUploadFailure = new AtomicBoolean(false);

        for (int i = 0; i < localUris.size(); i++) {
            final int index = i;
            String uriString = localUris.get(i);
            Uri fileUri = Uri.parse(uriString);
            String fileName = "products/" + UUID.randomUUID() + ".jpg";
            StorageReference ref = storage.getReference().child(fileName);

            ref.putFile(fileUri).continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return ref.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    downloadUrls.set(index, task.getResult().toString());
                } else {
                    hasUploadFailure.set(true);
                }

                if (completedCount.incrementAndGet() == localUris.size()) {
                    if (hasUploadFailure.get() || downloadUrls.contains(null)) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Upload áº£nh tháº¥t báº¡i. Tin chÆ°a Ä‘Æ°á»£c Ä‘Äƒng, vui lÃ²ng thá»­ láº¡i.");
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
                errorMessage.setValue("ÄÄƒng tin tháº¥t báº¡i: " + result.getError());
            }
        });
    }

    private List<Category> buildFallbackCategories() {
        List<Category> fb = new ArrayList<>();
        fb.add(new Category("cat_books",       "GiÃ¡o trÃ¬nh & SÃ¡ch",          null));
        fb.add(new Category("cat_stationery",  "Dá»¥ng cá»¥ há»c táº­p",            null));
        fb.add(new Category("cat_laptop",      "Laptop & MÃ¡y tÃ­nh",          null));
        fb.add(new Category("cat_phone",       "Äiá»‡n thoáº¡i & MÃ¡y tÃ­nh báº£ng", null));
        fb.add(new Category("cat_accessories", "Phá»¥ kiá»‡n cÃ´ng nghá»‡",         null));
        fb.add(new Category("cat_dorm",        "Äá»“ dÃ¹ng phÃ²ng trá»",          null));
        fb.add(new Category("cat_fashion",     "Thá»i trang sinh viÃªn",       null));
        fb.add(new Category("cat_sport",       "Thá»ƒ thao & Giáº£i trÃ­",        null));
        fb.add(new Category("cat_transport",   "PhÆ°Æ¡ng tiá»‡n di chuyá»ƒn",      null));
        fb.add(new Category("cat_free",        "GÃ³c 0 Ä‘á»“ng / Cho táº·ng",      null));
        return fb;
    }

    private String nowIsoUtc() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }
}
