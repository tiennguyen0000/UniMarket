package com.example.unimarket.pages.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.MainActivity;
import com.example.unimarket.R;
import com.example.unimarket.data.model.User;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvUniversity, tvVerifyStatus, tvOrderCount, tvPostCount, tvRating;
    private ImageView ivAvatar, ivSettings;
    private View btnEditProfile, btnShare;
    private TabLayout tabLayout;
    private RecyclerView rvContent;
    
    private ProfileViewModel profileViewModel;

    @Nullable
    @Override
    // Nạp file XML (fragment_profile) cho Fragment
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        tvName = view.findViewById(R.id.tvName);
        tvUniversity = view.findViewById(R.id.tvUniversity);
        tvVerifyStatus = view.findViewById(R.id.tvVerifyStatus);
        tvOrderCount = view.findViewById(R.id.tvOrderCount);
        tvPostCount = view.findViewById(R.id.tvPostCount);
        tvRating = view.findViewById(R.id.tvRating);
        ivAvatar = view.findViewById(R.id.ivAvatar);
        ivSettings = view.findViewById(R.id.ivSettings);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnShare = view.findViewById(R.id.btnShare);
        tabLayout = view.findViewById(R.id.tabLayout);
        rvContent = view.findViewById(R.id.rvContent);

        // Setup RecyclerView
        rvContent.setLayoutManager(new LinearLayoutManager(requireContext()));
        // TODO: Set adapter based on selected tab

        // Thay vì để Fragment trực tiếp lấy từ db, ta sẽ lấy từ ViewModel để tránh dữ liệu không mất khi xoay màn hình.
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        // observe để khi thông tin thay đổi, giao diện cũng sẽ cập nhật theo.
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
        // Log-out
        ivSettings.setOnClickListener(v -> {
            // Show settings or Logout
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        btnEditProfile.setOnClickListener(v -> {
            // Handle edit profile navigation
            Toast.makeText(requireContext(), "Chỉnh sửa hồ sơ", Toast.LENGTH_SHORT).show();
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Show Orders
                } else {
                    // Show Posts
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void observeViewModel() {
        profileViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || state.getProfile() == null) return;

            User user = state.getProfile();
            tvName.setText(user.getFull_name());
            tvUniversity.setText(user.getUniversity());
            
            if (user.is_verified()) {
                tvVerifyStatus.setText("Sinh viên đã xác thực");
                tvVerifyStatus.setTextColor(getResources().getColor(R.color.verification_green));
            } else {
                tvVerifyStatus.setText("Chưa xác thực");
                tvVerifyStatus.setTextColor(getResources().getColor(R.color.text_secondary));
            }

            if (user.getAvatar_url() != null && !user.getAvatar_url().isEmpty()) {
                Glide.with(this)
                        .load(user.getAvatar_url())
                        .placeholder(R.drawable.ic_user_placeholder)
                        .error(R.drawable.ic_user_placeholder)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_user_placeholder);
            }
            
            // Stats (These could be fetched from VM later)
            // tvOrderCount.setText(...);
            // tvPostCount.setText(...);
        });
    }
}
