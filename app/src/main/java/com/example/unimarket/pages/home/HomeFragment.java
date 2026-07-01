package com.example.unimarket.pages.home;

import android.graphics.Color;
import android.content.Intent;
import android.net.Uri;
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
import androidx.fragment.app.Fragment;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Notification;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.NotificationService;
import com.example.unimarket.data.service.SavedSearchAlertSync;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.pages.chat.ChatInboxBottomSheetFragment;
import com.example.unimarket.pages.post.PostListingFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private static final String SURVEY_FORM_URL =
            "https://docs.google.com/forms/d/e/1FAIpQLSeQ8zFEeuqJF7GQrGL4aT-dM6gousilPE9yo3fqIcs2--_4ZA/viewform?usp=publish-editor";

    private TextView tvHomeGreeting;
    private TextView tvUserName;
    private TextView tvHomeSubtitle;
    private TextView tvHomeUniversity;
    private TextView tvHomeVerifiedBadge;
    private ImageView ivAvatar;
    private TextView tvAvatar;
    private ImageView layoutSearch;
    private ImageView layoutNotification;
    private TextView tvNotificationBadge;
    private TextView tvViewAll;
    private View btnHomeSearchPrimary;
    private View btnHomePostPrimary;
    private View cardHomeOrders;
    private View cardHomeMessages;
    private View cardHomeProfile;
    private View cardHomeOffers;
    private View cardHomeVerifyMission;
    private View cardHomeSurveyMission;
    private View cardHomeSellerMission;
    private TextView tvHomeMissionStatus;
    private View layoutHomeLoading;
    private View homeAppBarLayout;
    private NestedScrollView homeScrollView;

    private RecyclerView rvCategories;
    private CategoryAdapter categoryAdapter;
    private HomeViewModel homeViewModel;

    private final UserService userService = new UserService();
    private final NotificationService notificationService = new NotificationService();
    private final SavedSearchAlertSync savedSearchAlertSync = new SavedSearchAlertSync();
    private final List<Category> categoryList = new ArrayList<>();
    private boolean isExpandedCategories = false;
    private boolean homeBarHidden;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupLightSystemBars();

        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        initViews(view);
        setupRecyclerViews();
        setupUserInfo();
        setupClicks();
        setupHomeBarAutoHide();
        setupRefreshListener();
        setupObservers();
        homeViewModel.loadHomeData();
        syncSavedSearchAlerts();
        refreshUnreadNotificationBadge();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncSavedSearchAlerts();
        refreshUnreadNotificationBadge();
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

    private void initViews(View root) {
        tvHomeGreeting = root.findViewById(R.id.tvHomeGreeting);
        tvUserName = root.findViewById(R.id.tvUserName);
        tvHomeSubtitle = root.findViewById(R.id.tvHomeSubtitle);
        tvHomeUniversity = root.findViewById(R.id.tvHomeUniversity);
        tvHomeVerifiedBadge = root.findViewById(R.id.tvHomeVerifiedBadge);
        ivAvatar = root.findViewById(R.id.ivAvatar);
        tvAvatar = root.findViewById(R.id.tvAvatar);
        layoutSearch = root.findViewById(R.id.layoutSearch);
        layoutNotification = root.findViewById(R.id.layoutNotification);
        tvNotificationBadge = root.findViewById(R.id.tvNotificationBadge);
        tvViewAll = root.findViewById(R.id.tvViewAll);
        btnHomeSearchPrimary = root.findViewById(R.id.btnHomeSearchPrimary);
        btnHomePostPrimary = root.findViewById(R.id.btnHomePostPrimary);
        cardHomeOrders = root.findViewById(R.id.cardHomeOrders);
        cardHomeMessages = root.findViewById(R.id.cardHomeMessages);
        cardHomeProfile = root.findViewById(R.id.cardHomeProfile);
        cardHomeOffers = root.findViewById(R.id.cardHomeOffers);
        cardHomeVerifyMission = root.findViewById(R.id.cardHomeVerifyMission);
        cardHomeSurveyMission = root.findViewById(R.id.cardHomeSurveyMission);
        cardHomeSellerMission = root.findViewById(R.id.cardHomeSellerMission);
        tvHomeMissionStatus = root.findViewById(R.id.tvHomeMissionStatus);
        layoutHomeLoading = root.findViewById(R.id.layoutHomeLoading);
        homeAppBarLayout = root.findViewById(R.id.homeAppBarLayout);
        homeScrollView = root.findViewById(R.id.homeScrollView);
        rvCategories = root.findViewById(R.id.rvCategories);
    }

    private void setupHomeBarAutoHide() {
        if (homeScrollView == null || homeAppBarLayout == null) {
            return;
        }
        homeScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    int deltaY = scrollY - oldScrollY;
                    if (scrollY <= dpToPx(8)) {
                        showHomeBar();
                    } else if (deltaY > 2) {
                        float hideDistance = Math.max(dpToPx(92), homeAppBarLayout.getHeight());
                        float progress = Math.min(1f, scrollY / hideDistance);
                        applyHomeBarProgress(progress);
                    }
                });
    }

    private void applyHomeBarProgress(float progress) {
        if (homeAppBarLayout == null) {
            return;
        }
        homeBarHidden = progress >= 1f;
        homeAppBarLayout.animate().cancel();
        homeAppBarLayout.setAlpha(1f - progress);
        homeAppBarLayout.setTranslationY(-homeAppBarLayout.getHeight() * progress);
        homeAppBarLayout.setVisibility(progress >= 1f ? View.INVISIBLE : View.VISIBLE);
    }

    private void showHomeBar() {
        if (homeAppBarLayout == null || !homeBarHidden && homeAppBarLayout.getAlpha() >= 1f) {
            return;
        }
        homeBarHidden = false;
        homeAppBarLayout.setVisibility(View.VISIBLE);
        homeAppBarLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(160)
                .start();
    }

    private void setupRecyclerViews() {
        categoryAdapter = new CategoryAdapter(new ArrayList<>(), this::navigateToSearch);
        rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        rvCategories.setNestedScrollingEnabled(false);
        rvCategories.setAdapter(categoryAdapter);
        rvCategories.addItemDecoration(new GridSpacingItemDecoration(4, dpToPx(8), true));
        isExpandedCategories = false;
    }

    private void setupObservers() {
        homeViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (!isAdded() || state == null) {
                return;
            }

            setHomeLoading(state.isLoading());

            categoryList.clear();
            List<Category> categories = state.getCategories();
            if (categories != null) {
                categoryList.addAll(categories);
            }

            showCurrentCategoryMode();
        });

        homeViewModel.getUiEvent().observe(getViewLifecycleOwner(), event -> {
            if (!isAdded()) {
                return;
            }
            if (event != null && !TextUtils.isEmpty(event.getMessage())) {
                Toast.makeText(requireContext(), event.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUserInfo() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String initialName = resolveInitialUserName(firebaseUser);
        String initialAvatarUrl = resolveInitialAvatarUrl(firebaseUser);

        bindHeaderUser(initialName, initialAvatarUrl, null, false);

        if (firebaseUser == null || TextUtils.isEmpty(firebaseUser.getUid())) {
            return;
        }

        userService.fetchById(firebaseUser.getUid(), result -> {
            if (!isAdded() || getView() == null || !result.isSuccess() || result.getData() == null) {
                return;
            }

            User profile = result.getData();
            String profileName = normalizeDisplayName(profile.getFull_name());
            String name = !TextUtils.isEmpty(profileName) ? profileName : initialName;
            String avatarUrl = firstNonEmpty(profile.getAvatar_url(), initialAvatarUrl);
            bindHeaderUser(name, avatarUrl, profile.getUniversity(), profile.isVerified());
        });
    }

    private String resolveInitialUserName(FirebaseUser firebaseUser) {
        String displayName = firebaseUser != null ? normalizeDisplayName(firebaseUser.getDisplayName()) : null;
        if (!TextUtils.isEmpty(displayName)) {
            return displayName;
        }

        Bundle args = getArguments();
        if (args != null) {
            String argName = normalizeDisplayName(args.getString("user_name"));
            if (!TextUtils.isEmpty(argName)) {
                return argName;
            }
        }

        if (requireActivity().getIntent() != null) {
            String intentName = normalizeDisplayName(requireActivity().getIntent().getStringExtra("user_name"));
            if (!TextUtils.isEmpty(intentName)) {
                return intentName;
            }
        }

        String emailName = firebaseUser != null ? buildNameFromEmail(firebaseUser.getEmail()) : null;
        return !TextUtils.isEmpty(emailName) ? emailName : "Bạn";
    }

    private String resolveInitialAvatarUrl(FirebaseUser firebaseUser) {
        if (firebaseUser != null && firebaseUser.getPhotoUrl() != null) {
            return firebaseUser.getPhotoUrl().toString();
        }

        Bundle args = getArguments();
        if (args != null && !TextUtils.isEmpty(args.getString("user_avatar"))) {
            return args.getString("user_avatar");
        }

        if (requireActivity().getIntent() != null) {
            return requireActivity().getIntent().getStringExtra("user_avatar");
        }

        return null;
    }

    private void bindHeaderUser(String name, String avatarUrl, String university, boolean verified) {
        String safeName = !TextUtils.isEmpty(name) ? name.trim() : "Bạn";
        String safeUniversity = !TextUtils.isEmpty(university) ? university.trim() : "UniMarket Campus";

        if (tvHomeGreeting != null) {
            tvHomeGreeting.setText(buildGreeting());
        }
        tvUserName.setText(safeName);
        if (tvHomeSubtitle != null) {
            tvHomeSubtitle.setText(verified
                    ? "Hồ sơ đã xác thực"
                    : "Khu chợ sinh viên của bạn");
        }
        if (tvHomeUniversity != null) {
            tvHomeUniversity.setText(safeUniversity);
        }
        if (tvHomeVerifiedBadge != null) {
            tvHomeVerifiedBadge.setVisibility(verified ? View.VISIBLE : View.GONE);
        }
        if (tvHomeMissionStatus != null) {
            tvHomeMissionStatus.setText(verified ? "Đã mở VERIFIED15" : "Mở mã");
        }

        if (!TextUtils.isEmpty(avatarUrl)) {
            tvAvatar.setVisibility(View.GONE);
            ivAvatar.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivAvatar);
        } else {
            Glide.with(this).clear(ivAvatar);
            ivAvatar.setImageDrawable(null);
            tvAvatar.setText(HomeUiUtils.extractInitial(safeName));
            tvAvatar.setVisibility(View.VISIBLE);
        }
    }

    private String buildGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 11) {
            return "Chào buổi sáng,";
        }
        if (hour >= 11 && hour < 14) {
            return "Chào buổi trưa,";
        }
        if (hour >= 14 && hour < 18) {
            return "Chào buổi chiều,";
        }
        return "Chào buổi tối,";
    }

    private String normalizeDisplayName(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty() || isGenericName(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private boolean isGenericName(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.equals("người dùng")
                || lower.equals("nguoi dung")
                || lower.equals("unimarket user")
                || lower.equals("user")
                || lower.equals("null");
    }

    private String buildNameFromEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return null;
        }
        int atIndex = email.indexOf('@');
        String localPart = atIndex > 0 ? email.substring(0, atIndex) : email;
        String cleaned = localPart.replaceAll("[._-]+", " ").trim();
        return !cleaned.isEmpty() ? titleCase(cleaned) : null;
    }

    private String titleCase(String value) {
        String[] parts = value.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(lower.charAt(0)));
            if (lower.length() > 1) {
                builder.append(lower.substring(1));
            }
        }
        return builder.toString();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private void setupClicks() {
        layoutSearch.setOnClickListener(v -> navigateToSearch());
        btnHomeSearchPrimary.setOnClickListener(v -> navigateToSearch());
        btnHomePostPrimary.setOnClickListener(v -> navigateToPostListing());
        cardHomeOrders.setOnClickListener(v -> navigateToOrders());
        cardHomeMessages.setOnClickListener(v -> showChatInbox());
        cardHomeProfile.setOnClickListener(v -> navigateToProfile());
        cardHomeOffers.setOnClickListener(v -> showOffersDialog());
        if (cardHomeVerifyMission != null) {
            cardHomeVerifyMission.setOnClickListener(v -> navigateToProfile());
        }
        if (cardHomeSurveyMission != null) {
            cardHomeSurveyMission.setOnClickListener(v -> openSurveyForm());
        }
        if (cardHomeSellerMission != null) {
            cardHomeSellerMission.setOnClickListener(v -> navigateToPostListing());
        }
        layoutNotification.setOnClickListener(v -> showNotificationBottomSheet());
        tvViewAll.setOnClickListener(v -> toggleCategories());
    }

    private void setupRefreshListener() {
        getParentFragmentManager().setFragmentResultListener(
                PostListingFragment.RESULT_LISTING_CREATED,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    homeViewModel.refreshCatalogData();
                    syncSavedSearchAlerts();
                }
        );
        getChildFragmentManager().setFragmentResultListener(
                NotificationBottomSheetFragment.RESULT_NOTIFICATIONS_CHANGED,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    if (result != null && result.containsKey("unread_count")) {
                        bindNotificationBadge(result.getInt("unread_count"));
                    } else {
                        refreshUnreadNotificationBadge();
                    }
                }
        );
    }

    private void navigateToSearch() {
        NavHostFragment.findNavController(this).navigate(R.id.searchFragment);
    }

    private void navigateToSearch(Category category) {
        Bundle args = new Bundle();
        if (category != null) {
            args.putString("category_id", category.getId());
            args.putString("category_name", category.getName());
        }
        NavHostFragment.findNavController(this).navigate(R.id.searchFragment, args);
    }

    private void navigateToPostListing() {
        NavHostFragment.findNavController(this).navigate(R.id.postListingFragment);
    }

    private void navigateToOrders() {
        NavHostFragment.findNavController(this).navigate(R.id.ordersFragment);
    }

    private void navigateToProfile() {
        NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
    }

    private void showChatInbox() {
        new ChatInboxBottomSheetFragment().show(getParentFragmentManager(), "home_chat_inbox");
    }

    private void showOffersDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ưu đãi mã giảm")
                .setMessage("WELCOME10: giảm 10% cho đơn từ 50.000đ\n"
                        + "FREESHIP: miễn 10.000đ phí ship cho đơn từ 80.000đ\n"
                        + "BOOK25: giảm 25% cho đơn sách từ 120.000đ\n"
                        + "CAMPUS5: giảm 5.000đ cho đơn bất kỳ\n"
                        + "VERIFIED15: giảm 15% cho tài khoản đã xác thực\n"
                        + "SURVEY10: giảm 10.000đ sau khảo sát campus\n"
                        + "SELLER20: giảm 20% khi bạn đăng tin đầu tiên\n\n"
                        + "Các mã do hệ thống ưu đãi, không trừ vào doanh thu người bán.")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void openSurveyForm() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SURVEY_FORM_URL));
        startActivity(intent);
    }

    private void showNotificationBottomSheet() {
        NotificationBottomSheetFragment bottomSheet = new NotificationBottomSheetFragment();
        bottomSheet.show(getChildFragmentManager(), "notifications");
    }

    private void refreshUnreadNotificationBadge() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || TextUtils.isEmpty(user.getUid()) || tvNotificationBadge == null) {
            if (tvNotificationBadge != null) {
                tvNotificationBadge.setVisibility(View.GONE);
            }
            return;
        }

        notificationService.getNotificationsByUserId(user.getUid(), new AsyncCrudService.ListCallback<Notification>() {
            @Override
            public void onSuccess(List<Notification> data) {
                if (!isAdded() || tvNotificationBadge == null) {
                    return;
                }
                int unreadCount = 0;
                if (data != null) {
                    for (Notification notification : data) {
                        if (notification != null && !notification.isRead()) {
                            unreadCount++;
                        }
                    }
                }
                bindNotificationBadge(unreadCount);
            }

            @Override
            public void onError(String error) {
                if (isAdded() && tvNotificationBadge != null) {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }
        });
    }

    private void syncSavedSearchAlerts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || TextUtils.isEmpty(user.getUid())) {
            return;
        }
        savedSearchAlertSync.sync(user.getUid(), this::refreshUnreadNotificationBadge);
    }

    private void bindNotificationBadge(int unreadCount) {
        if (unreadCount <= 0) {
            tvNotificationBadge.setVisibility(View.GONE);
            return;
        }
        tvNotificationBadge.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
        tvNotificationBadge.setVisibility(View.VISIBLE);
    }

    private void toggleCategories() {
        isExpandedCategories = !isExpandedCategories;
        showCurrentCategoryMode();
    }

    private void showCurrentCategoryMode() {
        List<Category> displayCategories = new ArrayList<>();
        if (isExpandedCategories) {
            displayCategories.addAll(categoryList);
        } else {
            for (int i = 0; i < Math.min(4, categoryList.size()); i++) {
                displayCategories.add(categoryList.get(i));
            }
        }

        categoryAdapter.submitList(displayCategories);
        updateViewAllText();
    }

    private void setHomeLoading(boolean loading) {
        if (layoutHomeLoading == null || rvCategories == null) {
            return;
        }
        layoutHomeLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvCategories.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void updateViewAllText() {
        if (categoryList.size() > 4) {
            tvViewAll.setVisibility(View.VISIBLE);
            tvViewAll.setText(isExpandedCategories ? "Ẩn bớt" : "Xem tất cả");
        } else {
            tvViewAll.setVisibility(View.GONE);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacing;
        private final boolean includeEdge;

        GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }
}
