package com.example.unimarket.pages.orders;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.service.OrderService;

import java.util.ArrayList;
import java.util.List;

public class OrdersViewModel extends ViewModel {
    private final OrderService orderService = new OrderService();

    private final MutableLiveData<List<Order>> allOrders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<List<Order>> getAllOrders() { return allOrders; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadOrders(String userId) {
        if (userId == null) return;
        isLoading.setValue(true);

        orderService.getOrdersByBuyerId(userId, new com.example.unimarket.data.service.base.AsyncCrudService.ListCallback<Order>() {
            @Override
            public void onSuccess(List<Order> data) {
                isLoading.setValue(false);
                allOrders.setValue(data != null ? data : new ArrayList<>());
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
                allOrders.setValue(new ArrayList<>());
            }
        });
    }

    /** Lọc theo status. Truyền null để lấy tất cả. */
    public List<Order> filterByStatus(String status) {
        List<Order> all = allOrders.getValue();
        if (all == null) return new ArrayList<>();
        if (status == null) return new ArrayList<>(all);

        List<Order> result = new ArrayList<>();
        for (Order o : all) {
            if (status.equalsIgnoreCase(o.getStatus())) result.add(o);
        }
        return result;
    }
}
