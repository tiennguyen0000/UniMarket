package com.example.unimarket.pages.profile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.MainActivity;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.User;
import com.example.unimarket.pages.home.HomeUiUtils;
import com.example.unimarket.pages.home.ProductDetailBottomSheetFragment;
import com.example.unimarket.pages.post.PostListingFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

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
        setupLightSystemBars();

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

        ordersAdapter = new OrdersInProfileAdapter(this::showOrderDetailDialog);
        postsAdapter = new UserPostAdapter(this::openPostDetail);

        rvContent.setNestedScrollingEnabled(false);

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        observeViewModel();
        setupRefreshListener();
        loadUserProfile();
        setupListeners();
        switchTab(0);
    }

    private void setupLightSystemBars() {
        requireActivity().getWindow().setStatusBarColor(Color.WHITE);
        requireActivity().getWindow().setNavigationBarColor(Color.WHITE);

        int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        requireActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;
        profileViewModel.loadProfile(firebaseUser.getUid(), firebaseUser.getDisplayName());
    }

    private void setupRefreshListener() {
        getParentFragmentManager().setFragmentResultListener(
                PostListingFragment.RESULT_LISTING_CREATED,
                getViewLifecycleOwner(),
                (requestKey, result) -> loadUserProfile()
        );
    }

    private void setupListeners() {
        ivSettings.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnShare.setOnClickListener(v -> shareProfile());

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
            rvContent.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvContent.setAdapter(ordersAdapter);
            if (state != null) ordersAdapter.submitList(state.getOrders());
        } else {
            rvContent.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            rvContent.setAdapter(postsAdapter);
            if (state != null) postsAdapter.submitList(state.getPosts());
        }
    }

    private void observeViewModel() {
        profileViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;

            tvOrderCount.setText(String.valueOf(state.getOrders().size()));
            tvPostCount.setText(String.valueOf(state.getPosts().size()));
            tvRating.setText(state.getRatingCount() > 0
                    ? String.format(Locale.getDefault(), "%.1f", state.getRatingAverage())
                    : "-");

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
            if (event != null) {
                Toast.makeText(requireContext(), event.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditProfileDialog() {
        ProfileUiState state = profileViewModel.getUiState().getValue();
        User user = state != null ? state.getProfile() : null;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_profile, null, false);
        TextInputEditText etFullName = dialogView.findViewById(R.id.etEditFullName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etEditPhone);
        TextInputEditText etUniversity = dialogView.findViewById(R.id.etEditUniversity);

        if (user != null) {
            etFullName.setText(user.getFull_name());
            etPhone.setText(user.getPhone());
            etUniversity.setText(user.getUniversity());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chỉnh sửa hồ sơ")
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser == null) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String university = etUniversity.getText() != null ? etUniversity.getText().toString().trim() : "";

            if (TextUtils.isEmpty(fullName)) {
                etFullName.setError("Vui lòng nhập họ và tên");
                return;
            }

            profileViewModel.saveProfile(firebaseUser.getUid(), fullName, phone, university);
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void shareProfile() {
        ProfileUiState state = profileViewModel.getUiState().getValue();
        User user = state != null ? state.getProfile() : null;
        if (user == null) {
            Toast.makeText(requireContext(), "Chưa có thông tin hồ sơ để chia sẻ", Toast.LENGTH_SHORT).show();
            return;
        }

        String displayName = !TextUtils.isEmpty(user.getFull_name()) ? user.getFull_name() : "UniMarket User";
        String university = !TextUtils.isEmpty(user.getUniversity()) ? user.getUniversity() : "UniMarket";
        String ratingText = state != null && state.getRatingCount() > 0
                ? String.format(Locale.getDefault(), "%.1f/5", state.getRatingAverage())
                : "Chưa có đánh giá";

        String shareText = displayName
                + " - " + university
                + "\nTin đăng: " + (state != null ? state.getPosts().size() : 0)
                + "\nĐơn hàng: " + (state != null ? state.getOrders().size() : 0)
                + "\nĐánh giá: " + ratingText
                + "\nHồ sơ trên UniMarket";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Hồ sơ UniMarket");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ hồ sơ"));
    }

    private void openPostDetail(Product product) {
        if (product == null) {
            return;
        }
        String imageUrl = null;
        if (product.getImage_urls() != null && !product.getImage_urls().isEmpty()) {
            imageUrl = product.getImage_urls().get(0);
        }
        String categoryName = !TextUtils.isEmpty(product.getCategory_id())
                ? product.getCategory_id().replace("cat_", "") : "Tin đăng của tôi";
        ProductDetailBottomSheetFragment.newInstance(product, imageUrl, categoryName)
                .show(getChildFragmentManager(), "profile_post_detail");
    }

    private void showOrderDetailDialog(Order order) {
        if (order == null) {
            return;
        }

        String orderId = !TextUtils.isEmpty(order.getId())
                ? "#UM" + order.getId().substring(0, Math.min(6, order.getId().length())).toUpperCase(Locale.ROOT)
                : "#UM";
        int quantity = order.getQuantity() != null ? order.getQuantity() : 1;
        double unitPrice = order.getUnit_price() != null ? order.getUnit_price() : 0;
        double discount = order.getDiscount_amount() != null ? order.getDiscount_amount() : 0;

        StringBuilder message = new StringBuilder();
        message.append("Sản phẩm: ")
                .append(!TextUtils.isEmpty(order.getProduct_title()) ? order.getProduct_title() : "Sản phẩm")
                .append("\n");
        message.append("Trạng thái: ").append(statusLabel(order.getStatus())).append("\n");
        message.append("Số lượng: ").append(quantity).append("\n");
        if (unitPrice > 0) {
            message.append("Đơn giá: ").append(HomeUiUtils.formatPrice(unitPrice)).append("\n");
        }
        if (!TextUtils.isEmpty(order.getDiscount_code()) || discount > 0) {
            message.append("Mã giảm giá: ")
                    .append(!TextUtils.isEmpty(order.getDiscount_code()) ? order.getDiscount_code() : "Đã áp dụng")
                    .append("\n");
            message.append("Giảm: ").append(HomeUiUtils.formatPrice(discount)).append("\n");
        }
        message.append("Tổng thanh toán: ").append(HomeUiUtils.formatPrice(order.getTotal_price()));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chi tiết đơn " + orderId)
                .setMessage(message.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }

    private String statusLabel(String status) {
        if (TextUtils.isEmpty(status)) {
            return "Chờ xác nhận";
        }
        switch (status.toLowerCase(Locale.ROOT)) {
            case "pending": return "Chờ xác nhận";
            case "confirmed": return "Đã xác nhận";
            case "shipping": return "Đang giao";
            case "done": return "Hoàn thành";
            case "cancelled": return "Đã hủy";
            default: return status;
        }
    }
}
