package com.example.unimarket.pages.home;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.Notification;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private final List<Notification> notifications = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Notification> newNotifications) {
        notifications.clear();
        if (newNotifications != null) {
            notifications.addAll(newNotifications);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        boolean unread = notification != null && !notification.isRead();

        holder.itemRoot.setBackgroundResource(unread
                ? R.drawable.bg_notification_item_unread
                : R.drawable.bg_notification_item_read);
        holder.unreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);
        holder.title.setText(resolveTitle(notification));
        holder.content.setText(resolveContent(notification));
        holder.time.setText(formatRelativeTime(notification != null ? notification.getCreated_at() : null));
        holder.title.setTextColor(unread ? 0xFF101828 : 0xFF475467);
        holder.content.setTextColor(unread ? 0xFF667085 : 0xFF98A2B3);
        holder.icon.setAlpha(unread ? 1f : 0.58f);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && notification != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private String resolveTitle(Notification notification) {
        if (notification == null) {
            return "Thông báo hệ thống";
        }
        if (!TextUtils.isEmpty(notification.getTitle())) {
            return notification.getTitle();
        }
        String content = notification.getContent();
        if (TextUtils.isEmpty(content)) {
            return "Thông báo hệ thống";
        }
        int split = content.indexOf('.');
        if (split <= 0) {
            split = content.indexOf('-');
        }
        if (split > 0) {
            return content.substring(0, split).trim();
        }
        return content.length() > 48 ? content.substring(0, 48).trim() + "..." : content;
    }

    private String resolveContent(Notification notification) {
        if (notification == null || TextUtils.isEmpty(notification.getContent())) {
            return "UniMarket vừa có cập nhật mới dành cho bạn.";
        }
        return notification.getContent();
    }

    private String formatRelativeTime(String createdAt) {
        if (TextUtils.isEmpty(createdAt)) {
            return "Vừa xong";
        }
        try {
            Instant created = Instant.parse(createdAt);
            Duration duration = Duration.between(created, Instant.now());
            long minutes = Math.max(0, duration.toMinutes());
            if (minutes < 1) {
                return "Vừa xong";
            }
            if (minutes < 60) {
                return minutes + " phút trước";
            }
            long hours = duration.toHours();
            if (hours < 24) {
                return hours + " giờ trước";
            }
            long days = duration.toDays();
            if (days < 7) {
                return days + " ngày trước";
            }
            long weeks = days / 7;
            if (weeks < 5) {
                return weeks + " tuần trước";
            }
            return createdAt.substring(0, Math.min(10, createdAt.length()));
        } catch (DateTimeParseException e) {
            return createdAt;
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final View itemRoot;
        final ImageView icon;
        final View unreadDot;
        final TextView title;
        final TextView content;
        final TextView time;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            itemRoot = itemView.findViewById(R.id.layoutNotificationItem);
            icon = itemView.findViewById(R.id.ivNotificationIcon);
            unreadDot = itemView.findViewById(R.id.viewUnreadDot);
            title = itemView.findViewById(R.id.tvNotificationTitle);
            content = itemView.findViewById(R.id.tvNotificationContent);
            time = itemView.findViewById(R.id.tvNotificationTime);
        }
    }
}