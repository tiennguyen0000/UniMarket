package com.example.unimarket.pages.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.unimarket.MainActivity;
import com.example.unimarket.R;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private EditText etFullName, etPhone, etUniversity;
    private Button btnSaveProfile, btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etFullName    = view.findViewById(R.id.etFullName);
        etPhone       = view.findViewById(R.id.etPhone);
        etUniversity  = view.findViewById(R.id.etUniversity);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        btnLogout     = view.findViewById(R.id.btnLogout);

        loadUserProfile();
        setupListeners();
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        // Hiển thị tên từ Firebase trước (nhanh, không cần network)
        String displayName = firebaseUser.getDisplayName();
        if (!TextUtils.isEmpty(displayName)) {
            etFullName.setText(displayName);
        }

        // Load thêm phone/university từ Supabase profile
        AsyncCrudService.getById(
                "profiles",
                firebaseUser.getUid(),
                User.class,
                new AsyncCrudService.ItemCallback<User>() {
                    @Override
                    public void onSuccess(User data) {
                        if (!isAdded() || data == null) return;
                        if (!TextUtils.isEmpty(data.getFull_name())) {
                            etFullName.setText(data.getFull_name());
                        }
                        if (!TextUtils.isEmpty(data.getPhone())) {
                            etPhone.setText(data.getPhone());
                        }
                        if (!TextUtils.isEmpty(data.getUniversity())) {
                            etUniversity.setText(data.getUniversity());
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Profile chưa có hoặc lỗi mạng - không block UI
                    }
                }
        );
    }

    private void setupListeners() {
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void saveProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        String fullName   = etFullName.getText().toString().trim();
        String phone      = etPhone.getText().toString().trim();
        String university = etUniversity.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Vui lòng nhập họ và tên");
            return;
        }

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Đang lưu...");

        User updatedUser = new User();
        updatedUser.setId(firebaseUser.getUid());
        updatedUser.setFull_name(fullName);
        updatedUser.setPhone(phone);
        updatedUser.setUniversity(university);

        new UserService().upsertProfile(updatedUser, new AsyncCrudService.ItemCallback<User>() {
            @Override
            public void onSuccess(User data) {
                if (!isAdded()) return;
                btnSaveProfile.setEnabled(true);
                btnSaveProfile.setText("Lưu");
                Toast.makeText(requireContext(), "Đã lưu hồ sơ!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                btnSaveProfile.setEnabled(true);
                btnSaveProfile.setText("Lưu");
                Toast.makeText(requireContext(), "Lưu thất bại: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
