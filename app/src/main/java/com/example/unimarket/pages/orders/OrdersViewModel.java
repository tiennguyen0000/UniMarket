package com.example.unimarket.pages.orders;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.base.AsyncCrudService;

import java.util.ArrayList;
import java.util.List;

public class OrdersViewModel extends ViewModel {
    private final OrderService orderService = new OrderService();
    private final UserService userService = new UserService();

    private final MutableLiveData<List<Order>> buyerOrders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Order>> sellerOrders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<User> currentProfile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<List<Order>> getBuyerOrders() { return buyerOrders; }
    public LiveData<List<Order>> getSellerOrders() { return sellerOrders; }
    public LiveData<User> getCurrentProfile() { return currentProfile; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadOrders(String userId) {
        if (userId == null) return;

        isLoading.setValue(true);
        userService.getProfileById(userId, new AsyncCrudService.ItemCallback<User>() {
            @Override
            public void onSuccess(User profile) {
                currentProfile.setValue(profile);
                loadBuyerAndSellerOrders(userId);
            }

            @Override
            public void onError(String error) {
                currentProfile.setValue(null);
                loadBuyerAndSellerOrders(userId);
            }
        });
    }

    private void loadBuyerAndSellerOrders(String userId) {
        final int[] pending = {2};
        final String[] firstError = {null};

        orderService.getOrdersByBuyerId(userId, new AsyncCrudService.ListCallback<Order>() {
            @Override
            public void onSuccess(List<Order> data) {
                buyerOrders.setValue(sorted(data));
                finishOne();
            }

            @Override
            public void onError(String error) {
                firstError[0] = error;
                buyerOrders.setValue(new ArrayList<>());
                finishOne();
            }

            private void finishOne() {
                pending[0]--;
                if (pending[0] == 0) finishLoad(firstError[0]);
            }
        });

        orderService.getOrdersBySellerId(userId, new AsyncCrudService.ListCallback<Order>() {
            @Override
            public void onSuccess(List<Order> data) {
                sellerOrders.setValue(sorted(data));
                finishOne();
            }

            @Override
            public void onError(String error) {
                firstError[0] = firstError[0] == null ? error : firstError[0];
                sellerOrders.setValue(new ArrayList<>());
                finishOne();
            }

            private void finishOne() {
                pending[0]--;
                if (pending[0] == 0) finishLoad(firstError[0]);
            }
        });
    }

    private void finishLoad(String error) {
        isLoading.setValue(false);
        if (error != null && isBothListsEmpty()) {
            errorMessage.setValue(error);
        }
    }

    public List<Order> ordersForMode(boolean sellerMode, String status) {
        List<Order> source = sellerMode ? sellerOrders.getValue() : buyerOrders.getValue();
        if (source == null) return new ArrayList<>();
        if (status == null) return new ArrayList<>(source);

        List<Order> result = new ArrayList<>();
        for (Order order : source) {
            String orderStatus = order != null && order.getStatus() != null ? order.getStatus() : "pending";
            if (status.equalsIgnoreCase(orderStatus)) result.add(order);
        }
        return result;
    }

    private List<Order> sorted(List<Order> orders) {
        List<Order> result = new ArrayList<>();
        if (orders != null) result.addAll(orders);
        result.sort((left, right) -> safe(right.getCreated_at()).compareTo(safe(left.getCreated_at())));
        return result;
    }

    private boolean isBothListsEmpty() {
        List<Order> buying = buyerOrders.getValue();
        List<Order> selling = sellerOrders.getValue();
        return (buying == null || buying.isEmpty()) && (selling == null || selling.isEmpty());
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
