package com.example.unimarket.pages.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.SavedSearch;
import com.example.unimarket.data.service.SavedSearchService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SavedSearchesFragment extends Fragment {
    private final SavedSearchService savedSearchService = new SavedSearchService();

    private RecyclerView rvSavedSearches;
    private View layoutLoading;
    private View layoutEmpty;
    private TextView tvEmptyMessage;
    private SavedSearchAdapter adapter;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_searches, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbarSavedSearches);
        toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        rvSavedSearches = view.findViewById(R.id.rvSavedSearches);
        layoutLoading = view.findViewById(R.id.layoutSavedSearchLoading);
        layoutEmpty = view.findViewById(R.id.layoutSavedSearchEmpty);
        tvEmptyMessage = view.findViewById(R.id.tvSavedSearchEmptyMessage);

        adapter = new SavedSearchAdapter(new SavedSearchAdapter.Listener() {
            @Override
            public void onOpen(SavedSearch savedSearch) {
                Bundle args = new Bundle();
                args.putString("saved_search_id", savedSearch.getId());
                NavHostFragment.findNavController(SavedSearchesFragment.this)
                        .navigate(R.id.searchFragment, args);
            }

            @Override
            public void onToggleAlerts(SavedSearch savedSearch, boolean enabled) {
                savedSearch.setAlerts_enabled(enabled);
                savedSearchService.save(savedSearch, result -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (!result.isSuccess()) {
                        Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
                        loadSavedSearches();
                    }
                });
            }

            @Override
            public void onDelete(SavedSearch savedSearch) {
                confirmDelete(savedSearch);
            }
        });

        rvSavedSearches.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSavedSearches.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = user != null ? user.getUid() : null;
        loadSavedSearches();
    }

    private void loadSavedSearches() {
        if (TextUtils.isEmpty(currentUserId)) {
            bindEmpty("Bạn cần đăng nhập để quản lý tìm kiếm đã lưu.");
            return;
        }

        layoutLoading.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvSavedSearches.setVisibility(View.GONE);

        savedSearchService.getSavedSearchesByUserId(currentUserId, new AsyncCrudService.ListCallback<SavedSearch>() {
            @Override
            public void onSuccess(List<SavedSearch> data) {
                if (!isAdded()) {
                    return;
                }
                List<SavedSearch> items = data != null ? new ArrayList<>(data) : new ArrayList<>();
                Collections.sort(items, (left, right) -> safe(right.getCreated_at()).compareTo(safe(left.getCreated_at())));
                layoutLoading.setVisibility(View.GONE);
                if (items.isEmpty()) {
                    bindEmpty("Lưu một bộ lọc trong màn Tìm kiếm để xem lại nhanh và nhận alert khi có tin mới.");
                    return;
                }
                layoutEmpty.setVisibility(View.GONE);
                rvSavedSearches.setVisibility(View.VISIBLE);
                adapter.submitList(items);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                layoutLoading.setVisibility(View.GONE);
                bindEmpty("Không tải được danh sách lúc này.");
            }
        });
    }

    private void bindEmpty(String message) {
        rvSavedSearches.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        if (tvEmptyMessage != null) {
            tvEmptyMessage.setText(message);
        }
    }

    private void confirmDelete(SavedSearch savedSearch) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa tìm kiếm đã lưu")
                .setMessage("Bạn sẽ không còn nhận alert cho bộ lọc này.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) ->
                        savedSearchService.deleteById(savedSearch.getId(), new AsyncCrudService.BooleanCallback() {
                            @Override
                            public void onSuccess(boolean success) {
                                if (isAdded()) {
                                    loadSavedSearches();
                                }
                            }

                            @Override
                            public void onError(String error) {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }))
                .show();
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
