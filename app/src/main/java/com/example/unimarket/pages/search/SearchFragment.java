package com.example.unimarket.pages.search;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

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
import com.example.unimarket.pages.home.CartBottomSheetFragment;
import com.example.unimarket.pages.home.CartFlow;
import com.example.unimarket.pages.home.HomeViewModel;
import com.example.unimarket.pages.home.ProductAdapter;
import com.example.unimarket.pages.home.ProductDetailBottomSheetFragment;
import com.example.unimarket.pages.chat.ChatInboxBottomSheetFragment;
import com.example.unimarket.pages.post.PostListingFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchFragment extends Fragment {
    private static final String SEARCH_PREFS = "search_history";
    private static final String SEARCH_HISTORY_KEY = "queries";
    private static final int MAX_SEARCH_HISTORY = 6;
    private static final int INITIAL_SEARCH_PRODUCT_LIMIT = 10;

    private EditText etSearchQuery;
    private ImageView btnFilter;
    private ImageView btnSearchCart;
    private ImageView btnSearchChat;
    private View searchAppBarLayout;
    private LinearLayout layoutSearchHistoryDropdown;
    private LinearLayout layoutSearchHistoryList;
    private TextView btnClearSearchHistory;
    private NestedScrollView searchScrollView;
    private TextView tvSearchResults;
    private TextView tvResultCount;
    private TextView sortRelevance;
    private TextView sortNewest;
    private TextView sortPriceLow;
    private TextView sortPriceHigh;
    private TextView sortRating;
    private TextView sortSaved;
    private TextView sortMine;
    private RecyclerView rvSearchProducts;
    private View layoutSearchLoading;
    private View layoutSearchEmpty;
    private TextView tvSearchEmptyTitle;
    private TextView tvSearchEmptyMessage;
    private TextView btnShowMoreProducts;

    private ProductAdapter productAdapter;
    private HomeViewModel homeViewModel;
    private SearchStateViewModel searchStateViewModel;
    private final ReviewService reviewService = new ReviewService();
    private final ProductService productService = new ProductService();
    private final UserService userService = new UserService();
    private final WishlistService wishlistService = new WishlistService();
    private final SavedSearchService savedSearchService = new SavedSearchService();

    private final List<Product> productList = new ArrayList<>();
    private final List<Product> filteredProductList = new ArrayList<>();
    private final List<Product> currentSortedProductList = new ArrayList<>();
    private final List<TextView> sortButtons = new ArrayList<>();
    private final Map<String, String> categoryNameById = new HashMap<>();
    private final Map<String, Double> productRatingById = new HashMap<>();
    private final Set<String> savedProductIds = new HashSet<>();
    private final List<String> searchHistory = new ArrayList<>();
    private String currentSort = "relevance";

    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;
    private boolean filterNew = false;
    private boolean filterUsed = false;
    private boolean filterSavedOnly = false;
    private boolean filterSellerOnly = false;
    private String initialCategoryName;
    private String initialProductId;
    private String initialSavedSearchId;
    private boolean openedInitialProduct;
    private boolean catalogLoading = true;
    private boolean adminMode;
    private boolean searchBarHidden;
    private boolean showAllProducts;
    private String currentUserId;
    private String productBatchSignature = "";
    private boolean suppressQueryWatcher;
    private boolean restoredSavedScroll;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onPause() {
        persistSearchState();
        super.onPause();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        searchStateViewModel = new ViewModelProvider(requireActivity()).get(SearchStateViewModel.class);
        initViews(view);
        setupRecyclerView();
        restoreSearchState();
        setupClicks();
        setupSearchBarAutoHide();
        view.post(this::syncSearchContentTopPadding);
        applyInitialCategoryArgument();
        setupRefreshListener();
        setupObservers();
        highlightSelectedSortViewForCurrentSort();
        loadAdminState();
        loadWishlistState();
        loadReviewRatings();
        homeViewModel.loadCatalogData();
    }

    private void initViews(View root) {
        etSearchQuery = root.findViewById(R.id.etSearchQuery);
        btnFilter = root.findViewById(R.id.btnFilter);
        btnSearchCart = root.findViewById(R.id.btnSearchCart);
        btnSearchChat = root.findViewById(R.id.btnSearchChat);
        searchAppBarLayout = root.findViewById(R.id.searchAppBarLayout);
        searchScrollView = root.findViewById(R.id.searchScrollView);
        layoutSearchHistoryDropdown = root.findViewById(R.id.layoutSearchHistoryDropdown);
        layoutSearchHistoryList = root.findViewById(R.id.layoutSearchHistoryList);
        btnClearSearchHistory = root.findViewById(R.id.btnClearSearchHistory);
        tvSearchResults = root.findViewById(R.id.tvSearchResults);
        tvResultCount = root.findViewById(R.id.tvResultCount);
        rvSearchProducts = root.findViewById(R.id.rvSearchProducts);
        layoutSearchLoading = root.findViewById(R.id.layoutSearchLoading);
        layoutSearchEmpty = root.findViewById(R.id.layoutSearchEmpty);
        tvSearchEmptyTitle = root.findViewById(R.id.tvSearchEmptyTitle);
        tvSearchEmptyMessage = root.findViewById(R.id.tvSearchEmptyMessage);
        btnShowMoreProducts = root.findViewById(R.id.btnShowMoreProducts);

        sortRelevance = root.findViewById(R.id.sortRelevance);
        sortNewest = root.findViewById(R.id.sortNewest);
        sortPriceLow = root.findViewById(R.id.sortPriceLow);
        sortPriceHigh = root.findViewById(R.id.sortPriceHigh);
        sortRating = root.findViewById(R.id.sortRating);
        sortSaved = root.findViewById(R.id.sortSaved);
        sortMine = root.findViewById(R.id.sortMine);

        sortButtons.clear();
        sortButtons.add(sortRelevance);
        sortButtons.add(sortNewest);
        sortButtons.add(sortPriceLow);
        sortButtons.add(sortPriceHigh);
        sortButtons.add(sortRating);
        sortButtons.add(sortSaved);
        sortButtons.add(sortMine);
        prepareSortButtons();
    }

    private void restoreSearchState() {
        if (searchStateViewModel == null) {
            return;
        }
        currentSort = searchStateViewModel.sort;
        minPrice = searchStateViewModel.minPrice;
        maxPrice = searchStateViewModel.maxPrice;
        filterNew = searchStateViewModel.filterNew;
        filterUsed = searchStateViewModel.filterUsed;
        filterSavedOnly = searchStateViewModel.filterSavedOnly;
        filterSellerOnly = searchStateViewModel.filterSellerOnly;
        showAllProducts = searchStateViewModel.showAllProducts;
        productBatchSignature = searchStateViewModel.productBatchSignature;
        setSearchQueryText(searchStateViewModel.query);
    }

    private void persistSearchState() {
        if (searchStateViewModel == null) {
            return;
        }
        searchStateViewModel.query = etSearchQuery != null && etSearchQuery.getText() != null
                ? etSearchQuery.getText().toString() : "";
        searchStateViewModel.sort = currentSort;
        searchStateViewModel.minPrice = minPrice;
        searchStateViewModel.maxPrice = maxPrice;
        searchStateViewModel.filterNew = filterNew;
        searchStateViewModel.filterUsed = filterUsed;
        searchStateViewModel.filterSavedOnly = filterSavedOnly;
        searchStateViewModel.filterSellerOnly = filterSellerOnly;
        searchStateViewModel.showAllProducts = showAllProducts;
        searchStateViewModel.productBatchSignature = productBatchSignature;
        if (searchScrollView != null) {
            searchStateViewModel.scrollY = searchScrollView.getScrollY();
        }
    }

    private void setSearchQueryText(String query) {
        suppressQueryWatcher = true;
        String safeQuery = query != null ? query : "";
        etSearchQuery.setText(safeQuery);
        etSearchQuery.setSelection(etSearchQuery.getText() != null ? etSearchQuery.getText().length() : 0);
        suppressQueryWatcher = false;
    }

    private void prepareSortButtons() {
        for (TextView button : sortButtons) {
            ViewGroup.LayoutParams params = button.getLayoutParams();
            if (params != null) {
                params.height = dpToPx(56);
                button.setLayoutParams(params);
            }
            button.setGravity(android.view.Gravity.CENTER);
            button.setBackgroundColor(Color.TRANSPARENT);
            button.setCompoundDrawablePadding(dpToPx(4));
            button.setPadding(dpToPx(12), 0, dpToPx(12), 0);
        }
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
        productAdapter.setOnSellerEditClickListener(this::navigateToEditProduct);
        productAdapter.setOnSellerRemoveClickListener(this::showSellerRemoveDialog);
        rvSearchProducts.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rvSearchProducts.setNestedScrollingEnabled(false);
        rvSearchProducts.setItemAnimator(null);
        rvSearchProducts.setAdapter(productAdapter);
        rvSearchProducts.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(8), true));
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
        btnFilter.setOnClickListener(v -> {
            hideSearchHistoryDropdown();
            showFilterBottomSheet();
        });
        btnSearchCart.setOnClickListener(v -> {
            hideSearchHistoryDropdown();
            CartBottomSheetFragment.newInstance()
                    .show(requireActivity().getSupportFragmentManager(), "cart");
        });
        btnSearchChat.setOnClickListener(v -> {
            hideSearchHistoryDropdown();
            new ChatInboxBottomSheetFragment()
                    .show(requireActivity().getSupportFragmentManager(), "search_chat_inbox");
        });
        btnShowMoreProducts.setOnClickListener(v -> {
            showAllProducts = true;
            persistSearchState();
            renderVisibleProducts(true);
        });
        btnClearSearchHistory.setOnClickListener(v -> clearSearchHistory());

        loadSearchHistory();
        renderSearchHistoryDropdown();
        etSearchQuery.setOnClickListener(v -> showSearchHistoryDropdown());
        etSearchQuery.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showSearchBar();
                showSearchHistoryDropdown();
            } else {
                hideSearchHistoryDropdown();
            }
        });

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressQueryWatcher) {
                    return;
                }
                persistSearchState();
                applySearchFiltersAndSort();
                renderSearchHistoryDropdown();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                commitCurrentSearchQuery();
                hideSearchHistoryDropdown();
            }
            applySearchFiltersAndSort();
            return false;
        });

        sortRelevance.setOnClickListener(v -> setSortOption("relevance", sortRelevance));
        sortNewest.setOnClickListener(v -> setSortOption("newest", sortNewest));
        sortPriceLow.setOnClickListener(v -> setSortOption("price_low", sortPriceLow));
        sortPriceHigh.setOnClickListener(v -> setSortOption("price_high", sortPriceHigh));
        sortRating.setOnClickListener(v -> setSortOption("rating", sortRating));
        sortSaved.setOnClickListener(v -> toggleSavedOnly());
        sortMine.setOnClickListener(v -> toggleSellerOnly());
    }

    private void setupSearchBarAutoHide() {
        if (searchScrollView == null || searchAppBarLayout == null) {
            return;
        }
        searchScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (view, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (searchStateViewModel != null) {
                        searchStateViewModel.scrollY = scrollY;
                    }
                    int deltaY = scrollY - oldScrollY;
                    if (etSearchQuery.hasFocus() || layoutSearchHistoryDropdown.getVisibility() == View.VISIBLE) {
                        showSearchBar();
                        updateSearchContentFade(scrollY);
                        return;
                    }
                    if (scrollY <= dpToPx(8) || deltaY < -2) {
                        showSearchBar();
                    } else if (deltaY > 2) {
                        float hideDistance = Math.max(dpToPx(76), searchAppBarLayout.getHeight());
                        float progress = Math.min(1f, scrollY / hideDistance);
                        applySearchBarProgress(progress);
                    }
                    updateSearchContentFade(scrollY);
                });
    }

    private void applySearchBarProgress(float progress) {
        if (searchAppBarLayout == null) {
            return;
        }
        searchBarHidden = progress >= 1f;
        searchAppBarLayout.animate().cancel();
        searchAppBarLayout.setAlpha(1f - progress);
        searchAppBarLayout.setTranslationY(-searchAppBarLayout.getHeight() * progress);
        searchAppBarLayout.setVisibility(progress >= 1f ? View.INVISIBLE : View.VISIBLE);
        updateSearchContentFade(searchScrollView != null ? searchScrollView.getScrollY() : 0);
    }

    private void showSearchBar() {
        if (searchAppBarLayout == null || !searchBarHidden && searchAppBarLayout.getAlpha() >= 1f) {
            return;
        }
        searchBarHidden = false;
        searchAppBarLayout.setVisibility(View.VISIBLE);
        searchAppBarLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(160)
                .start();
        syncSearchContentTopPadding();
        updateSearchContentFade(searchScrollView != null ? searchScrollView.getScrollY() : 0);
    }

    private void syncSearchContentTopPadding() {
        if (searchScrollView == null || searchAppBarLayout == null) {
            return;
        }
        searchAppBarLayout.post(() -> {
            int topPadding = Math.max(dpToPx(76), searchAppBarLayout.getHeight());
            if (searchScrollView.getPaddingTop() != topPadding) {
                searchScrollView.setPadding(
                        searchScrollView.getPaddingLeft(),
                        topPadding,
                        searchScrollView.getPaddingRight(),
                        searchScrollView.getPaddingBottom());
            }
            updateSearchContentFade(searchScrollView.getScrollY());
        });
    }

    private void updateSearchContentFade(int scrollY) {
        // The bottom navigation scrim is owned by Controller. Search must not add a
        // second overlay here, otherwise product cards get washed out while scrolling.
    }

    private void loadSearchHistory() {
        searchHistory.clear();
        SharedPreferences preferences = requireContext()
                .getSharedPreferences(SEARCH_PREFS, android.content.Context.MODE_PRIVATE);
        String rawHistory = preferences.getString(SEARCH_HISTORY_KEY, "");
        if (TextUtils.isEmpty(rawHistory)) {
            return;
        }
        String[] items = rawHistory.split("\\n");
        for (String item : items) {
            String query = item != null ? item.trim() : "";
            if (!TextUtils.isEmpty(query) && !searchHistory.contains(query)) {
                searchHistory.add(query);
            }
            if (searchHistory.size() >= MAX_SEARCH_HISTORY) {
                break;
            }
        }
    }

    private void saveSearchHistory() {
        StringBuilder builder = new StringBuilder();
        for (String query : searchHistory) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(query);
        }
        requireContext()
                .getSharedPreferences(SEARCH_PREFS, android.content.Context.MODE_PRIVATE)
                .edit()
                .putString(SEARCH_HISTORY_KEY, builder.toString())
                .apply();
    }

    private void commitCurrentSearchQuery() {
        String query = etSearchQuery.getText() != null ? etSearchQuery.getText().toString().trim() : "";
        if (TextUtils.isEmpty(query)) {
            return;
        }
        for (int i = searchHistory.size() - 1; i >= 0; i--) {
            if (query.equalsIgnoreCase(searchHistory.get(i))) {
                searchHistory.remove(i);
            }
        }
        searchHistory.add(0, query);
        while (searchHistory.size() > MAX_SEARCH_HISTORY) {
            searchHistory.remove(searchHistory.size() - 1);
        }
        saveSearchHistory();
        renderSearchHistoryDropdown();
    }

    private void clearSearchHistory() {
        searchHistory.clear();
        saveSearchHistory();
        hideSearchHistoryDropdown();
    }

    private void showSearchHistoryDropdown() {
        showSearchBar();
        renderSearchHistoryDropdown();
    }

    private void hideSearchHistoryDropdown() {
        if (layoutSearchHistoryDropdown != null) {
            layoutSearchHistoryDropdown.setVisibility(View.GONE);
        }
        syncSearchContentTopPadding();
    }

    private void renderSearchHistoryDropdown() {
        if (layoutSearchHistoryList == null || layoutSearchHistoryDropdown == null) {
            return;
        }
        layoutSearchHistoryList.removeAllViews();

        String typedQuery = etSearchQuery.getText() != null
                ? etSearchQuery.getText().toString().trim().toLowerCase(Locale.ROOT) : "";
        int renderedCount = 0;
        for (String query : searchHistory) {
            if (!TextUtils.isEmpty(typedQuery)
                    && !query.toLowerCase(Locale.ROOT).contains(typedQuery)) {
                continue;
            }
            layoutSearchHistoryList.addView(createSearchHistoryRow(query));
            renderedCount++;
            if (renderedCount >= MAX_SEARCH_HISTORY) {
                break;
            }
        }

        boolean shouldShow = etSearchQuery.hasFocus() && renderedCount > 0;
        layoutSearchHistoryDropdown.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        syncSearchContentTopPadding();
    }

    private View createSearchHistoryRow(String query) {
        TextView row = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(42));
        params.topMargin = dpToPx(6);
        row.setLayoutParams(params);
        row.setBackgroundResource(R.drawable.bg_search_history_item);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(12), 0, dpToPx(12), 0);
        row.setSingleLine(true);
        row.setEllipsize(TextUtils.TruncateAt.END);
        row.setText(query);
        row.setTextColor(0xFF101828);
        row.setTextSize(14);
        row.setOnClickListener(v -> {
            setSearchQueryText(query);
            persistSearchState();
            commitCurrentSearchQuery();
            hideSearchHistoryDropdown();
            applySearchFiltersAndSort();
        });
        return row;
    }

    private void applyInitialCategoryArgument() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        initialCategoryName = args.getString("category_name");
        initialProductId = args.getString("product_id");
        initialSavedSearchId = args.getString("saved_search_id");
        if (!TextUtils.isEmpty(initialCategoryName)) {
            setSearchQueryText(initialCategoryName);
            persistSearchState();
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
                    homeViewModel.refreshCatalogData();
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

            updateProductBatch(products);

            productAdapter.setCategoryNameMap(new HashMap<>(categoryNameById));

            Map<String, String> avatars = state.getSellerAvatars();
            if (avatars != null) {
                productAdapter.setSellerAvatarMap(new HashMap<>(avatars));
            }

            applySearchFiltersAndSort(false);
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
            applySearchFiltersAndSort(false);
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
                applySearchFiltersAndSort(false);
            }

            @Override
            public void onError(String error) {
                savedProductIds.clear();
                productAdapter.setFavoriteIds(savedProductIds);
                applySearchFiltersAndSort(false);
            }
        });
    }

    private void applySearchFiltersAndSort() {
        applySearchFiltersAndSort(true);
    }

    private void applySearchFiltersAndSort(boolean resetShowAll) {
        String query = normalizedQuery();
        if (resetShowAll) {
            showAllProducts = false;
        }
        persistSearchState();
        filteredProductList.clear();
        filteredProductList.addAll(productList.stream()
                .filter(product -> product != null)
                .filter(product -> matchesQuery(product, query))
                .filter(this::matchesViewMode)
                .filter(this::matchesPriceFilter)
                .filter(this::matchesConditionFilter)
                .collect(Collectors.toList()));

        applySort();
    }

    private boolean matchesViewMode(Product product) {
        if (filterSellerOnly) {
            return isSellerViewProduct(product);
        }
        if (!isVisibleProduct(product)) {
            return false;
        }
        return !filterSavedOnly || savedProductIds.contains(product.getId());
    }

    private boolean matchesPriceFilter(Product product) {
        double price = product.getPrice() != null ? product.getPrice() : 0;
        return price >= minPrice && price <= maxPrice;
    }

    private boolean matchesConditionFilter(Product product) {
        if (!filterNew && !filterUsed) {
            return true;
        }
        String condition = product.getCondition();
        boolean isNew = "new".equalsIgnoreCase(condition) || "NEW".equals(condition);
        boolean isUsed = "used".equalsIgnoreCase(condition)
                || "good".equalsIgnoreCase(condition)
                || "USED".equals(condition);
        return (filterNew && isNew) || (filterUsed && isUsed);
    }

    private void toggleSavedOnly() {
        filterSavedOnly = !filterSavedOnly;
        if (filterSavedOnly) {
            filterSellerOnly = false;
        }
        persistSearchState();
        updateSavedChipState();
        updateSellerChipState();
        applySearchFiltersAndSort();
    }

    private void toggleSellerOnly() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Đăng nhập để xem tin của bạn.", Toast.LENGTH_SHORT).show();
            return;
        }
        currentUserId = user.getUid();
        filterSellerOnly = !filterSellerOnly;
        if (filterSellerOnly) {
            filterSavedOnly = false;
        }
        persistSearchState();
        updateSavedChipState();
        updateSellerChipState();
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
        setSearchQueryText(savedSearch.getQuery() != null ? savedSearch.getQuery() : "");
        minPrice = savedSearch.getMin_price() != null ? savedSearch.getMin_price() : 0d;
        maxPrice = savedSearch.getMax_price() != null ? savedSearch.getMax_price() : Double.MAX_VALUE;
        filterNew = savedSearch.isFilter_new();
        filterUsed = savedSearch.isFilter_used();
        filterSavedOnly = savedSearch.isFilter_saved_only();
        filterSellerOnly = false;
        currentSort = !TextUtils.isEmpty(savedSearch.getSort()) ? savedSearch.getSort() : "relevance";
        persistSearchState();
        highlightSelectedSortViewForCurrentSort();
        updateSavedChipState();
        updateSellerChipState();
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
        if (status.equals("removed")) {
            return false;
        }
        int quantity = product.getQuantity() != null ? Math.max(0, product.getQuantity()) : 1;
        return quantity > 0
                && !status.equals("inactive")
                && !status.equals("hidden")
                && !status.equals("sold");
    }

    private boolean isSellerViewProduct(Product product) {
        if (product == null || TextUtils.isEmpty(currentUserId)
                || !currentUserId.equals(product.getSeller_id())) {
            return false;
        }
        String status = product.getStatus() != null
                ? product.getStatus().trim().toLowerCase(Locale.ROOT)
                : "active";
        return !status.equals("removed");
    }

    private void showRestockDialog(Product product) {
        if (product == null || TextUtils.isEmpty(product.getId())) {
            return;
        }
        EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setHint("Số lượng mới");
        input.setText(String.valueOf(product.getQuantity() != null ? Math.max(0, product.getQuantity()) : 0));
        input.setSelectAllOnFocus(true);
        int padding = dpToPx(18);
        input.setPadding(padding, padding / 2, padding, padding / 2);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cập nhật số lượng")
                .setMessage("Nhập số lượng hiện có. Tin sẽ hiển thị lại khi số lượng lớn hơn 0.")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> saveRestockQuantity(product, input))
                .show();
    }

    private void saveRestockQuantity(Product product, EditText input) {
        String raw = input.getText() != null ? input.getText().toString().trim() : "";
        int quantity;
        try {
            quantity = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Số lượng không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (quantity < 0) {
            Toast.makeText(requireContext(), "Số lượng không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }
        product.setQuantity(quantity);
        product.setStatus(quantity > 0 ? "active" : "hidden");
        product.setUpdated_at(nowIsoUtc());
        productService.save(product, result -> {
            if (!isAdded()) {
                return;
            }
            if (result.isSuccess()) {
                Toast.makeText(requireContext(), "Đã cập nhật số lượng.", Toast.LENGTH_SHORT).show();
                homeViewModel.refreshCatalogData();
            } else {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Không thể cập nhật")
                        .setMessage(result.getError())
                        .setPositiveButton("Đóng", null)
                        .show();
            }
        });
    }

    private void navigateToEditProduct(Product product) {
        if (product == null || TextUtils.isEmpty(product.getId())) {
            return;
        }
        Bundle args = new Bundle();
        args.putString(PostListingFragment.ARG_EDIT_PRODUCT_ID, product.getId());
        NavHostFragment.findNavController(this).navigate(R.id.postListingFragment, args);
    }

    private void showSellerRemoveDialog(Product product) {
        if (product == null || TextUtils.isEmpty(product.getId())) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Gỡ bài đăng")
                .setMessage("Tin sẽ không còn hiển thị với người mua, nhưng vẫn được giữ trong hệ thống.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Gỡ bài", (dialog, which) -> removeOwnProduct(product))
                .show();
    }

    private void removeOwnProduct(Product product) {
        product.setStatus("removed");
        product.setUpdated_at(nowIsoUtc());
        productService.save(product, result -> {
            if (!isAdded()) {
                return;
            }
            if (result.isSuccess()) {
                Toast.makeText(requireContext(), "Đã gỡ bài đăng.", Toast.LENGTH_SHORT).show();
                homeViewModel.refreshCatalogData();
            } else {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Không thể gỡ bài")
                        .setMessage(result.getError())
                        .setPositiveButton("Đóng", null)
                        .show();
            }
        });
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
                homeViewModel.refreshCatalogData();
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

        currentSortedProductList.clear();
        currentSortedProductList.addAll(sortedList);
        renderVisibleProducts(false);
        updateSearchSummary(currentSortedProductList.size());
        updateSearchLoading();
        updateEmptyState(currentSortedProductList.size());
        openInitialProductIfReady();
    }

    private void renderVisibleProducts(boolean preserveScroll) {
        int scrollY = searchScrollView != null ? searchScrollView.getScrollY() : 0;
        int totalCount = currentSortedProductList.size();
        int visibleCount = showAllProducts
                ? totalCount
                : Math.min(INITIAL_SEARCH_PRODUCT_LIMIT, totalCount);
        List<Product> visibleProducts = new ArrayList<>(
                currentSortedProductList.subList(0, visibleCount));

        if (preserveScroll && showAllProducts && productAdapter.getItemCount() <= totalCount) {
            int currentCount = Math.max(0, productAdapter.getItemCount());
            if (currentCount < totalCount) {
                productAdapter.appendItems(new ArrayList<>(
                        currentSortedProductList.subList(currentCount, totalCount)));
            }
        } else {
            productAdapter.submitList(visibleProducts);
        }
        if (btnShowMoreProducts != null) {
            boolean canShowMore = !showAllProducts && totalCount > INITIAL_SEARCH_PRODUCT_LIMIT;
            btnShowMoreProducts.setVisibility(canShowMore ? View.VISIBLE : View.GONE);
        }
        if (preserveScroll && searchScrollView != null) {
            searchScrollView.post(() -> searchScrollView.scrollTo(searchScrollView.getScrollX(), scrollY));
        }
        restoreSavedScrollIfNeeded();
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
            if (btnShowMoreProducts != null) btnShowMoreProducts.setVisibility(View.GONE);
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
        if ((showEmpty || showLoading) && btnShowMoreProducts != null) {
            btnShowMoreProducts.setVisibility(View.GONE);
        }

        if (showEmpty) {
            String query = etSearchQuery.getText() != null
                    ? etSearchQuery.getText().toString().trim() : "";
            if (filterSellerOnly && FirebaseAuth.getInstance().getCurrentUser() == null) {
                tvSearchEmptyTitle.setText("Đăng nhập để xem tin của bạn");
                tvSearchEmptyMessage.setText("Chế độ này chỉ hiển thị các tin do tài khoản hiện tại đăng.");
            } else if (filterSellerOnly) {
                tvSearchEmptyTitle.setText("Bạn chưa có tin đăng nào");
                tvSearchEmptyMessage.setText("Tin hết hàng hoặc đang ẩn cũng sẽ xuất hiện ở đây để bạn cập nhật số lượng.");
            } else if (filterSavedOnly && FirebaseAuth.getInstance().getCurrentUser() == null) {
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
        showAllProducts = false;
        clearSpecialViewModes();
        highlightSelectedSort(selectedView);
        applySearchFiltersAndSort();
    }

    private void clearSpecialViewModes() {
        filterSavedOnly = false;
        filterSellerOnly = false;
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
        styleSortButton(selectedView, true);
        updateSavedChipState();
        updateSellerChipState();
    }

    private void resetSortButtons() {
        for (TextView button : sortButtons) {
            styleSortButton(button, false);
        }
    }

    private void updateSavedChipState() {
        styleSortButton(sortSaved, filterSavedOnly);
    }

    private void updateSellerChipState() {
        styleSortButton(sortMine, filterSellerOnly);
    }

    private void styleSortButton(TextView view, boolean selected) {
        int color = selected ? 0xFF21409A : 0xFF667085;
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setTextColor(color);
        view.setCompoundDrawables(null, tintedSortIcon(view, color), null, null);
    }

    private Drawable tintedSortIcon(TextView view, int color) {
        Drawable drawable = requireContext().getDrawable(sortIconRes(view.getId()));
        if (drawable == null) {
            return null;
        }
        drawable = drawable.mutate();
        drawable.setTint(color);
        drawable.setBounds(0, 0, dpToPx(22), dpToPx(22));
        return drawable;
    }

    private int sortIconRes(int viewId) {
        if (viewId == R.id.sortRelevance) return R.drawable.suitable;
        if (viewId == R.id.sortNewest) return R.drawable.new1;
        if (viewId == R.id.sortPriceLow) return R.drawable.cheap;
        if (viewId == R.id.sortPriceHigh) return R.drawable.expensive;
        if (viewId == R.id.sortRating) return R.drawable.appreciate;
        if (viewId == R.id.sortSaved) return R.drawable.save;
        if (viewId == R.id.sortMine) return R.drawable.self;
        return R.drawable.suitable;
    }

    private void updateSearchSummary(int resultCount) {
        String query = etSearchQuery.getText() != null
                ? etSearchQuery.getText().toString().trim() : "";
        if (filterSellerOnly) {
            tvSearchResults.setText("Tin của tôi");
        } else if (filterSavedOnly) {
            tvSearchResults.setText("Tin đã lưu");
        } else if (TextUtils.isEmpty(query)) {
            tvSearchResults.setText("");
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

    private String nowIsoUtc() {
        java.text.SimpleDateFormat format =
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return format.format(new java.util.Date());
    }

    private void updateProductBatch(List<Product> products) {
        String newSignature = buildProductBatchSignature(products);
        if (newSignature.equals(productBatchSignature) && !productList.isEmpty()) {
            return;
        }

        productBatchSignature = newSignature;
        productList.clear();
        if (products == null || products.isEmpty()) {
            if (searchStateViewModel != null) {
                searchStateViewModel.productBatchSignature = "";
                searchStateViewModel.shuffledProductIds.clear();
            }
            return;
        }

        if (searchStateViewModel != null
                && newSignature.equals(searchStateViewModel.productBatchSignature)
                && !searchStateViewModel.shuffledProductIds.isEmpty()) {
            productList.addAll(orderProductsBySavedIds(products, searchStateViewModel.shuffledProductIds));
            return;
        }

        productList.addAll(products);
        Collections.shuffle(productList);
        saveShuffledProductOrder();
        persistSearchState();
    }

    private List<Product> orderProductsBySavedIds(List<Product> products, List<String> savedIds) {
        Map<String, Product> productById = new HashMap<>();
        List<Product> withoutIds = new ArrayList<>();
        for (Product product : products) {
            if (product == null) {
                continue;
            }
            if (!TextUtils.isEmpty(product.getId())) {
                productById.put(product.getId(), product);
            } else {
                withoutIds.add(product);
            }
        }

        List<Product> ordered = new ArrayList<>();
        for (String id : savedIds) {
            Product product = productById.remove(id);
            if (product != null) {
                ordered.add(product);
            }
        }
        ordered.addAll(productById.values());
        ordered.addAll(withoutIds);
        return ordered;
    }

    private void saveShuffledProductOrder() {
        if (searchStateViewModel == null) {
            return;
        }
        searchStateViewModel.productBatchSignature = productBatchSignature;
        searchStateViewModel.shuffledProductIds.clear();
        for (Product product : productList) {
            if (product != null && !TextUtils.isEmpty(product.getId())) {
                searchStateViewModel.shuffledProductIds.add(product.getId());
            }
        }
    }

    private void restoreSavedScrollIfNeeded() {
        if (restoredSavedScroll || searchStateViewModel == null
                || searchScrollView == null || searchStateViewModel.scrollY <= 0) {
            return;
        }
        restoredSavedScroll = true;
        int targetScrollY = searchStateViewModel.scrollY;
        searchScrollView.post(() -> searchScrollView.scrollTo(searchScrollView.getScrollX(), targetScrollY));
    }

    private String buildProductBatchSignature(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Product product : products) {
            if (product == null) {
                continue;
            }
            builder.append(product.getId() != null ? product.getId() : "")
                    .append(':')
                    .append(product.getUpdated_at() != null ? product.getUpdated_at() : "")
                    .append(':')
                    .append(product.getQuantity() != null ? product.getQuantity() : "")
                    .append(':')
                    .append(product.getStatus() != null ? product.getStatus() : "")
                    .append('|');
        }
        return builder.toString();
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
