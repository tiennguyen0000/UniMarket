package com.example.unimarket.pages.profile;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.StudentVerification;

import java.util.ArrayList;
import java.util.List;

public class AdminVerificationAdapter extends RecyclerView.Adapter<AdminVerificationAdapter.ViewHolder> {
    public interface OnRequestLongPress {
        void onOpenDetails(StudentVerification request);
    }

    private final List<StudentVerification> items = new ArrayList<>();
    private final OnRequestLongPress onRequestLongPress;

    public AdminVerificationAdapter(OnRequestLongPress onRequestLongPress) {
        this.onRequestLongPress = onRequestLongPress;
    }

    public void submitList(List<StudentVerification> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_verification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentVerification request = items.get(position);
        holder.tvUserName.setText(displayName(request));
        holder.tvStudentId.setText("MSSV: " + displayStudentId(request));
        holder.itemView.setOnLongClickListener(v -> {
            if (onRequestLongPress != null) {
                onRequestLongPress.onOpenDetails(request);
            }
            return true;
        });
    }

    private String displayName(StudentVerification request) {
        if (request != null && !TextUtils.isEmpty(request.getUser_name())) {
            return request.getUser_name();
        }
        return "Người dùng chưa có tên";
    }

    private String displayStudentId(StudentVerification request) {
        if (request != null && !TextUtils.isEmpty(request.getStudent_id())) {
            return request.getStudent_id();
        }
        return "Chưa có";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvUserName;
        final TextView tvStudentId;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvVerificationUserName);
            tvStudentId = itemView.findViewById(R.id.tvVerificationStudentId);
        }
    }
}
