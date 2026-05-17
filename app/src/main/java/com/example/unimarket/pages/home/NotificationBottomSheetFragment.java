package com.example.unimarket.pages.home;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.Notification;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.service.NotificationService;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class NotificationBottomSheetFragment extends BottomSheetDialogFragment {
    public static final String RESULT_NOTIFICATIONS_CHANGED = "notifications_changed";

    private final NotificationService notificationService = new NotificationService();
    private final OrderService orderService = new OrderService();
    private final ProductService productService = new ProductService();

    private TextView tvNotificationSubtitle;
    private TextView btnMarkAllRead;
    private View layoutNotificationLoading;
    private View layoutNotificationEmpty;
    private TextView tvNotificationEmptyMessage;
    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;

    private final List<Notification> notifications = new ArrayList<>();
    private String currentUserId;
    private boolean seededDuringThisOpen;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_notifications, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() == null) {
            return;
        }
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNotificationSubtitle = view.findViewById(R.id.tvNotificationSubtitle);
        btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead);
        layoutNotificationLoading = view.findViewById(R.id.layoutNotificationLoading);
        layoutNotificationEmpty = view.findViewById(R.id.layoutNotificationEmpty);
        tvNotificationEmptyMessage = view.findViewById(R.id.tvNotificationEmptyMessage);
        rvNotifications = view.findViewById(R.id.rvNotifications);

        adapter = new NotificationAdapter(this::handleNotificationClick);
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(adapter);
        rvNotifications.setItemAnimator(null);

        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());
        btnMarkAllRead.setOnClickListener(v -> markAllRead());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = user != null ? user.getUid() : null;
        if (TextUtils.isEmpty(currentUserId)) {
            showEmpty("Bạn cần đăng nhập để nhận thông báo hệ thống.");
            return;
        }

        loadNotifications(true);
    }

    private void loadNotifications(boolean allowSeed) {
        showLoading();
        notificationService.getNotificationsByUserId(currentUserId, new AsyncCrudService.ListCallback<Notification>() {
            @Override
            public void onSuccess(List<Notification> data) {
                if (!isAdded()) {
                    return;
                }
                List<Notification> loaded = data != null ? data : new ArrayList<>();
                sortNotifications(loaded);
                if (loaded.isEmpty() && allowSeed && !seededDuringThisOpen) {
                    seededDuringThisOpen = true;
                    seedStarterNotifications();
                    return;
                }
                bindNotifications(loaded);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                showEmpty("Không tải được thông báo lúc này. Bạn thử mở lại sau nhé.");
            }
        });
    }

    private void bindNotifications(List<Notification> loaded) {
        notifications.clear();
        notifications.addAll(loaded);
        adapter.submitList(notifications);

        int unreadCount = countUnread(notifications);
        if (notifications.isEmpty()) {
            showEmpty("Các cập nhật về đơn hàng, tin đăng và đánh giá sẽ xuất hiện tại đây.");
            return;
        }

        layoutNotificationLoading.setVisibility(View.GONE);
        layoutNotificationEmpty.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.VISIBLE);
        btnMarkAllRead.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
        tvNotificationSubtitle.setText(unreadCount > 0
                ? unreadCount + " thông báo chưa đọc"
                : "Bạn đã đọc hết thông báo");
    }

    private void handleNotificationClick(Notification notification) {
        if (notification == null) {
            return;
        }

        if (!notification.isRead()) {
            notification.setIs_read(true);
            notificationService.save(notification, result -> notifyHomeBadgeChanged(countUnread(notifications)));
            adapter.notifyDataSetChanged();
            updateSubtitleOnly();
        }

        openNotificationTarget(notification);
    }

    private void openNotificationTarget(Notification notification) {
        String type = notification.getType() != null
                ? notification.getType().toLowerCase(Locale.ROOT) : "";
        if (!isAdded()) {
            return;
        }
        if (type.contains("order") || type.contains("review")) {
            dismiss();
            NavHostFragment.findNavController(requireParentFragment()).navigate(R.id.ordersFragment);
        } else if (type.contains("product") || type.contains("price") || type.contains("listing")) {
            dismiss();
            NavHostFragment.findNavController(requireParentFragment()).navigate(R.id.searchFragment);
        }
    }

    private void markAllRead() {
        List<Notification> changedNotifications = new ArrayList<>();
        for (Notification notification : notifications) {
            if (notification != null && !notification.isRead()) {
                notification.setIs_read(true);
                changedNotifications.add(notification);
            }
        }
        if (!changedNotifications.isEmpty()) {
            final int[] pendingWrites = {changedNotifications.size()};
            for (Notification notification : changedNotifications) {
                notificationService.save(notification, result -> {
                    pendingWrites[0]--;
                    if (pendingWrites[0] <= 0) {
                        notifyHomeBadgeChanged(0);
                    }
                });
            }
            adapter.notifyDataSetChanged();
            updateSubtitleOnly();
            notifyHomeBadgeChanged(0);
        }
    }

    private void updateSubtitleOnly() {
        int unreadCount = countUnread(notifications);
        btnMarkAllRead.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
        tvNotificationSubtitle.setText(unreadCount > 0
                ? unreadCount + " thông báo chưa đọc"
                : "Bạn đã đọc hết thông báo");
    }

    private void seedStarterNotifications() {
        List<Notification> starter = new ArrayList<>();
        starter.add(new Notification(
                buildNotificationId("welcome"),
                currentUserId,
                "Chào mừng bạn quay lại UniMarket",
                "Theo dõi đơn hàng, tin đăng và phản hồi từ người mua ngay tại mục thông báo.",
                "system",
                null,
                false,
                Instant.now().minusSeconds(30 * 60L).toString()
        ));

        orderService.getOrdersByBuyerId(currentUserId, new AsyncCrudService.ListCallback<Order>() {
            @Override
            public void onSuccess(List<Order> orders) {
                if (orders != null && !orders.isEmpty()) {
                    Order order = orders.get(0);
                    starter.add(buildOrderNotification(order));
                }
                addProductHintAndSave(starter);
            }

            @Override
            public void onError(String error) {
                addProductHintAndSave(starter);
            }
        });
    }

    private void addProductHintAndSave(List<Notification> starter) {
        productService.getActiveProducts(new AsyncCrudService.ListCallback<Product>() {
            @Override
            public void onSuccess(List<Product> products) {
                Product product = firstAvailableProduct(products);
                if (product != null) {
                    starter.add(new Notification(
                            buildNotificationId("product_hint"),
                            currentUserId,
                            "Có món đồ phù hợp quanh campus",
                            safeProductTitle(product) + " đang được nhiều sinh viên quan tâm. Mở Tìm kiếm để xem thêm.",
                            "product",
                            product.getId(),
                            false,
                            Instant.now().minusSeconds(3 * 60 * 60L).toString()
                    ));
                }
                saveStarterNotifications(starter);
            }

            @Override
            public void onError(String error) {
                saveStarterNotifications(starter);
            }
        });
    }

    private void saveStarterNotifications(List<Notification> starter) {
        if (starter == null || starter.isEmpty()) {
            bindNotifications(new ArrayList<>());
            return;
        }
        final int[] pending = {starter.size()};
        for (Notification notification : starter) {
            notificationService.save(notification, result -> {
                pending[0]--;
                if (pending[0] <= 0 && isAdded()) {
                    loadNotifications(false);
                    notifyHomeBadgeChanged(countUnread(starter));
                }
            });
        }
    }

    private Notification buildOrderNotification(Order order) {
        String orderId = order != null && !TextUtils.isEmpty(order.getId()) ? order.getId() : "order";
        String status = order != null ? order.getStatus() : null;
        String title;
        String body;
        if ("confirmed".equalsIgnoreCase(status) || "shipping".equalsIgnoreCase(status)) {
            title = "Đơn hàng của bạn đã được xác nhận";
            body = "Đơn hàng #" + shortId(orderId) + " đang được người bán chuẩn bị.";
        } else if ("done".equalsIgnoreCase(status)) {
            title = "Đánh giá sản phẩm ngay";
            body = "Đơn hàng #" + shortId(orderId) + " đã hoàn tất. Hãy chia sẻ trải nghiệm của bạn.";
        } else {
            title = "Đơn hàng đang chờ xử lý";
            body = "Đơn hàng #" + shortId(orderId) + " đã được ghi nhận trên UniMarket.";
        }
        return new Notification(
                buildNotificationId("order_" + orderId),
                currentUserId,
                title,
                body,
                "order",
                orderId,
                false,
                Instant.now().minusSeconds(90 * 60L).toString()
        );
    }

    private Product firstAvailableProduct(List<Product> products) {
        if (products == null) {
            return null;
        }
        for (Product product : products) {
            if (product != null && !TextUtils.isEmpty(product.getTitle())) {
                return product;
            }
        }
        return null;
    }

    private String safeProductTitle(Product product) {
        if (product == null || TextUtils.isEmpty(product.getTitle())) {
            return "Một sản phẩm mới";
        }
        return product.getTitle();
    }

    private void showLoading() {
        layoutNotificationLoading.setVisibility(View.VISIBLE);
        layoutNotificationEmpty.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.GONE);
        btnMarkAllRead.setVisibility(View.GONE);
        tvNotificationSubtitle.setText("Đang tải thông báo của bạn...");
    }

    private void showEmpty(String message) {
        layoutNotificationLoading.setVisibility(View.GONE);
        layoutNotificationEmpty.setVisibility(View.VISIBLE);
        rvNotifications.setVisibility(View.GONE);
        btnMarkAllRead.setVisibility(View.GONE);
        tvNotificationSubtitle.setText("Không có thông báo chưa đọc");
        if (tvNotificationEmptyMessage != null) {
            tvNotificationEmptyMessage.setText(message);
        }
    }

    private void notifyHomeBadgeChanged(int unreadCount) {
        if (getParentFragmentManager() != null) {
            Bundle result = new Bundle();
            result.putInt("unread_count", Math.max(0, unreadCount));
            getParentFragmentManager().setFragmentResult(RESULT_NOTIFICATIONS_CHANGED, result);
        }
    }

    private void sortNotifications(List<Notification> list) {
        Collections.sort(list, (a, b) -> compareCreatedAt(b, a));
    }

    private int compareCreatedAt(Notification left, Notification right) {
        String l = left != null ? left.getCreated_at() : null;
        String r = right != null ? right.getCreated_at() : null;
        try {
            return Instant.parse(l).compareTo(Instant.parse(r));
        } catch (Exception e) {
            if (l == null && r == null) return 0;
            if (l == null) return -1;
            if (r == null) return 1;
            return l.compareTo(r);
        }
    }

    private int countUnread(List<Notification> list) {
        int count = 0;
        if (list == null) {
            return 0;
        }
        for (Notification notification : list) {
            if (notification != null && !notification.isRead()) {
                count++;
            }
        }
        return count;
    }

    private String buildNotificationId(String suffix) {
        String safeUser = currentUserId != null ? currentUserId.replaceAll("[^A-Za-z0-9_-]", "") : "guest";
        String safeSuffix = suffix != null ? suffix.replaceAll("[^A-Za-z0-9_-]", "") : "system";
        return "notif_" + safeUser + "_" + safeSuffix;
    }

    private String shortId(String id) {
        if (TextUtils.isEmpty(id)) {
            return "----";
        }
        return id.length() <= 6 ? id : id.substring(0, 6);
    }
}
