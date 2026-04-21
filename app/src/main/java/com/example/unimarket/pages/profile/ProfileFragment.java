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
import androidx.recyclerview.widget.GridLayoutManager;
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
    private OrdersInProfileAdapter ordersAdapter;
    private UserPostAdapter postsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName         = view.findViewById(R.id.tvName);
        tvUniversity   = view.findViewById(R.id.tvUniversity);
        tvVerifyStatus = view.findViewById(R.id.tvVerifyStatus);
        tvOrderCount   = view.findViewById(R.id.tvOrderCount);
        tvPostCount    = view.findViewById(R.id.tvPostCount);
        tvRating       = view.findViewById(R.id.tvRating);
        ivAvatar       = view.findViewById(R.id.ivAvatar);
        ivSettings     = view.findViewById(R.id.ivSettings);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnShare       = view.findViewById(R.id.btnShare);
        tabLayout      = view.findViewById(R.id.tabLayout);
        rvContent      = view.findViewById(R.id.rvContent);

        // Adapters
        ordersAdapter = new OrdersInProfileAdapter();
        postsAdapter  = new UserPostAdapter(product ->
                Toast.makeText(requireContext(), product.getTitle(), Toast.LENGTH_SHORT).show());

        rvContent.setNestedScrollingEnabled(false);

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        observeViewModel();
        loadUserProfile();
        setupListeners();

        // Mặc định tab đầu (Đơn hàng)
        switchTab(0);
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;
        profileViewModel.loadProfile(firebaseUser.getUid(), firebaseUser.getDisplayName());
    }

    private void setupListeners() {
        ivSettings.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        btnEditProfile.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Chỉnh sửa hồ sơ", Toast.LENGTH_SHORT).show());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) { switchTab(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void switchTab(int position) {
        ProfileUiState state = profileViewModel.getUiState().getValue();
        if (position == 0) {
            // Tab Đơn hàng
            rvContent.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvContent.setAdapter(ordersAdapter);
            if (state != null) ordersAdapter.submitList(state.getOrders());
        } else {
            // Tab Tin đăng
            rvContent.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            rvContent.setAdapter(postsAdapter);
            if (state != null) postsAdapter.submitList(state.getPosts());
        }
    }

    private void observeViewModel() {
        profileViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;

            // Cập nhật số đếm dù profile chưa load
            tvOrderCount.setText(String.valueOf(state.getOrders().size()));
            tvPostCount.setText(String.valueOf(state.getPosts().size()));

            // Refresh adapter đang hiển thị
            int tabPos = tabLayout.getSelectedTabPosition();
            if (tabPos == 0) {
                ordersAdapter.submitList(state.getOrders());
            } else {
                postsAdapter.submitList(state.getPosts());
            }

            User user = state.getProfile();
            if (user == null) return;

            tvName.setText(!TextUtils.isEmpty(user.getFull_name()) ? user.getFull_name() : "UniMarket User");
            tvUniversity.setText(!TextUtils.isEmpty(user.getUniversity()) ? user.getUniversity() : "Chưa cập nhật trường");

            if (user.is_verified()) {
                tvVerifyStatus.setText("Sinh viên đã xác thực");
                tvVerifyStatus.setTextColor(getResources().getColor(R.color.verification_green));
            } else {
                tvVerifyStatus.setText("Chưa xác thực");
                tvVerifyStatus.setTextColor(getResources().getColor(R.color.text_secondary));
            }

            if (!TextUtils.isEmpty(user.getAvatar_url())) {
                Glide.with(this)
                        .load(user.getAvatar_url())
                        .placeholder(R.drawable.ic_user_placeholder)
                        .error(R.drawable.ic_user_placeholder)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_user_placeholder);
            }
        });

        profileViewModel.getUiEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null)
                Toast.makeText(requireContext(), event.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
