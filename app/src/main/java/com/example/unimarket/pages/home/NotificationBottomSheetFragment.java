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
import com.example.unimarket.data.service.NotificationService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class NotificationBottomSheetFragment extends BottomSheetDialogFragment {
    public static final String RESULT_NOTIFICATIONS_CHANGED = "notifications_changed";

    private final NotificationService notificationService = new NotificationService();

    private TextView tvNotificationSubtitle;
    private TextView btnMarkAllRead;
    private View layoutNotificationLoading;
    private View layoutNotificationEmpty;
    private TextView tvNotificationEmptyMessage;
    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;

    private final List<Notification> notifications = new ArrayList<>();
    private String currentUserId;

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

        loadNotifications();
    }

    private void loadNotifications() {
        showLoading();
        notificationService.getNotificationsByUserId(currentUserId, new AsyncCrudService.ListCallback<Notification>() {
            @Override
            public void onSuccess(List<Notification> data) {
                if (!isAdded()) {
                    return;
                }
                List<Notification> loaded = data != null ? data : new ArrayList<>();
                sortNotifications(loaded);
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
            notifyHomeBadgeChanged(countUnread(notifications));
            notificationService.save(notification, result -> {});
            adapter.notifyDataSetChanged();
            updateSubtitleOnly();
        }

        openNotificationTarget(notification);
    }

    private void openNotificationTarget(Notification notification) {
        String type = notification.getType() != null
                ? notification.getType().toLowerCase(Locale.ROOT) : "";
        androidx.fragment.app.Fragment parent = getParentFragment();
        if (!isAdded() || parent == null) {
            return;
        }
        if (type.contains("order") || type.contains("review")) {
            dismiss();
            NavHostFragment.findNavController(parent).navigate(R.id.ordersFragment);
        } else if (type.contains("product") || type.contains("price") || type.contains("listing")) {
            dismiss();
            NavHostFragment.findNavController(parent).navigate(R.id.searchFragment);
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
            for (Notification notification : changedNotifications) {
                notificationService.save(notification, result -> {});
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
        if (!isAdded()) {
            return;
        }
        Bundle result = new Bundle();
        result.putInt("unread_count", Math.max(0, unreadCount));
        getParentFragmentManager().setFragmentResult(RESULT_NOTIFICATIONS_CHANGED, result);
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
}
