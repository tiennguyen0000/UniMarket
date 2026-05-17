package com.example.unimarket.pages.orders;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.DomainConstants;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.service.CheckoutService;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.base.AsyncCrudService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrdersViewModel extends ViewModel {
    private final OrderService orderService = new OrderService();
    private final CheckoutService checkoutService = new CheckoutService();

    private final MutableLiveData<List<Order>> allOrders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<List<Order>> getAllOrders() { return allOrders; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadOrders(String userId) {
        if (userId == null) return;
        isLoading.setValue(true);
        errorMessage.setValue(null);

        Map<String, Order> merged = new LinkedHashMap<>();
        final int[] pending = {2};
        AsyncCrudService.ListCallback<Order> callback = new AsyncCrudService.ListCallback<Order>() {
            @Override
            public void onSuccess(List<Order> data) {
                if (data != null) {
                    for (Order order : data) {
                        if (order != null && order.getId() != null) {
                            merged.put(order.getId(), order);
                        }
                    }
                }
                finishLoad(merged, pending, null);
            }

            @Override
            public void onError(String error) {
                finishLoad(merged, pending, error);
            }
        };

        orderService.getOrdersByBuyerId(userId, callback);
        orderService.getOrdersBySellerId(userId, callback);
    }

    public void updateOrderStatus(String orderId, String actorId, String nextStatus) {
        isLoading.setValue(true);
        checkoutService.updateOrderStatus(orderId, actorId, nextStatus, result -> {
            isLoading.setValue(false);
            if (result.isSuccess() && result.getData() != null) {
                replaceOrder(result.getData());
            } else {
                errorMessage.setValue(result.getError());
            }
        });
    }

    private void replaceOrder(Order updatedOrder) {
        List<Order> current = new ArrayList<>(
                allOrders.getValue() != null ? allOrders.getValue() : new ArrayList<>());
        boolean replaced = false;
        for (int i = 0; i < current.size(); i++) {
            Order order = current.get(i);
            if (order != null && updatedOrder.getId() != null && updatedOrder.getId().equals(order.getId())) {
                current.set(i, updatedOrder);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            current.add(updatedOrder);
        }
        allOrders.setValue(current);
    }

    private void finishLoad(Map<String, Order> merged, int[] pending, String error) {
        if (error != null && errorMessage.getValue() == null) {
            errorMessage.setValue(error);
        }
        pending[0]--;
        if (pending[0] > 0) {
            return;
        }
        isLoading.setValue(false);
        allOrders.setValue(new ArrayList<>(merged.values()));
    }

    public List<Order> filterByStatus(String filter) {
        List<Order> all = allOrders.getValue();
        if (all == null) return new ArrayList<>();
        if (filter == null || OrderUiFormatter.FILTER_ALL.equals(filter)) return new ArrayList<>(all);

        List<Order> result = new ArrayList<>();
        for (Order o : all) {
            if (OrderUiFormatter.matchesFilter(o, filter)) result.add(o);
        }
        return result;
    }

    public int countByFilter(String filter) {
        return filterByStatus(filter).size();
    }

    public String nextActionForUser(Order order, String userId) {
        if (order == null || userId == null) return null;
        String status = order.getStatus() != null
                ? order.getStatus().toLowerCase(Locale.ROOT)
                : DomainConstants.OrderStatus.PENDING;
        boolean isSeller = userId.equals(order.getSeller_id());
        boolean isBuyer = userId.equals(order.getBuyer_id());
        if (isSeller && DomainConstants.OrderStatus.PENDING.equals(status)) return DomainConstants.OrderStatus.CONFIRMED;
        if (isSeller && DomainConstants.OrderStatus.CONFIRMED.equals(status)) return DomainConstants.OrderStatus.SHIPPING;
        if (isSeller && DomainConstants.OrderStatus.SHIPPING.equals(status)) return DomainConstants.OrderStatus.DONE;
        if ((isBuyer || isSeller)
                && (DomainConstants.OrderStatus.PENDING.equals(status)
                || DomainConstants.OrderStatus.CONFIRMED.equals(status))) {
            return DomainConstants.OrderStatus.CANCELLED;
        }
        return null;
    }
}
