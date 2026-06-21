package com.example.unimarket.pages.search;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.auth.AccessControl;
import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.Review;
import com.example.unimarket.data.model.SavedSearch;
import com.example.unimarket.data.model.Wishlist;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.ReviewService;
import com.example.unimarket.data.service.SavedSearchService;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.WishlistService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.data.util.FirestoreIds;
import com.example.unimarket.pages.home.CartBottomSheetFragment;
import com.example.unimarket.pages.home.CartFlow;
import com.example.unimarket.pages.home.HomeViewModel;
import com.example.unimarket.pages.home.ProductAdapter;
import com.example.unimarket.pages.home.ProductDetailBottomSheetFragment;
import com.example.unimarket.pages.post.PostListingFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SearchFragment extends Fragment {

    private EditText etSearchQuery;
    private ImageView btnFilter;
    private ImageView btnSaveSearch;
    private TextView tvSearchResults;
    private TextView tvResultCount;
    private TextView sortRelevance;
    private TextView sortNewest;
    private TextView sortPriceLow;
    private TextView sortPriceHigh;
    private TextView sortRating;
    private TextView sortSaved;
    private RecyclerView rvSearchProducts;
    private View layoutSearchLoading;
    private View layoutSearchEmpty;
    private TextView tvSearchEmptyTitle;
    private TextView tvSearchEmptyMessage;

    private ProductAdapter productAdapter;
    private HomeViewModel homeViewModel;
    private final ReviewService reviewService = new ReviewService();
    private final ProductService productService = new ProductService();
    private final UserService userService = new UserService();
    private final WishlistService wishlistService = new WishlistService();
    private final SavedSearchService savedSearchService = new SavedSearchService();

    private final List<Product> productList = new ArrayList<>();
    private final List<Product> filteredProductList = new ArrayList<>();
    private final Map<String, String> categoryNameById = new HashMap<>();
    private final Map<String, Double> productRatingById = new HashMap<>();
    private final Set<String> savedProductIds = new HashSet<>();
    private String currentSort = "relevance";

    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;
    private boolean filterNew = false;
    private boolean filterUsed = false;
    private boolean filterSavedOnly = false;
    private String initialCategoryName;
    private String initialCategoryId;
    private String initialProductId;
    private String initialSavedSearchId;
    private boolean openedInitialProduct;
    private boolean catalogLoading = true;
    private boolean adminMode;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        initViews(view);
        setupRecyclerView();
        setupClicks();
        applyInitialCategoryArgument();
        setupRefreshListener();
        setupObservers();
        highlightSelectedSort(sortRelevance);
        loadAdminState();
        loadWishlistState();
        loadReviewRatings();
        homeViewModel.loadCatalogData();
    }

    private void initViews(View root) {
        etSearchQuery = root.findViewById(R.id.etSearchQuery);
        btnFilter = root.findViewById(R.id.btnFilter);
        btnSaveSearch = root.findViewById(R.id.btnSaveSearch);
        tvSearchResults = root.findViewById(R.id.tvSearchResults);
        tvResultCount = root.findViewById(R.id.tvResultCount);
        rvSearchProducts = root.findViewById(R.id.rvSearchProducts);
        layoutSearchLoading = root.findViewById(R.id.layoutSearchLoading);
        layoutSearchEmpty = root.findViewById(R.id.layoutSearchEmpty);
        tvSearchEmptyTitle = root.findViewById(R.id.tvSearchEmptyTitle);
        tvSearchEmptyMessage = root.findViewById(R.id.tvSearchEmptyMessage);

        sortRelevance = root.findViewById(R.id.sortRelevance);
        sortNewest = root.findViewById(R.id.sortNewest);
        sortPriceLow = root.findViewById(R.id.sortPriceLow);
        sortPriceHigh = root.findViewById(R.id.sortPriceHigh);
        sortRating = root.findViewById(R.id.sortRating);
        sortSaved = root.findViewById(R.id.sortSaved);
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(
                new ArrayList<>(),
                new HashMap<>(),
                product -> openProductDetail(product,
                        firstProductImage(product),
                        categoryNameById.get(product.getCategory_id())),
                (product, imageUrl, categoryName) -> openProductDetail(product, imageUrl, categoryName),
                this::addProductToCart
        );
        productAdapter.setFavoriteIds(savedProductIds);
        productAdapter.setOnFavoriteChangedListener(this::handleFavoriteChanged);
        rvSearchProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvSearchProducts.setNestedScrollingEnabled(false);
        rvSearchProducts.setItemAnimator(null);
        rvSearchProducts.setAdapter(productAdapter);
        rvSearchProducts.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(4), true));
    }

    private void loadAdminState() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            adminMode = false;
            currentUserId = null;
            productAdapter.setAdminModeration(false, null, null);
            return;
        }

        currentUserId = firebaseUser.getUid();
        userService.getProfileById(currentUserId, new AsyncCrudService.ItemCallback<com.example.unimarket.data.model.User>() {
            @Override
            public void onSuccess(com.example.unimarket.data.model.User data) {
                adminMode = AccessControl.isModerator(data);
                productAdapter.setAdminModeration(adminMode, currentUserId, SearchFragment.this::showAdminRemoveDialog);
            }

            @Override
            public void onError(String error) {
                adminMode = false;
                productAdapter.setAdminModeration(false, currentUserId, null);
            }
        });
    }

    private void addProductToCart(Product product) {
        new CartFlow().add(product, 1, new CartFlow.Callback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }
                CartBottomSheetFragment.newInstance()
                        .show(requireActivity().getSupportFragmentManager(), "cart");
            }

            @Override
            public void onError(String message) {
                openProductDetail(product, firstProductImage(product), categoryNameById.get(product.getCategory_id()));
            }
        });
    }

    private void setupClicks() {
        btnFilter.setOnClickListener(v -> showFilterBottomSheet());
        btnSaveSearch.setOnClickListener(v -> showSaveSearchDialog());

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchFiltersAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            applySearchFiltersAndSort();
            return false;
        });

        sortRelevance.setOnClickListener(v -> setSortOption("relevance", sortRelevance));
        sortNewest.setOnClickListener(v -> setSortOption("newest", sortNewest));
        sortPriceLow.setOnClickListener(v -> setSortOption("price_low", sortPriceLow));
        sortPriceHigh.setOnClickListener(v -> setSortOption("price_high", sortPriceHigh));
        sortRating.setOnClickListener(v -> setSortOption("rating", sortRating));
        sortSaved.setOnClickListener(v -> toggleSavedOnly());
    }

    private void applyInitialCategoryArgument() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        initialCategoryName = args.getString("category_name");
        initialCategoryId = args.getString("category_id");
        initialProductId = args.getString("product_id");
        initialSavedSearchId = args.getString("saved_search_id");
        if (!TextUtils.isEmpty(initialCategoryName)) {
            etSearchQuery.setText(initialCategoryName);
            etSearchQuery.setSelection(etSearchQuery.getText().length());
        }
        if (!TextUtils.isEmpty(initialSavedSearchId)) {
            loadSavedSearch(initialSavedSearchId);
        }
    }

    private void setupRefreshListener() {
        getParentFragmentManager().setFragmentResultListener(
                PostListingFragment.RESULT_LISTING_CREATED,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    loadReviewRatings();
                    homeViewModel.loadCatalogData();
                }
        );
    }

    private void setupObservers() {
        homeViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (!isAdded() || state == null) {
                return;
            }

            List<Category> categories = state.getCategories();
            List<Product> products = state.getProducts();
            catalogLoading = state.isLoading();

            rebuildCategoryMap(categories);

            productList.clear();
            if (products != null) {
                productList.addAll(products);
            }

            productAdapter.setCategoryNameMap(new HashMap<>(categoryNameById));

            Map<String, String> avatars = state.getSellerAvatars();
            if (avatars != null) {
                productAdapter.setSellerAvatarMap(new HashMap<>(avatars));
            }

            applySearchFiltersAndSort();
        });
    }

    private void rebuildCategoryMap(List<Category> categories) {
        categoryNameById.clear();
        if (categories == null) {
            return;
        }
        for (Category category : categories) {
            if (category != null
                    && category.getId() != null
                    && !TextUtils.isEmpty(category.getName())) {
                categoryNameById.put(category.getId(), category.getName());
            }
        }
    }

    private void loadReviewRatings() {
        reviewService.fetchAll(result -> {
            productRatingById.clear();
            if (result.isSuccess() && result.getData() != null) {
                Map<String, Integer> ratingCountByProduct = new HashMap<>();
                Map<String, Integer> ratingSumByProduct = new HashMap<>();
                for (Review review : result.getData()) {
                    if (review == null
                            || TextUtils.isEmpty(review.getProduct_id())
                            || review.getRating() == null) {
                        continue;
                    }
                    String productId = review.getProduct_id();
                    int currentCount = ratingCountByProduct.containsKey(productId)
                            ? ratingCountByProduct.get(productId) : 0;
                    int currentSum = ratingSumByProduct.containsKey(productId)
                            ? ratingSumByProduct.get(productId) : 0;
                    ratingCountByProduct.put(productId, currentCount + 1);
                    ratingSumByProduct.put(productId, currentSum + review.getRating());
                }

                for (Map.Entry<String, Integer> entry : ratingCountByProduct.entrySet()) {
                    String productId = entry.getKey();
                    int count = entry.getValue();
                    int sum = ratingSumByProduct.containsKey(productId)
                            ? ratingSumByProduct.get(productId) : 0;
                    if (count > 0) {
                        productRatingById.put(productId, sum / (double) count);
                    }
                }
            }
            applySort();
        });
    }

    private void loadWishlistState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            savedProductIds.clear();
            productAdapter.setFavoriteIds(savedProductIds);
            applySearchFiltersAndSort();
            return;
        }

        wishlistService.getWithFilter("user_id", user.getUid(), new AsyncCrudService.ListCallback<Wishlist>() {
            @Override
            public void onSuccess(List<Wishlist> data) {
                savedProductIds.clear();
                if (data != null) {
                    for (Wishlist item : data) {
                        if (item != null && !TextUtils.isEmpty(item.getProduct_id())) {
                            savedProductIds.add(item.getProduct_id());
                        }
                    }
                }
                productAdapter.setFavoriteIds(savedProductIds);
                applySearchFiltersAndSort();
            }

            @Override
            public void onError(String error) {
                savedProductIds.clear();
                productAdapter.setFavoriteIds(savedProductIds);
                applySearchFiltersAndSort();
            }
        });
    }

    private void applySearchFiltersAndSort() {
        String query = normalizedQuery();
        filteredProductList.clear();

        for (Product product : productList) {
            if (product == null) {
                continue;
            }
            if (!matchesQuery(product, query)) {
                continue;
            }
            if (!isVisibleProduct(product)) {
                continue;
            }
            if (filterSavedOnly && !savedProductIds.contains(product.getId())) {
                continue;
            }

            double price = product.getPrice() != null ? product.getPrice() : 0;
            if (price < minPrice || price > maxPrice) {
                continue;
            }

            String condition = product.getCondition();
            if (filterNew || filterUsed) {
                boolean isNew = "new".equalsIgnoreCase(condition) || "NEW".equals(condition);
                boolean isUsed = "used".equalsIgnoreCase(condition)
                        || "good".equalsIgnoreCase(condition)
                        || "USED".equals(condition);
                if (filterNew && !isNew) {
                    continue;
                }
                if (filterUsed && !isUsed) {
                    continue;
                }
            }

            filteredProductList.add(product);
        }

        applySort();
    }

    private void toggleSavedOnly() {
        filterSavedOnly = !filterSavedOnly;
        updateSavedChipState();
        applySearchFiltersAndSort();
    }

    private void loadSavedSearch(String savedSearchId) {
        savedSearchService.getById(savedSearchId, new AsyncCrudService.ItemCallback<SavedSearch>() {
            @Override
            public void onSuccess(SavedSearch data) {
                if (!isAdded() || data == null) {
                    return;
                }
                applySavedSearch(data);
            }

            @Override
            public void onError(String error) {
                if (isAdded() && !TextUtils.isEmpty(error)) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applySavedSearch(SavedSearch savedSearch) {
        etSearchQuery.setText(savedSearch.getQuery() != null ? savedSearch.getQuery() : "");
        etSearchQuery.setSelection(etSearchQuery.getText().length());
        minPrice = savedSearch.getMin_price() != null ? savedSearch.getMin_price() : 0d;
        maxPrice = savedSearch.getMax_price() != null ? savedSearch.getMax_price() : Double.MAX_VALUE;
        filterNew = savedSearch.isFilter_new();
        filterUsed = savedSearch.isFilter_used();
        filterSavedOnly = savedSearch.isFilter_saved_only();
        currentSort = !TextUtils.isEmpty(savedSearch.getSort()) ? savedSearch.getSort() : "relevance";
        highlightSelectedSortViewForCurrentSort();
        updateSavedChipState();
        applySearchFiltersAndSort();
    }

    private void handleFavoriteChanged(String productId, boolean saved) {
        if (TextUtils.isEmpty(productId)) {
            return;
        }
        if (saved) {
            savedProductIds.add(productId);
        } else {
            savedProductIds.remove(productId);
        }
        if (filterSavedOnly) {
            applySearchFiltersAndSort();
        }
    }

    private boolean isVisibleProduct(Product product) {
        String status = product.getStatus() != null
                ? product.getStatus().trim().toLowerCase(Locale.ROOT)
                : "active";
        return !status.equals("removed") && !status.equals("inactive");
    }

    private void showAdminRemoveDialog(Product product) {
        if (product == null || TextUtils.isEmpty(product.getId())) {
            return;
        }
        String[] reasons = {
                "Sai sự thật hoặc gây hiểu nhầm",
                "Sản phẩm không liên quan đến IT",
                "Sản phẩm bị cấm hoặc có rủi ro",
                "Spam hoặc đăng trùng lặp",
                "Vi phạm quy định cộng đồng"
        };
        final int[] selected = {0};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Gỡ bài đăng")
                .setSingleChoiceItems(reasons, selected[0], (dialog, which) -> selected[0] = which)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gỡ bài", (dialog, which) -> removeProductAsAdmin(product, reasons[selected[0]]))
                .show();
    }

    private void removeProductAsAdmin(Product product, String reason) {
        product.setStatus("removed");
        product.setRemoval_reason(reason);
        product.setRemoved_by(currentUserId);
        product.setRemoved_at(nowIsoUtc());
        product.setUpdated_at(product.getRemoved_at());
        productService.save(product, result -> {
            if (!isAdded()) {
                return;
            }
            if (result.isSuccess()) {
                homeViewModel.loadCatalogData();
            } else {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Không thể gỡ bài")
                        .setMessage(result.getError())
                        .setPositiveButton("Đóng", null)
                        .show();
            }
        });
    }

    private boolean matchesQuery(Product product, String query) {
        if (TextUtils.isEmpty(query)) {
            return true;
        }

        String title = safeLower(product.getTitle());
        String description = safeLower(product.getDescription());
        String categoryName = safeLower(categoryNameById.get(product.getCategory_id()));

        return title.contains(query) || description.contains(query) || categoryName.contains(query);
    }

    private void applySort() {
        List<Product> sortedList = new ArrayList<>(filteredProductList);

        switch (currentSort) {
            case "price_low":
                sortedList.sort((a, b) -> Double.compare(safePrice(a), safePrice(b)));
                break;
            case "price_high":
                sortedList.sort((a, b) -> Double.compare(safePrice(b), safePrice(a)));
                break;
            case "rating":
                sortedList.sort((a, b) -> {
                    int ratingCompare = Double.compare(safeRating(b), safeRating(a));
                    if (ratingCompare != 0) {
                        return ratingCompare;
                    }
                    return buildSortKey(b).compareTo(buildSortKey(a));
                });
                break;
            case "newest":
                sortedList.sort((a, b) -> buildSortKey(b).compareTo(buildSortKey(a)));
                break;
            case "relevance":
            default:
                break;
        }

        productAdapter.submitList(new ArrayList<>(sortedList));
        updateSearchSummary(sortedList.size());
        updateSearchLoading();
        updateEmptyState(sortedList.size());
        openInitialProductIfReady();
    }

    private void openInitialProductIfReady() {
        if (openedInitialProduct || catalogLoading || TextUtils.isEmpty(initialProductId)) {
            return;
        }
        for (Product product : productList) {
            if (product != null && initialProductId.equals(product.getId()) && isVisibleProduct(product)) {
                openedInitialProduct = true;
                rvSearchProducts.post(() -> openProductDetail(
                        product,
                        firstProductImage(product),
                        categoryNameById.get(product.getCategory_id())));
                return;
            }
        }
    }

    private void updateSearchLoading() {
        if (layoutSearchLoading == null || rvSearchProducts == null) {
            return;
        }
        boolean showLoading = catalogLoading && productList.isEmpty();
        layoutSearchLoading.setVisibility(showLoading ? View.VISIBLE : View.GONE);
        if (showLoading) {
            rvSearchProducts.setVisibility(View.GONE);
            if (layoutSearchEmpty != null) layoutSearchEmpty.setVisibility(View.GONE);
        }
    }

    private void updateEmptyState(int resultCount) {
        if (layoutSearchEmpty == null || rvSearchProducts == null || layoutSearchLoading == null) {
            return;
        }
        boolean showLoading = catalogLoading && productList.isEmpty();
        boolean showEmpty = !showLoading && resultCount == 0;

        rvSearchProducts.setVisibility(showEmpty || showLoading ? View.GONE : View.VISIBLE);
        layoutSearchEmpty.setVisibility(showEmpty ? View.VISIBLE : View.GONE);

        if (showEmpty) {
            String query = etSearchQuery.getText() != null
                    ? etSearchQuery.getText().toString().trim() : "";
            if (filterSavedOnly && FirebaseAuth.getInstance().getCurrentUser() == null) {
                tvSearchEmptyTitle.setText("Đăng nhập để xem tin đã lưu");
                tvSearchEmptyMessage.setText("Bạn cần đăng nhập trước khi xem danh sách sản phẩm đã lưu.");
            } else if (filterSavedOnly && savedProductIds.isEmpty()) {
                tvSearchEmptyTitle.setText("Bạn chưa lưu tin nào");
                tvSearchEmptyMessage.setText("Nhấn Lưu tin trong menu ba chấm để lưu lại sản phẩm muốn theo dõi.");
            } else if (TextUtils.isEmpty(query) && productList.isEmpty()) {
                tvSearchEmptyTitle.setText("Chưa có sản phẩm nào");
                tvSearchEmptyMessage.setText("Khi người bán đăng tin mới, sản phẩm sẽ xuất hiện tại đây.");
            } else {
                tvSearchEmptyTitle.setText("Chưa tìm thấy sản phẩm");
                tvSearchEmptyMessage.setText("Thử đổi từ khóa, bỏ bớt bộ lọc hoặc chọn cách sắp xếp khác.");
            }
        }
    }

    private void showFilterBottomSheet() {
        SearchFilterBottomSheetFragment bottomSheet = new SearchFilterBottomSheetFragment();
        bottomSheet.setInitialState(minPrice, maxPrice, filterNew, filterUsed);
        bottomSheet.setFilterListener(new SearchFilterBottomSheetFragment.FilterListener() {
            @Override
            public void onApplyFilter(double minPrice, double maxPrice, boolean filterNew,
                                      boolean filterUsed) {
                SearchFragment.this.minPrice = minPrice;
                SearchFragment.this.maxPrice = maxPrice;
                SearchFragment.this.filterNew = filterNew;
                SearchFragment.this.filterUsed = filterUsed;
                applySearchFiltersAndSort();
            }

            @Override
            public void onResetFilter() {
                SearchFragment.this.minPrice = 0;
                SearchFragment.this.maxPrice = Double.MAX_VALUE;
                SearchFragment.this.filterNew = false;
                SearchFragment.this.filterUsed = false;
                applySearchFiltersAndSort();
            }
        });
        bottomSheet.show(getChildFragmentManager(), "search_filter");
    }

    private void setSortOption(String sortOption, TextView selectedView) {
        currentSort = sortOption;
        highlightSelectedSort(selectedView);
        applySort();
    }

    private void highlightSelectedSortViewForCurrentSort() {
        switch (currentSort) {
            case "newest":
                highlightSelectedSort(sortNewest);
                break;
            case "price_low":
                highlightSelectedSort(sortPriceLow);
                break;
            case "price_high":
                highlightSelectedSort(sortPriceHigh);
                break;
            case "rating":
                highlightSelectedSort(sortRating);
                break;
            case "relevance":
            default:
                highlightSelectedSort(sortRelevance);
                break;
        }
    }

    private void highlightSelectedSort(TextView selectedView) {
        resetSortButtons();
        selectedView.setBackgroundResource(R.drawable.bg_sort_chip_selected);
        selectedView.setTextColor(0xFFFFFFFF);
        updateSavedChipState();
    }

    private void resetSortButtons() {
        sortRelevance.setBackgroundResource(R.drawable.bg_sort_chip);
        sortRelevance.setTextColor(0xFF6B7280);

        sortNewest.setBackgroundResource(R.drawable.bg_sort_chip);
        sortNewest.setTextColor(0xFF6B7280);

        sortPriceLow.setBackgroundResource(R.drawable.bg_sort_chip);
        sortPriceLow.setTextColor(0xFF6B7280);

        sortPriceHigh.setBackgroundResource(R.drawable.bg_sort_chip);
        sortPriceHigh.setTextColor(0xFF6B7280);

        sortRating.setBackgroundResource(R.drawable.bg_sort_chip);
        sortRating.setTextColor(0xFF6B7280);
    }

    private void updateSavedChipState() {
        if (filterSavedOnly) {
            sortSaved.setBackgroundResource(R.drawable.bg_sort_chip_selected);
            sortSaved.setTextColor(0xFFFFFFFF);
            return;
        }
        sortSaved.setBackgroundResource(R.drawable.bg_sort_chip);
        sortSaved.setTextColor(0xFF6B7280);
    }

    private void updateSearchSummary(int resultCount) {
        String query = etSearchQuery.getText() != null
                ? etSearchQuery.getText().toString().trim() : "";
        if (filterSavedOnly) {
            tvSearchResults.setText("Tin đã lưu");
        } else if (TextUtils.isEmpty(query)) {
            tvSearchResults.setText("Tất cả sản phẩm");
        } else {
            tvSearchResults.setText("Kết quả cho \"" + query + "\"");
        }
        tvResultCount.setText("(" + resultCount + ")");
    }

    private String normalizedQuery() {
        String query = etSearchQuery.getText() != null
                ? etSearchQuery.getText().toString().trim() : "";
        return query.toLowerCase(Locale.ROOT);
    }

    private String safeLower(String value) {
        return value != null ? value.toLowerCase(Locale.ROOT) : "";
    }

    private double safePrice(Product product) {
        return product != null && product.getPrice() != null ? product.getPrice() : 0;
    }

    private double safeRating(Product product) {
        if (product == null || product.getId() == null) {
            return 0;
        }
        return productRatingById.containsKey(product.getId())
                ? productRatingById.get(product.getId()) : 0;
    }

    private String buildSortKey(Product product) {
        if (product == null) {
            return "";
        }
        if (!TextUtils.isEmpty(product.getCreated_at())) {
            return product.getCreated_at();
        }
        return product.getId() != null ? product.getId() : "";
    }

    private void openProductDetail(Product product, String imageUrl, String categoryName) {
        ProductDetailBottomSheetFragment bottomSheet =
                ProductDetailBottomSheetFragment.newInstance(product, imageUrl, categoryName);
        bottomSheet.show(getChildFragmentManager(), "product_detail");
    }

    private String firstProductImage(Product product) {
        if (product == null || product.getImage_urls() == null || product.getImage_urls().isEmpty()) {
            return null;
        }
        return product.getImage_urls().get(0);
    }

    private void showSaveSearchDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || TextUtils.isEmpty(user.getUid())) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để lưu tìm kiếm.", Toast.LENGTH_SHORT).show();
            return;
        }

        SavedSearch draft = buildSavedSearchDraft(user.getUid());
        final EditText input = new EditText(requireContext());
        input.setHint("Ví dụ: Laptop dưới 10 triệu");
        input.setText(buildSavedSearchNameFallback());
        input.setSelection(input.getText().length());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Lưu tìm kiếm")
                .setMessage("Lưu bộ lọc hiện tại để mở lại nhanh và nhận thông báo khi có tin mới phù hợp.")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = input.getText() != null ? input.getText().toString().trim() : "";
                    draft.setName(!TextUtils.isEmpty(name) ? name : buildSavedSearchNameFallback());
                    draft.setId(FirestoreIds.stableDocId(
                            "saved_search",
                            draft.getUser_id(),
                            draft.getName(),
                            String.valueOf(System.currentTimeMillis())));
                    savedSearchService.save(draft, result -> {
                        if (!isAdded()) {
                            return;
                        }
                        if (result.isSuccess()) {
                            Toast.makeText(requireContext(), "Đã lưu tìm kiếm.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), result.getError(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private SavedSearch buildSavedSearchDraft(String userId) {
        SavedSearch savedSearch = new SavedSearch();
        String now = nowIsoUtc();
        savedSearch.setUser_id(userId);
        savedSearch.setQuery(etSearchQuery.getText() != null ? etSearchQuery.getText().toString().trim() : "");
        savedSearch.setCategory_id(resolveCategoryFilterId());
        savedSearch.setMin_price(minPrice > 0d ? minPrice : 0d);
        savedSearch.setMax_price(maxPrice < Double.MAX_VALUE ? maxPrice : null);
        savedSearch.setFilter_new(filterNew);
        savedSearch.setFilter_used(filterUsed);
        savedSearch.setFilter_saved_only(filterSavedOnly);
        savedSearch.setSort(currentSort);
        savedSearch.setAlerts_enabled(true);
        savedSearch.setLast_seen_product_created_at(latestFilteredProductCreatedAt());
        savedSearch.setCreated_at(now);
        savedSearch.setUpdated_at(now);
        return savedSearch;
    }

    private String buildSavedSearchNameFallback() {
        String query = etSearchQuery.getText() != null ? etSearchQuery.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(query)) {
            return query;
        }
        if (filterSavedOnly) {
            return "Tin đã lưu";
        }
        if (filterNew) {
            return "Sản phẩm mới";
        }
        if (filterUsed) {
            return "Sản phẩm đã qua sử dụng";
        }
        if (minPrice > 0d || maxPrice < Double.MAX_VALUE) {
            return "Bộ lọc giá";
        }
        return "Tất cả sản phẩm";
    }

    private String resolveCategoryFilterId() {
        String query = etSearchQuery.getText() != null ? etSearchQuery.getText().toString().trim() : "";
        if (!TextUtils.isEmpty(initialCategoryId)
                && !TextUtils.isEmpty(initialCategoryName)
                && initialCategoryName.equalsIgnoreCase(query)) {
            return initialCategoryId;
        }
        return null;
    }

    private String latestFilteredProductCreatedAt() {
        String latest = null;
        for (Product product : filteredProductList) {
            if (product == null || TextUtils.isEmpty(product.getCreated_at())) {
                continue;
            }
            if (latest == null || product.getCreated_at().compareTo(latest) > 0) {
                latest = product.getCreated_at();
            }
        }
        return latest;
    }

    private String nowIsoUtc() {
        java.text.SimpleDateFormat format =
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return format.format(new java.util.Date());
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
