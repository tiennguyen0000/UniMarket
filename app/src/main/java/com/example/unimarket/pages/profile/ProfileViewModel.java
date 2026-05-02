package com.example.unimarket.pages.profile;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.Review;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.ReviewService;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.base.AsyncCrudService;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {
    private final UserService userService = new UserService();
    private final OrderService orderService = new OrderService();
    private final ProductService productService = new ProductService();
    private final ReviewService reviewService = new ReviewService();

    private final MutableLiveData<ProfileUiState> uiState =
            new MutableLiveData<>(ProfileUiState.initial());
    private final MutableLiveData<ProfileUiEvent> uiEvent = new MutableLiveData<>();

    public LiveData<ProfileUiState> getUiState() { return uiState; }
    public LiveData<ProfileUiEvent> getUiEvent() { return uiEvent; }

    public void loadProfile(String userId, String displayName) {
        if (!TextUtils.isEmpty(displayName)) {
            User initial = new User();
            initial.setId(userId);
            initial.setFull_name(displayName);
            updateState(initial, false, null, null, null, null, null);
        }

        userService.fetchById(userId, result -> {
            if (result.isSuccess() && result.getData() != null) {
                updateState(result.getData(), null, null, null, null, null, null);
            }
        });

        loadOrders(userId);
        loadPosts(userId);
        loadRating(userId);
    }

    private void loadOrders(String userId) {
        orderService.getOrdersByBuyerId(userId, new AsyncCrudService.ListCallback<Order>() {
            @Override public void onSuccess(List<Order> data) {
                updateState(null, null, null, data, null, null, null);
            }

            @Override public void onError(String error) {
                updateState(null, null, null, new ArrayList<>(), null, null, null);
            }
        });
    }

    private void loadPosts(String userId) {
        productService.getProductsBySellerId(userId, new AsyncCrudService.ListCallback<Product>() {
            @Override public void onSuccess(List<Product> data) {
                updateState(null, null, null, null, data, null, null);
            }

            @Override public void onError(String error) {
                updateState(null, null, null, null, new ArrayList<>(), null, null);
            }
        });
    }

    private void loadRating(String userId) {
        reviewService.getReviewsBySellerId(userId, new AsyncCrudService.ListCallback<Review>() {
            @Override
            public void onSuccess(List<Review> data) {
                if (data == null || data.isEmpty()) {
                    updateState(null, null, null, null, null, 0d, 0);
                    return;
                }

                double total = 0;
                int count = 0;
                for (Review review : data) {
                    if (review != null && review.getRating() != null) {
                        total += review.getRating();
                        count++;
                    }
                }

                double average = count > 0 ? total / count : 0;
                updateState(null, null, null, null, null, average, count);
            }

            @Override
            public void onError(String error) {
                updateState(null, null, null, null, null, 0d, 0);
            }
        });
    }

    public void saveProfile(String userId, String fullName, String phone, String university) {
        updateState(null, null, true, null, null, null, null);

        User currentProfile = uiState.getValue() != null ? uiState.getValue().getProfile() : null;
        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setFull_name(fullName);
        updatedUser.setPhone(phone);
        updatedUser.setUniversity(university);

        if (currentProfile != null) {
            updatedUser.setAvatar_url(currentProfile.getAvatar_url());
            updatedUser.setIs_verified(currentProfile.is_verified());
            updatedUser.setCreated_at(currentProfile.getCreated_at());
            updatedUser.setUpdated_at(currentProfile.getUpdated_at());
        }

        userService.save(updatedUser, result -> {
            updateState(result.isSuccess() ? updatedUser : null, null, false, null, null, null, null);
            if (result.isSuccess()) {
                uiEvent.setValue(ProfileUiEvent.success("ÄÃ£ lÆ°u há»“ sÆ¡!"));
            } else {
                uiEvent.setValue(ProfileUiEvent.error("LÆ°u tháº¥t báº¡i: " + result.getError()));
            }
        });
    }

    private void updateState(User profile, Boolean loading, Boolean saving,
                             List<Order> orders, List<Product> posts,
                             Double ratingAverage, Integer ratingCount) {
        ProfileUiState cur = uiState.getValue() != null
                ? uiState.getValue() : ProfileUiState.initial();
        uiState.setValue(new ProfileUiState(
                profile != null ? profile : cur.getProfile(),
                loading != null ? loading : cur.isLoading(),
                saving != null ? saving : cur.isSaving(),
                orders != null ? orders : cur.getOrders(),
                posts != null ? posts : cur.getPosts(),
                ratingAverage != null ? ratingAverage : cur.getRatingAverage(),
                ratingCount != null ? ratingCount : cur.getRatingCount()
        ));
    }
}
