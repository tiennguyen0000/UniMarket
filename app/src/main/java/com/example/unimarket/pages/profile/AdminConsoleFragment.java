package com.example.unimarket.pages.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.example.unimarket.pages.home.HomeUiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AdminConsoleFragment extends Fragment {

    private TextView tvAdminUsersTotal;
    private TextView tvAdminVerifiedTotal;
    private TextView tvAdminSuspendedTotal;
    private TextView tvAdminProductsTotal;
    private TextView tvAdminRemovedTotal;
    private TextView tvAdminOrdersTotal;
    private TextView tvAdminRevenueTotal;
    private TextView tvAdminConsoleRole;
    private TextView tvVerificationQueueSummary;
    private EditText etAdminUserSearch;
    private RecyclerView rvAdminUsers;
    private RecyclerView rvVerificationQueue;
    private View layoutAdminLoading;
    private View cardAdminStats;
    private View cardAdminUsers;
    private ImageView btnBack;

    private final UserService userService = new UserService();
    private final OrderService orderService = new OrderService();
    private final ProductService productService = new ProductService();
    private final StudentVerificationService verificationService = new StudentVerificationService();
    private final List<User> allUsers = new ArrayList<>();
    private AdminUserAdapter adminUserAdapter;
    private AdminVerificationAdapter verificationAdapter;
    private String currentUserId;
    private boolean currentIsAdmin;
    private int pendingLoads;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_console, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = firebaseUser != null ? firebaseUser.getUid() : null;

        btnBack = view.findViewById(R.id.btnAdminBack);
        tvAdminUsersTotal = view.findViewById(R.id.tvAdminUsersTotal);
        tvAdminVerifiedTotal = view.findViewById(R.id.tvAdminVerifiedTotal);
        tvAdminSuspendedTotal = view.findViewById(R.id.tvAdminSuspendedTotal);
        tvAdminProductsTotal = view.findViewById(R.id.tvAdminProductsTotal);
        tvAdminRemovedTotal = view.findViewById(R.id.tvAdminRemovedTotal);
        tvAdminOrdersTotal = view.findViewById(R.id.tvAdminOrdersTotal);
        tvAdminRevenueTotal = view.findViewById(R.id.tvAdminRevenueTotal);
        tvAdminConsoleRole = view.findViewById(R.id.tvAdminConsoleRole);
        tvVerificationQueueSummary = view.findViewById(R.id.tvVerificationQueueSummary);
        etAdminUserSearch = view.findViewById(R.id.etAdminUserSearch);
        rvAdminUsers = view.findViewById(R.id.rvAdminUsers);
        rvVerificationQueue = view.findViewById(R.id.rvVerificationQueue);
        layoutAdminLoading = view.findViewById(R.id.layoutAdminLoading);
        cardAdminStats = view.findViewById(R.id.cardAdminStats);
        cardAdminUsers = view.findViewById(R.id.cardAdminUsers);

        adminUserAdapter = new AdminUserAdapter(currentUserId, this::confirmToggleUserStatus);
        rvAdminUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAdminUsers.setAdapter(adminUserAdapter);
        rvAdminUsers.setNestedScrollingEnabled(false);
        verificationAdapter = new AdminVerificationAdapter(this::confirmApproveVerification);
        rvVerificationQueue.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvVerificationQueue.setAdapter(verificationAdapter);
        rvVerificationQueue.setNestedScrollingEnabled(false);

        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        etAdminUserSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAdminUsers();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        guardAdminAndLoad();
    }

    private void guardAdminAndLoad() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            requireActivity().onBackPressed();
            return;
        }

        userService.getProfileById(firebaseUser.getUid(), new AsyncCrudService.ItemCallback<User>() {
            @Override
            public void onSuccess(User data) {
                if (!AccessControl.isModerator(data)) {
                    requireActivity().onBackPressed();
                    return;
                }
                currentIsAdmin = AccessControl.isAdmin(data);
                bindRoleSurface();
                loadAdminConsole();
            }

            @Override
            public void onError(String error) {
                requireActivity().onBackPressed();
            }
        });
    }

    private void loadAdminConsole() {
        layoutAdminLoading.setVisibility(View.VISIBLE);
        pendingLoads = currentIsAdmin ? 4 : 1;

        verificationService.getPendingRequests(new AsyncCrudService.ListCallback<StudentVerification>() {
            @Override
            public void onSuccess(List<StudentVerification> data) {
                if (!isAdded()) {
                    return;
                }
                bindVerificationRequests(data);
                finishLoadIfReady();
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    showAdminError(error);
                    finishLoadIfReady();
                }
            }
        });

        if (!currentIsAdmin) {
            return;
        }

        userService.fetchAll(result -> {
            if (!isAdded()) {
                return;
            }
            if (result.isSuccess()) {
                bindAdminUsers(result.getData());
            } else {
                showAdminError(result.getError());
            }
            finishLoadIfReady();
        });

        productService.fetchAll(result -> {
            if (!isAdded()) {
                return;
            }
            if (result.isSuccess()) {
                bindAdminProducts(result.getData());
            } else {
                showAdminError(result.getError());
            }
            finishLoadIfReady();
        });

        orderService.fetchAll(result -> {
            if (!isAdded()) {
                return;
            }
            if (result.isSuccess()) {
                bindAdminOrders(result.getData());
            } else {
                showAdminError(result.getError());
            }
            finishLoadIfReady();
        });
    }

    private void bindRoleSurface() {
        tvAdminConsoleRole.setText(currentIsAdmin ? "Admin" : "Moderator");
        cardAdminStats.setVisibility(currentIsAdmin ? View.VISIBLE : View.GONE);
        cardAdminUsers.setVisibility(currentIsAdmin ? View.VISIBLE : View.GONE);
    }

    private void finishLoadIfReady() {
        pendingLoads--;
        if (pendingLoads <= 0 && layoutAdminLoading != null) {
            layoutAdminLoading.setVisibility(View.GONE);
        }
    }

    private void bindAdminUsers(List<User> users) {
        List<User> data = users != null ? users : new ArrayList<>();
        allUsers.clear();
        allUsers.addAll(data);
        int verified = 0;
        int suspended = 0;
        for (User user : data) {
            if (user == null) {
                continue;
            }
            if (user.isVerified()) {
                verified++;
            }
            if (!AccessControl.isActive(user)) {
                suspended++;
            }
        }
        tvAdminUsersTotal.setText(String.valueOf(data.size()));
        tvAdminVerifiedTotal.setText(String.valueOf(verified));
        tvAdminSuspendedTotal.setText(String.valueOf(suspended));
        filterAdminUsers();
    }

    private void bindVerificationRequests(List<StudentVerification> requests) {
        List<StudentVerification> data = requests != null ? requests : new ArrayList<>();
        verificationAdapter.submitList(data);
        tvVerificationQueueSummary.setText(data.isEmpty()
                ? "Không có yêu cầu đang chờ duyệt."
                : data.size() + " yêu cầu đang chờ duyệt.");
    }

    private void filterAdminUsers() {
        String query = etAdminUserSearch != null && etAdminUserSearch.getText() != null
                ? etAdminUserSearch.getText().toString().trim().toLowerCase(Locale.ROOT)
                : "";
        if (TextUtils.isEmpty(query)) {
            adminUserAdapter.submitList(new ArrayList<>(allUsers));
            return;
        }

        List<User> filtered = new ArrayList<>();
        for (User user : allUsers) {
            if (matchesUserQuery(user, query)) {
                filtered.add(user);
            }
        }
        adminUserAdapter.submitList(filtered);
    }

    private boolean matchesUserQuery(User user, String query) {
        if (user == null) {
            return false;
        }
        return safeLower(user.getFull_name()).contains(query)
                || safeLower(user.getUniversity()).contains(query)
                || safeLower(user.getPhone()).contains(query)
                || safeLower(user.getRole()).contains(query)
                || safeLower(user.getId()).contains(query);
    }

    private String safeLower(String value) {
        return value != null ? value.toLowerCase(Locale.ROOT) : "";
    }

    private void bindAdminProducts(List<Product> products) {
        List<Product> data = products != null ? products : new ArrayList<>();
        int removed = 0;
        for (Product product : data) {
            String status = product != null && product.getStatus() != null
                    ? product.getStatus().toLowerCase(Locale.ROOT) : "";
            if ("removed".equals(status)) {
                removed++;
            }
        }
        tvAdminProductsTotal.setText(String.valueOf(data.size()));
        tvAdminRemovedTotal.setText(String.valueOf(removed));
    }

    private void bindAdminOrders(List<Order> orders) {
        List<Order> data = orders != null ? orders : new ArrayList<>();
        double doneTotal = 0;
        for (Order order : data) {
            if (order == null) {
                continue;
            }
            String status = order.getStatus() != null ? order.getStatus().toLowerCase(Locale.ROOT) : "";
            if ("done".equals(status) && order.getTotal_price() != null) {
                doneTotal += order.getTotal_price();
            }
        }
        tvAdminOrdersTotal.setText(String.valueOf(data.size()));
        tvAdminRevenueTotal.setText(HomeUiUtils.formatPrice(doneTotal));
    }

    private void confirmToggleUserStatus(User user) {
        if (user == null || TextUtils.isEmpty(user.getId()) || user.getId().equals(currentUserId)) {
            return;
        }
        boolean suspended = AccessControl.STATUS_SUSPENDED.equalsIgnoreCase(user.getAccount_status());
        String nextStatus = suspended ? AccessControl.STATUS_ACTIVE : AccessControl.STATUS_SUSPENDED;
        String title = suspended ? "Mở khóa tài khoản" : "Khóa tài khoản";
        String message = suspended
                ? "Người dùng sẽ có thể đăng tin, đặt hàng và nhắn tin trở lại."
                : "Người dùng sẽ không thể đăng tin, đặt hàng hoặc nhắn tin cho đến khi được mở khóa.";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Hủy", null)
                .setPositiveButton(suspended ? "Mở khóa" : "Khóa", (dialog, which) ->
                        updateUserStatus(user.getId(), nextStatus))
                .show();
    }

    private void confirmApproveVerification(StudentVerification request) {
        if (request == null || TextUtils.isEmpty(request.getId()) || TextUtils.isEmpty(request.getUser_id())) {
            showAdminError("Yêu cầu xác thực không hợp lệ.");
            return;
        }

        String studentId = !TextUtils.isEmpty(request.getStudent_id())
                ? request.getStudent_id() : "Chưa có MSSV";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Duyệt xác thực")
                .setMessage("Xác thực sinh viên " + studentId + " cho user này?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Duyệt", (dialog, which) -> approveVerification(request))
                .show();
    }

    private void approveVerification(StudentVerification request) {
        userService.setStudentVerified(request.getUser_id(), true, new AsyncCrudService.BooleanCallback() {
            @Override
            public void onSuccess(boolean success) {
                verificationService.approveRequest(request.getId(), nowIsoUtc(), new AsyncCrudService.BooleanCallback() {
                    @Override
                    public void onSuccess(boolean success) {
                        loadAdminConsole();
                    }

                    @Override
                    public void onError(String error) {
                        showAdminError(error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                showAdminError(error);
            }
        });
    }

    private void updateUserStatus(String userId, String status) {
        userService.setAccountStatus(userId, status, nowIsoUtc(), new AsyncCrudService.BooleanCallback() {
            @Override
            public void onSuccess(boolean success) {
                loadAdminConsole();
            }

            @Override
            public void onError(String error) {
                showAdminError(error);
            }
        });
    }

    private void showAdminError(String error) {
        if (!isAdded()) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Không thể cập nhật quản trị")
                .setMessage(error)
                .setPositiveButton("Đóng", null)
                .show();
    }

    private String nowIsoUtc() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }
}
