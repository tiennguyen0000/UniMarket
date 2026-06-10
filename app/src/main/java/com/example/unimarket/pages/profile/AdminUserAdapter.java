package com.example.unimarket.pages.profile;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.auth.AccessControl;
import com.example.unimarket.data.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.AdminUserViewHolder> {
    interface ActionListener {
        void onToggleStatus(User user);
    }

    private final List<User> users = new ArrayList<>();
    private final String currentUserId;
    private final ActionListener actionListener;

    AdminUserAdapter(String currentUserId, ActionListener actionListener) {
        this.currentUserId = currentUserId;
        this.actionListener = actionListener;
    }

    void submitList(List<User> data) {
        users.clear();
        if (data != null) {
            users.addAll(data);
        }
        users.sort((a, b) -> safeName(a).compareToIgnoreCase(safeName(b)));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new AdminUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminUserViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    private static String safeName(User user) {
        if (user == null) {
            return "";
        }
        if (!TextUtils.isEmpty(user.getFull_name())) {
            return user.getFull_name();
        }
        return !TextUtils.isEmpty(user.getId()) ? user.getId() : "";
    }

    class AdminUserViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAvatar;
        private final TextView tvName;
        private final TextView tvMeta;
        private final TextView tvAction;

        AdminUserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAdminUserAvatar);
            tvName = itemView.findViewById(R.id.tvAdminUserName);
            tvMeta = itemView.findViewById(R.id.tvAdminUserMeta);
            tvAction = itemView.findViewById(R.id.tvAdminUserAction);
        }

        void bind(User user) {
            tvName.setText(safeName(user));
            tvMeta.setText(buildMeta(user));

            Glide.with(itemView.getContext())
                    .load(user != null && !TextUtils.isEmpty(user.getAvatar_url())
                            ? user.getAvatar_url()
                            : R.drawable.ic_user_placeholder)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(ivAvatar);

            boolean self = user != null && user.getId() != null && user.getId().equals(currentUserId);
            boolean suspended = user != null
                    && AccessControl.STATUS_SUSPENDED.equalsIgnoreCase(user.getAccount_status());

            tvAction.setEnabled(!self);
            tvAction.setText(self ? "Bạn" : suspended ? "Mở khóa" : "Khóa");
            tvAction.setTextColor(self ? 0xFF667085 : suspended ? 0xFF027A48 : 0xFFB42318);
            tvAction.setOnClickListener(!self && actionListener != null
                    ? v -> actionListener.onToggleStatus(user)
                    : null);
        }

        private String buildMeta(User user) {
            String role = !TextUtils.isEmpty(user.getRole()) ? user.getRole() : AccessControl.ROLE_USER;
            String status = AccessControl.isActive(user) ? "đang hoạt động" : "đang bị khóa";
            String verified = user != null && user.isVerified() ? "đã xác thực" : "chưa xác thực";
            return role.toUpperCase(Locale.ROOT) + " · " + verified + " · " + status;
        }
    }
}
