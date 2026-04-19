package com.example.unimarket.pages.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView tvUserName;
    private ImageView ivAvatar;
    private TextView tvAvatar;
    private LinearLayout layoutSearch;
    private ImageView layoutNotification;
    private TextView tvViewAll;

    private RecyclerView rvCategories;
    private RecyclerView rvProducts;

    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;
    private HomeViewModel homeViewModel;

    private final List<Category> categoryList = new ArrayList<>();
    private final List<Product> productList = new ArrayList<>();
    private final Map<String, String> categoryNameById = new HashMap<>();
    private final Map<String, String> productImageById = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        initViews(view);
        setupRecyclerViews();
        setupUserInfo();
        setupClicks();
        setupObservers();
        homeViewModel.loadHomeData();
    }

    private void initViews(View root) {
        tvUserName = root.findViewById(R.id.tvUserName);
        ivAvatar = root.findViewById(R.id.ivAvatar);
        tvAvatar = root.findViewById(R.id.tvAvatar);
        layoutSearch = root.findViewById(R.id.layoutSearch);
        layoutNotification = root.findViewById(R.id.layoutNotification);
        tvViewAll = root.findViewById(R.id.tvViewAll);

        rvCategories = root.findViewById(R.id.rvCategories);
        rvProducts = root.findViewById(R.id.rvProducts);
    }

    private void setupRecyclerViews() {
        categoryAdapter = new CategoryAdapter(new ArrayList<>(), category ->
                Toast.makeText(requireContext(), category.getName(), Toast.LENGTH_SHORT).show()
        );
        rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        rvCategories.setNestedScrollingEnabled(false);
        rvCategories.setAdapter(categoryAdapter);
        rvCategories.addItemDecoration(new GridSpacingItemDecoration(4, dpToPx(10), true));

        productAdapter = new ProductAdapter(
                new ArrayList<>(),
                new HashMap<>(),
                product -> Toast.makeText(requireContext(), safeProductTitle(product), Toast.LENGTH_SHORT).show()
        );
        rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvProducts.setNestedScrollingEnabled(false);
        rvProducts.setAdapter(productAdapter);
        rvProducts.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(12), true));

        RecyclerView.ItemAnimator itemAnimator = rvProducts.getItemAnimator();
        if (itemAnimator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }
    }

    private void setupObservers() {
        homeViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (!isAdded() || state == null) {
                return;
            }

            List<Category> categories = state.getCategories();
            List<Product> products = state.getProducts();
            Map<String, String> images = state.getProductImages();

            categoryList.clear();
            if (categories != null) {
                categoryList.addAll(categories);
            }
            categoryAdapter.submitList(new ArrayList<>(categoryList));
            rebuildCategoryMap();

            productList.clear();
            if (products != null) {
                productList.addAll(products);
            }
            productAdapter.submitList(new ArrayList<>(productList));

            productImageById.clear();
            if (images != null) {
                productImageById.putAll(images);
            }
            productAdapter.setProductImageMap(new HashMap<>(productImageById));
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
        String userName = null;
        String userAvatarUrl = null;

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            userName = firebaseUser.getDisplayName();
            userAvatarUrl = firebaseUser.getPhotoUrl() != null
                    ? firebaseUser.getPhotoUrl().toString() : null;
        }

        if (TextUtils.isEmpty(userName)) {
            Bundle args = getArguments();
            if (args != null) {
                userName = args.getString("user_name");
                if (userAvatarUrl == null) {
                    userAvatarUrl = args.getString("user_avatar");
                }
            }
        }
        if (TextUtils.isEmpty(userName) && requireActivity().getIntent() != null) {
            userName = requireActivity().getIntent().getStringExtra("user_name");
            if (userAvatarUrl == null) {
                userAvatarUrl = requireActivity().getIntent().getStringExtra("user_avatar");
            }
        }

        if (TextUtils.isEmpty(userName)) {
            userName = "Người dùng";
        }

        tvUserName.setText(userName);

        if (!TextUtils.isEmpty(userAvatarUrl)) {
            tvAvatar.setVisibility(View.GONE);
            Glide.with(this)
                    .load(userAvatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivAvatar);
        } else {
            tvAvatar.setText(HomeUiUtils.extractInitial(userName));
            tvAvatar.setVisibility(View.VISIBLE);
        }
    }

    private void rebuildCategoryMap() {
        categoryNameById.clear();
        for (Category category : categoryList) {
            if (category != null && category.getId() != null && !TextUtils.isEmpty(category.getName())) {
                categoryNameById.put(category.getId(), category.getName());
            }
        }
        productAdapter.setCategoryNameMap(new HashMap<>(categoryNameById));
    }

    private String safeProductTitle(Product product) {
        if (product == null || TextUtils.isEmpty(product.getTitle())) {
            return "San pham";
        }
        return product.getTitle();
    }

    private void setupClicks() {
        layoutSearch.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mở màn tìm kiếm", Toast.LENGTH_SHORT).show()
        );

        layoutNotification.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mở thông báo", Toast.LENGTH_SHORT).show()
        );

        tvViewAll.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Xem tất cả danh mục", Toast.LENGTH_SHORT).show()
        );
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
