package com.example.unimarket.pages.profile;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.base.AsyncCrudService;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {
    private final UserService userService = new UserService();
    private final OrderService orderService = new OrderService();
    private final ProductService productService = new ProductService();

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
            updateState(initial, false, null, null, null);
        }

        userService.fetchById(userId, result -> {
            if (result.isSuccess() && result.getData() != null) {
                updateState(result.getData(), null, null, null, null);
            }
        });

        // Load đơn hàng và tin đăng song song
        loadOrders(userId);
        loadPosts(userId);
    }

    private void loadOrders(String userId) {
        orderService.getOrdersByBuyerId(userId, new AsyncCrudService.ListCallback<Order>() {
            @Override public void onSuccess(List<Order> data) {
                updateState(null, null, null, data, null);
            }
            @Override public void onError(String error) {
                updateState(null, null, null, new ArrayList<>(), null);
            }
        });
    }

    private void loadPosts(String userId) {
        productService.getProductsBySellerId(userId, new AsyncCrudService.ListCallback<Product>() {
            @Override public void onSuccess(List<Product> data) {
                updateState(null, null, null, null, data);
            }
            @Override public void onError(String error) {
                updateState(null, null, null, null, new ArrayList<>());
            }
        });
    }

    public void saveProfile(String userId, String fullName, String phone, String university) {
        updateState(null, null, true, null, null);

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setFull_name(fullName);
        updatedUser.setPhone(phone);
        updatedUser.setUniversity(university);

        userService.save(updatedUser, result -> {
            updateState(result.isSuccess() ? result.getData() : null, null, false, null, null);
            if (result.isSuccess()) {
                uiEvent.setValue(ProfileUiEvent.success("Đã lưu hồ sơ!"));
            } else {
                uiEvent.setValue(ProfileUiEvent.error("Lưu thất bại: " + result.getError()));
            }
        });
    }

    private void updateState(User profile, Boolean loading, Boolean saving,
                             List<Order> orders, List<Product> posts) {
        ProfileUiState cur = uiState.getValue() != null
                ? uiState.getValue() : ProfileUiState.initial();
        uiState.setValue(new ProfileUiState(
                profile  != null ? profile  : cur.getProfile(),
                loading  != null ? loading  : cur.isLoading(),
                saving   != null ? saving   : cur.isSaving(),
                orders   != null ? orders   : cur.getOrders(),
                posts    != null ? posts    : cur.getPosts()
        ));
    }
}
