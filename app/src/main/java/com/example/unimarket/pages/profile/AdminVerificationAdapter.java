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
    public interface OnApproveClick {
        void onApprove(StudentVerification request);
    }

    private final List<StudentVerification> items = new ArrayList<>();
    private final OnApproveClick onApproveClick;

    public AdminVerificationAdapter(OnApproveClick onApproveClick) {
        this.onApproveClick = onApproveClick;
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
        String studentId = !TextUtils.isEmpty(request.getStudent_id())
                ? request.getStudent_id() : "Chưa có MSSV";
        holder.tvStudentId.setText(studentId);
        holder.tvMeta.setText(request.getUser_id());
        holder.tvNote.setText(!TextUtils.isEmpty(request.getNote())
                ? request.getNote() : "Không có ghi chú");
        holder.btnApprove.setOnClickListener(v -> {
            if (onApproveClick != null) {
                onApproveClick.onApprove(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvStudentId;
        final TextView tvMeta;
        final TextView tvNote;
        final TextView btnApprove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentId = itemView.findViewById(R.id.tvVerificationStudentId);
            tvMeta = itemView.findViewById(R.id.tvVerificationMeta);
            tvNote = itemView.findViewById(R.id.tvVerificationNote);
            btnApprove = itemView.findViewById(R.id.btnApproveVerification);
        }
    }
}
