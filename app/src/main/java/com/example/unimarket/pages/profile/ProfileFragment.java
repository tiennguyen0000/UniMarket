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
import androidx.lifecycle.ViewModelProvider;

import com.example.unimarket.MainActivity;
import com.example.unimarket.R;
import com.example.unimarket.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private EditText etFullName, etPhone, etUniversity;
    private Button btnSaveProfile, btnLogout;
    private ProfileViewModel profileViewModel;

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

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        observeViewModel();
        loadUserProfile();
        setupListeners();
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        String displayName = firebaseUser.getDisplayName();
        profileViewModel.loadProfile(firebaseUser.getUid(), displayName);
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

    private void observeViewModel() {
        profileViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            User data = state.getProfile();
            if (!isAdded() || data == null) {
                return;
            }
            if (!TextUtils.isEmpty(data.getFull_name())) {
                etFullName.setText(data.getFull_name());
            }
            if (!TextUtils.isEmpty(data.getPhone())) {
                etPhone.setText(data.getPhone());
            }
            if (!TextUtils.isEmpty(data.getUniversity())) {
                etUniversity.setText(data.getUniversity());
            }
            
            boolean isSaving = state.isSaving();
            btnSaveProfile.setEnabled(!isSaving);
            btnSaveProfile.setText(isSaving ? "Đang lưu..." : "Lưu");
        });

        profileViewModel.getUiEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null && isAdded() && !TextUtils.isEmpty(event.getMessage())) {
                Toast.makeText(requireContext(), event.getMessage(), Toast.LENGTH_SHORT).show();
            }
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
        profileViewModel.saveProfile(firebaseUser.getUid(), fullName, phone, university);
    }
}
