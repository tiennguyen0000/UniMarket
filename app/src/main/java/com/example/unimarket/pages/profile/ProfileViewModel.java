package com.example.unimarket.pages.profile;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.Review;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.model.Wishlist;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.ReviewService;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.WishlistService;
import com.example.unimarket.data.service.base.AsyncCrudService;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {
    private final UserService userService = new UserService();
    private final OrderService orderService = new OrderService();
    private final ProductService productService = new ProductService();
    private final ReviewService reviewService = new ReviewService();
    private final WishlistService wishlistService = new WishlistService();

    private final MutableLiveData<ProfileUiState> uiState =
            new MutableLiveData<>(ProfileUiState.initial());
    private final MutableLiveData<ProfileUiEvent> uiEvent = new MutableLiveData<>();
    private int pendingLoads = 0;

    public LiveData<ProfileUiState> getUiState() { return uiState; }
    public LiveData<ProfileUiEvent> getUiEvent() { return uiEvent; }

    public void loadProfile(String userId, String displayName) {
        if (TextUtils.isEmpty(userId)) {
            return;
        }

        pendingLoads = 5;
        updateState(null, true, null, null, null, null, null, null);

        if (!TextUtils.isEmpty(displayName)) {
            User initial = new User();
            initial.setId(userId);
            initial.setFull_name(displayName);
            updateState(initial, null, null, null, null, null, null, null);
        }

        loadProfileInfo(userId);
        loadOrders(userId);
        loadPosts(userId);
        loadSavedProducts(userId);
        loadRating(userId);
    }

    private void loadProfileInfo(String userId) {
        userService.fetchById(userId, result -> {
            if (result.isSuccess() && result.getData() != null) {
                updateState(result.getData(), null, null, null, null, null, null, null);
            }
            finishLoad();
        });
    }

    private void loadOrders(String userId) {
        orderService.getOrdersByBuyerId(userId, new AsyncCrudService.ListCallback<Order>() {
            @Override public void onSuccess(List<Order> data) {
                updateState(null, null, null, data != null ? data : new ArrayList<>(), null, null, null, null);
                finishLoad();
            }

            @Override public void onError(String error) {
                updateState(null, null, null, new ArrayList<>(), null, null, null, null);
                finishLoad();
            }
        });
    }

    private void loadPosts(String userId) {
        productService.getProductsBySellerId(userId, new AsyncCrudService.ListCallback<Product>() {
            @Override public void onSuccess(List<Product> data) {
                updateState(null, null, null, null, data != null ? data : new ArrayList<>(), null, null, null);
                finishLoad();
            }

            @Override public void onError(String error) {
                updateState(null, null, null, null, new ArrayList<>(), null, null, null);
                finishLoad();
            }
        });
    }

    private void loadSavedProducts(String userId) {
        wishlistService.getWithFilter("user_id", userId, new AsyncCrudService.ListCallback<Wishlist>() {
            @Override
            public void onSuccess(List<Wishlist> data) {
                List<Wishlist> saved = data != null ? data : new ArrayList<>();
                if (saved.isEmpty()) {
                    updateState(null, null, null, null, null, new ArrayList<>(), null, null);
                    finishLoad();
                    return;
                }

                List<Product> products = new ArrayList<>();
                final int[] pending = {saved.size()};
                for (Wishlist item : saved) {
                    productService.getById(item.getProduct_id(), new AsyncCrudService.ItemCallback<Product>() {
                        @Override
                        public void onSuccess(Product product) {
                            if (product != null) {
                                products.add(product);
                            }
                            finishOne();
                        }

                        @Override
                        public void onError(String error) {
                            finishOne();
                        }

                        private void finishOne() {
                            pending[0]--;
                            if (pending[0] == 0) {
                                updateState(null, null, null, null, null, products, null, null);
                                finishLoad();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                updateState(null, null, null, null, null, new ArrayList<>(), null, null);
                finishLoad();
            }
        });
    }

    private void loadRating(String userId) {
        reviewService.getReviewsBySellerId(userId, new AsyncCrudService.ListCallback<Review>() {
            @Override
            public void onSuccess(List<Review> data) {
                if (data == null || data.isEmpty()) {
                    updateState(null, null, null, null, null, null, 0d, 0);
                    finishLoad();
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
                updateState(null, null, null, null, null, null, average, count);
                finishLoad();
            }

            @Override
            public void onError(String error) {
                updateState(null, null, null, null, null, null, 0d, 0);
                finishLoad();
            }
        });
    }

    public void saveProfile(String userId, String fullName, String phone, String university, String location) {
        updateState(null, null, true, null, null, null, null, null);

        User currentProfile = uiState.getValue() != null ? uiState.getValue().getProfile() : null;
        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setFull_name(fullName);
        updatedUser.setPhone(phone);
        updatedUser.setUniversity(university);
        updatedUser.setLocation(location);

        if (currentProfile != null) {
            updatedUser.setAvatar_url(currentProfile.getAvatar_url());
            updatedUser.setVerified(currentProfile.isVerified());
            updatedUser.setRole(currentProfile.getRole());
            updatedUser.setAccount_status(currentProfile.getAccount_status());
            updatedUser.setCreated_at(currentProfile.getCreated_at());
            updatedUser.setUpdated_at(currentProfile.getUpdated_at());
        }

        userService.save(updatedUser, result -> {
            updateState(result.isSuccess() ? updatedUser : null, null, false, null, null, null, null, null);
            if (!result.isSuccess()) {
                uiEvent.setValue(ProfileUiEvent.error("Lưu thất bại: " + result.getError()));
            }
        });
    }

    private void finishLoad() {
        pendingLoads--;
        if (pendingLoads <= 0) {
            updateState(null, false, null, null, null, null, null, null);
        }
    }

    private void updateState(User profile, Boolean loading, Boolean saving,
                             List<Order> orders, List<Product> posts, List<Product> savedProducts,
                             Double ratingAverage, Integer ratingCount) {
        ProfileUiState cur = uiState.getValue() != null
                ? uiState.getValue() : ProfileUiState.initial();
        uiState.setValue(new ProfileUiState(
                profile != null ? profile : cur.getProfile(),
                loading != null ? loading : cur.isLoading(),
                saving != null ? saving : cur.isSaving(),
                orders != null ? orders : cur.getOrders(),
                posts != null ? posts : cur.getPosts(),
                savedProducts != null ? savedProducts : cur.getSavedProducts(),
                ratingAverage != null ? ratingAverage : cur.getRatingAverage(),
                ratingCount != null ? ratingCount : cur.getRatingCount()
        ));
    }
}
