package com.example.unimarket.pages.profile;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.MainActivity;
import com.example.unimarket.R;
import com.example.unimarket.auth.AccessControl;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.StudentVerification;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.StudentVerificationService;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.util.AppErrorLogger;
import com.example.unimarket.pages.home.HomeUiUtils;
import com.example.unimarket.pages.home.ProductDetailBottomSheetFragment;
import com.example.unimarket.pages.post.PostListingFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private TextView tvName, tvUniversity, tvVerifyStatus, tvOrderCount, tvPostCount, tvRating;
    private ImageView ivAvatar, ivSettings, ivVerifyBadge;
    private View btnEditProfile, btnShare, btnRequestVerification;
    private View cardAdminConsole, btnOpenAdminConsole;
    private View cardSavedSearches, btnOpenSavedSearches;
    private View layoutProfileLoading, layoutProfileEmpty, btnProfileEmptyAction;
    private TextView tvProfileEmptyTitle, tvProfileEmptyMessage;
    private TabLayout tabLayout;
    private RecyclerView rvContent;

    private ProfileViewModel profileViewModel;
    private OrdersInProfileAdapter ordersAdapter;
    private UserPostAdapter postsAdapter;
    private UserPostAdapter savedAdapter;
    private final StudentVerificationService verificationService = new StudentVerificationService();
    private final UserService userService = new UserService();
    private final OrderService orderService = new OrderService();
    private final ProductService productService = new ProductService();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private Uri verificationFrontCardUri;
    private Uri verificationBackCardUri;
    private ImageView verificationFrontPreview;
    private ImageView verificationBackPreview;
    private TextView verificationFrontHint;
    private TextView verificationBackHint;
    private View verificationFrontUploadCard;
    private View verificationBackUploadCard;
    private String currentUserId;

    private final ActivityResultLauncher<String> pickFrontCardLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null) {
                    return;
                }
                verificationFrontCardUri = uri;
                bindVerificationCardPreview(true, uri);
            });

    private final ActivityResultLauncher<String> pickBackCardLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null) {
                    return;
                }
                verificationBackCardUri = uri;
                bindVerificationCardPreview(false, uri);
            });

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
        ivVerifyBadge = view.findViewById(R.id.ivVerifyBadge);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnShare = view.findViewById(R.id.btnShare);
        btnRequestVerification = view.findViewById(R.id.btnRequestVerification);
        cardSavedSearches = view.findViewById(R.id.cardSavedSearches);
        btnOpenSavedSearches = view.findViewById(R.id.btnOpenSavedSearches);
        cardAdminConsole = view.findViewById(R.id.cardAdminConsole);
        btnOpenAdminConsole = view.findViewById(R.id.btnOpenAdminConsole);
        layoutProfileLoading = view.findViewById(R.id.layoutProfileLoading);
        layoutProfileEmpty = view.findViewById(R.id.layoutProfileEmpty);
        btnProfileEmptyAction = view.findViewById(R.id.btnProfileEmptyAction);
        tvProfileEmptyTitle = view.findViewById(R.id.tvProfileEmptyTitle);
        tvProfileEmptyMessage = view.findViewById(R.id.tvProfileEmptyMessage);
        tabLayout = view.findViewById(R.id.tabLayout);
        rvContent = view.findViewById(R.id.rvContent);

        ordersAdapter = new OrdersInProfileAdapter(this::showOrderDetailDialog);
        postsAdapter = new UserPostAdapter(this::openPostDetail);
        savedAdapter = new UserPostAdapter(this::openPostDetail);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = firebaseUser != null ? firebaseUser.getUid() : null;

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
        currentUserId = firebaseUser.getUid();
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
        ivSettings.setOnClickListener(v -> confirmSignOut());

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnShare.setOnClickListener(v -> shareProfile());
        btnRequestVerification.setOnClickListener(v -> showVerificationRequestDialog());
        btnOpenSavedSearches.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.savedSearchesFragment));
        btnOpenAdminConsole.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.adminConsoleFragment));
        btnProfileEmptyAction.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.postListingFragment));

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
        } else if (position == 1) {
            rvContent.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            rvContent.setAdapter(postsAdapter);
            if (state != null) postsAdapter.submitList(state.getPosts());
        } else {
            rvContent.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            rvContent.setAdapter(savedAdapter);
            if (state != null) savedAdapter.submitList(state.getSavedProducts());
        }
        updateContentState(state);
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
            } else if (tabPos == 1) {
                postsAdapter.submitList(state.getPosts());
            } else {
                savedAdapter.submitList(state.getSavedProducts());
            }
            updateContentState(state);

            User user = state.getProfile();
            if (user == null) return;

            tvName.setText(!TextUtils.isEmpty(user.getFull_name()) ? user.getFull_name() : "UniMarket User");
            tvUniversity.setText(!TextUtils.isEmpty(user.getUniversity()) ? user.getUniversity() : "Chưa cập nhật trường");

            if (user.isVerified()) {
                tvVerifyStatus.setText("Sinh viên đã xác thực");
                tvVerifyStatus.setTextColor(getResources().getColor(R.color.verification_green));
            } else {
                tvVerifyStatus.setText("Chưa xác thực");
                tvVerifyStatus.setTextColor(getResources().getColor(R.color.text_secondary));
            }
            bindVerificationActions(user);
            bindAdminConsole(user);

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

    private void confirmSignOut() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đăng xuất UniMarket?")
                .setMessage("Bạn sẽ cần đăng nhập lại để đăng tin, nhắn tin và theo dõi đơn hàng.")
                .setNegativeButton("Ở lại", null)
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }

    private void bindVerificationActions(User user) {
        boolean verified = user != null && user.isVerified();
        boolean reviewer = AccessControl.isModerator(user);

        ivVerifyBadge.setVisibility(verified ? View.VISIBLE : View.GONE);
        btnRequestVerification.setVisibility(!verified && !reviewer ? View.VISIBLE : View.GONE);
    }

    private void bindAdminConsole(User user) {
        cardAdminConsole.setVisibility(AccessControl.isModerator(user) ? View.VISIBLE : View.GONE);
    }

    private void showVerificationRequestDialog() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        ProfileUiState state = profileViewModel.getUiState().getValue();
        User profile = state != null ? state.getProfile() : null;
        if (firebaseUser == null || profile == null) {
            AppErrorLogger.append(requireContext(), "profile.verify.precheck",
                    "Cannot open verification request. auth=" + (firebaseUser != null)
                            + ", profile=" + (profile != null));
            Toast.makeText(requireContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        verificationFrontCardUri = null;
        verificationBackCardUri = null;

        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_student_verification, null, false);
        TextInputEditText etStudentId = content.findViewById(R.id.etVerificationStudentId);
        TextInputEditText etFullName = content.findViewById(R.id.etVerificationFullName);
        ProgressBar progress = content.findViewById(R.id.progressVerificationUpload);
        TextView btnClose = content.findViewById(R.id.btnVerificationClose);
        TextView btnSubmit = content.findViewById(R.id.btnVerificationSubmit);
        verificationFrontPreview = content.findViewById(R.id.ivFrontCardPreview);
        verificationBackPreview = content.findViewById(R.id.ivBackCardPreview);
        verificationFrontHint = content.findViewById(R.id.tvFrontCardHint);
        verificationBackHint = content.findViewById(R.id.tvBackCardHint);
        verificationFrontUploadCard = content.findViewById(R.id.layoutFrontCardUpload);
        verificationBackUploadCard = content.findViewById(R.id.layoutBackCardUpload);
        etFullName.setText(!TextUtils.isEmpty(profile.getFull_name())
                ? profile.getFull_name() : resolveVerificationUserName(profile, firebaseUser));

        verificationFrontUploadCard.setOnClickListener(v -> pickFrontCardLauncher.launch("image/*"));
        verificationBackUploadCard.setOnClickListener(v -> pickBackCardLauncher.launch("image/*"));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String studentId = etStudentId.getText() != null ? etStudentId.getText().toString().trim() : "";
            String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
            if (TextUtils.isEmpty(studentId)) {
                etStudentId.setError("Nhập MSSV");
                return;
            }
            if (TextUtils.isEmpty(fullName)) {
                etFullName.setError("Nhập họ và tên");
                return;
            }
            if (verificationFrontCardUri == null || verificationBackCardUri == null) {
                Toast.makeText(requireContext(), "Vui lòng chọn đủ 2 mặt thẻ sinh viên.", Toast.LENGTH_SHORT).show();
                return;
            }

            StudentVerification request = new StudentVerification();
            request.setId(firebaseUser.getUid());
            request.setUser_id(firebaseUser.getUid());
            request.setUser_name(fullName);
            request.setMethod("student_profile");
            request.setStatus("pending");
            request.setStudent_id(studentId);
            request.setNote("");
            request.setCreated_at(nowIsoUtc());

            btnSubmit.setEnabled(false);
            btnSubmit.setAlpha(0.55f);
            progress.setVisibility(View.VISIBLE);
            uploadVerificationImagesAndSubmit(request, verificationFrontCardUri, verificationBackCardUri, dialog,
                    btnSubmit, progress);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void bindVerificationCardPreview(boolean front, Uri uri) {
        ImageView preview = front ? verificationFrontPreview : verificationBackPreview;
        TextView hint = front ? verificationFrontHint : verificationBackHint;
        View card = front ? verificationFrontUploadCard : verificationBackUploadCard;
        if (preview == null || hint == null || card == null || uri == null || !isAdded()) {
            return;
        }
        preview.setImageTintList(null);
        preview.setPadding(0, 0, 0, 0);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(uri).centerCrop().into(preview);
        hint.setText("Đã chọn ảnh, chạm để đổi ảnh khác");
        card.setBackgroundResource(R.drawable.bg_verification_upload_card_selected);
    }

    private void uploadVerificationImagesAndSubmit(StudentVerification request,
                                                   Uri frontUri,
                                                   Uri backUri,
                                                   AlertDialog dialog,
                                                   View positiveButton,
                                                   ProgressBar progress) {
        uploadVerificationImage(request.getUser_id(), "front", frontUri, new VerificationImageUploadCallback() {
            @Override
            public void onSuccess(String url) {
                request.setFront_card_url(url);
                uploadVerificationImage(request.getUser_id(), "back", backUri, new VerificationImageUploadCallback() {
                    @Override
                    public void onSuccess(String url) {
                        request.setBack_card_url(url);
                        submitVerificationRequest(request, dialog, positiveButton, progress);
                    }

                    @Override
                    public void onError(String error) {
                        handleVerificationSubmitError(request.getUser_id(), "upload_back", error, positiveButton, progress);
                    }
                });
            }

            @Override
            public void onError(String error) {
                handleVerificationSubmitError(request.getUser_id(), "upload_front", error, positiveButton, progress);
            }
        });
    }

    private void uploadVerificationImage(String userId,
                                         String side,
                                         Uri fileUri,
                                         VerificationImageUploadCallback callback) {
        String fileName = "student_verifications/" + userId + "/" + side + "_" + UUID.randomUUID() + ".jpg";
        StorageReference ref = storage.getReference().child(fileName);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();
        ref.putFile(fileUri, metadata).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                callback.onSuccess(task.getResult().toString());
            } else {
                Exception exception = task.getException();
                Log.e(TAG, "Student card upload failed: " + side, exception);
                callback.onError(exception != null && exception.getMessage() != null
                        ? exception.getMessage() : "Upload ảnh thất bại");
            }
        });
    }

    private void submitVerificationRequest(StudentVerification request,
                                           AlertDialog dialog,
                                           View positiveButton,
                                           ProgressBar progress) {
        verificationService.submitRequest(request, new AsyncCrudService.ItemCallback<StudentVerification>() {
            @Override
            public void onSuccess(StudentVerification data) {
                if (!isAdded()) {
                    return;
                }
                progress.setVisibility(View.GONE);
                dialog.dismiss();
                Toast.makeText(requireContext(), "Yêu cầu xác thực đã được gửi.", Toast.LENGTH_SHORT).show();
                loadUserProfile();
            }

            @Override
            public void onError(String error) {
                handleVerificationSubmitError(request.getUser_id(), "firestore_submit", error, positiveButton, progress);
            }
        });
    }

    private void handleVerificationSubmitError(String userId,
                                               String area,
                                               String error,
                                               View positiveButton,
                                               ProgressBar progress) {
        if (!isAdded()) {
            return;
        }
        AppErrorLogger.append(requireContext(), "profile.verify." + area,
                "uid=" + userId + ", error=" + error);
        progress.setVisibility(View.GONE);
        positiveButton.setEnabled(true);
        positiveButton.setAlpha(1f);
        Toast.makeText(requireContext(), "Gửi yêu cầu thất bại: " + error, Toast.LENGTH_LONG).show();
    }

    private String resolveVerificationUserName(User profile, FirebaseUser firebaseUser) {
        if (profile != null && !TextUtils.isEmpty(profile.getFull_name())) {
            return profile.getFull_name();
        }
        if (firebaseUser != null && !TextUtils.isEmpty(firebaseUser.getDisplayName())) {
            return firebaseUser.getDisplayName();
        }
        if (firebaseUser != null && !TextUtils.isEmpty(firebaseUser.getEmail())) {
            return firebaseUser.getEmail();
        }
        return "Người dùng";
    }

    private interface VerificationImageUploadCallback {
        void onSuccess(String url);

        void onError(String error);
    }

    private void showVerificationReviewDialog() {
        verificationService.getPendingRequests(new AsyncCrudService.ListCallback<StudentVerification>() {
            @Override
            public void onSuccess(List<StudentVerification> data) {
                List<StudentVerification> requests = data != null ? data : new ArrayList<>();
                if (requests.isEmpty()) {
                    Toast.makeText(requireContext(), "Không có yêu cầu đang chờ", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] labels = new String[requests.size()];
                for (int i = 0; i < requests.size(); i++) {
                    StudentVerification request = requests.get(i);
                    String studentId = !TextUtils.isEmpty(request.getStudent_id())
                            ? request.getStudent_id() : "Chưa có MSSV";
                    labels[i] = studentId + " - " + request.getUser_id();
                }

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Duyệt xác thực")
                        .setItems(labels, (dialog, which) -> approveVerification(requests.get(which)))
                        .setNegativeButton("Đóng", null)
                        .show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Không thể tải yêu cầu: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void approveVerification(StudentVerification request) {
        if (request == null || TextUtils.isEmpty(request.getId()) || TextUtils.isEmpty(request.getUser_id())) {
            Toast.makeText(requireContext(), "Yêu cầu không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        userService.setStudentVerified(request.getUser_id(), true, new AsyncCrudService.BooleanCallback() {
            @Override
            public void onSuccess(boolean success) {
                verificationService.approveRequest(request.getId(), nowIsoUtc(), new AsyncCrudService.BooleanCallback() {
                    @Override
                    public void onSuccess(boolean success) {
                        loadUserProfile();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(requireContext(), "Không thể cập nhật yêu cầu: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Không thể xác thực user: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateContentState(ProfileUiState state) {
        if (state == null || layoutProfileLoading == null || layoutProfileEmpty == null) {
            return;
        }
        if (state.isLoading()) {
            layoutProfileLoading.setVisibility(View.VISIBLE);
            layoutProfileEmpty.setVisibility(View.GONE);
            rvContent.setVisibility(View.GONE);
            return;
        }

        int tabPos = tabLayout.getSelectedTabPosition();
        boolean showingOrders = tabPos == 0;
        boolean showingPosts = tabPos == 1;
        boolean empty = showingOrders
                ? state.getOrders().isEmpty()
                : showingPosts ? state.getPosts().isEmpty() : state.getSavedProducts().isEmpty();

        layoutProfileLoading.setVisibility(View.GONE);
        rvContent.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutProfileEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);

        if (empty) {
            if (showingOrders) {
                tvProfileEmptyTitle.setText("Bạn chưa có đơn hàng");
                tvProfileEmptyMessage.setText("Các đơn đã mua sẽ được lưu tại đây để bạn theo dõi trạng thái.");
                ((TextView) btnProfileEmptyAction).setText("Khám phá sản phẩm");
                btnProfileEmptyAction.setOnClickListener(v ->
                        NavHostFragment.findNavController(this).navigate(R.id.searchFragment));
            } else if (showingPosts) {
                tvProfileEmptyTitle.setText("Bạn chưa đăng tin nào");
                tvProfileEmptyMessage.setText("Đăng món đồ không dùng tới để sinh viên khác có thể tìm thấy.");
                ((TextView) btnProfileEmptyAction).setText("Đăng tin mới");
                btnProfileEmptyAction.setOnClickListener(v ->
                        NavHostFragment.findNavController(this).navigate(R.id.postListingFragment));
            } else {
                tvProfileEmptyTitle.setText("Bạn chưa lưu sản phẩm");
                tvProfileEmptyMessage.setText("Nhấn tim ở sản phẩm yêu thích để quay lại nhanh tại đây.");
                ((TextView) btnProfileEmptyAction).setText("Tìm sản phẩm");
                btnProfileEmptyAction.setOnClickListener(v ->
                        NavHostFragment.findNavController(this).navigate(R.id.searchFragment));
            }
        }
    }

    private void showEditProfileDialog() {
        ProfileUiState state = profileViewModel.getUiState().getValue();
        User user = state != null ? state.getProfile() : null;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_profile, null, false);
        TextInputEditText etFullName = dialogView.findViewById(R.id.etEditFullName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etEditPhone);
        TextInputEditText etUniversity = dialogView.findViewById(R.id.etEditUniversity);
        TextInputEditText etLocation = dialogView.findViewById(R.id.etEditLocation);
        TextView btnCancel = dialogView.findViewById(R.id.btnEditProfileCancel);
        TextView btnSave = dialogView.findViewById(R.id.btnEditProfileSave);

        if (user != null) {
            etFullName.setText(user.getFull_name());
            etPhone.setText(user.getPhone());
            etUniversity.setText(user.getUniversity());
            etLocation.setText(user.getLocation());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser == null) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String university = etUniversity.getText() != null ? etUniversity.getText().toString().trim() : "";
            String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";

            if (TextUtils.isEmpty(fullName)) {
                etFullName.setError("Vui lòng nhập họ và tên");
                return;
            }

            profileViewModel.saveProfile(firebaseUser.getUid(), fullName, phone, university, location);
            dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
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
        if (tabLayout.getSelectedTabPosition() == 1) {
            showPostActions(product);
            return;
        }
        showProductDetail(product);
    }

    private void showPostActions(Product product) {
        List<String> actions = new ArrayList<>();
        actions.add("Xem chi tiết");
        String status = product.getStatus() != null ? product.getStatus().toLowerCase(Locale.ROOT) : "active";
        if ("active".equals(status) || "available".equals(status)) {
            actions.add("Đánh dấu đã bán");
            actions.add("Ẩn tin");
        } else {
            actions.add("Đăng lại tin");
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(product.getTitle())
                .setItems(actions.toArray(new String[0]), (dialog, which) -> {
                    String action = actions.get(which);
                    if ("Xem chi tiết".equals(action)) {
                        showProductDetail(product);
                    } else if ("Đánh dấu đã bán".equals(action)) {
                        updateProductStatus(product, "sold");
                    } else if ("Ẩn tin".equals(action)) {
                        updateProductStatus(product, "inactive");
                    } else {
                        updateProductStatus(product, "active");
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void updateProductStatus(Product product, String status) {
        product.setStatus(status);
        product.setUpdated_at(nowIsoUtc());
        productService.save(product, result -> {
            if (!isAdded()) {
                return;
            }
            if (result.isSuccess()) {
                loadUserProfile();
            } else {
                Toast.makeText(requireContext(), "Không thể cập nhật tin: " + result.getError(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProductDetail(Product product) {
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

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chi tiết đơn " + orderId)
                .setMessage(message.toString())
                .setNegativeButton("Đóng", null);

        String status = order.getStatus() != null ? order.getStatus().toLowerCase(Locale.ROOT) : "pending";
        if ("pending".equals(status)) {
            builder.setPositiveButton("Hủy đơn", (dialog, which) -> updateOrderStatus(order, "cancelled"));
        } else if ("shipping".equals(status)) {
            builder.setPositiveButton("Đã nhận hàng", (dialog, which) -> updateOrderStatus(order, "done"));
        }
        builder.show();
    }

    private void updateOrderStatus(Order order, String status) {
        order.setStatus(status);
        order.setUpdated_at(nowIsoUtc());
        orderService.save(order, result -> {
            if (!isAdded()) {
                return;
            }
            if (result.isSuccess()) {
                loadUserProfile();
            } else {
                Toast.makeText(requireContext(), "Không thể cập nhật đơn: " + result.getError(), Toast.LENGTH_SHORT).show();
            }
        });
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

    private String nowIsoUtc() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }
}
