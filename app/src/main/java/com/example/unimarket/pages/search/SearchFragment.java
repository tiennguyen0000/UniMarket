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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.Review;
import com.example.unimarket.data.service.ReviewService;
import com.example.unimarket.pages.home.HomeViewModel;
import com.example.unimarket.pages.home.ProductAdapter;
import com.example.unimarket.pages.home.ProductDetailBottomSheetFragment;
import com.example.unimarket.pages.post.PostListingFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchFragment extends Fragment {

    private EditText etSearchQuery;
    private ImageView btnFilter;
    private TextView tvSearchResults;
    private TextView tvResultCount;
    private TextView sortRelevance;
    private TextView sortNewest;
    private TextView sortPriceLow;
    private TextView sortPriceHigh;
    private TextView sortRating;
    private RecyclerView rvSearchProducts;

    private ProductAdapter productAdapter;
    private HomeViewModel homeViewModel;
    private final ReviewService reviewService = new ReviewService();

    private final List<Product> productList = new ArrayList<>();
    private final List<Product> filteredProductList = new ArrayList<>();
    private final Map<String, String> productImageById = new HashMap<>();
    private final Map<String, String> categoryNameById = new HashMap<>();
    private final Map<String, Double> productRatingById = new HashMap<>();
    private String currentSort = "relevance";

    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;
    private boolean filterNew = false;
    private boolean filterUsed = false;

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
        setupRefreshListener();
        setupObservers();
        highlightSelectedSort(sortRelevance);
        loadReviewRatings();
        homeViewModel.loadCatalogData();
    }

    private void initViews(View root) {
        etSearchQuery = root.findViewById(R.id.etSearchQuery);
        btnFilter = root.findViewById(R.id.btnFilter);
        tvSearchResults = root.findViewById(R.id.tvSearchResults);
        tvResultCount = root.findViewById(R.id.tvResultCount);
        rvSearchProducts = root.findViewById(R.id.rvSearchProducts);

        sortRelevance = root.findViewById(R.id.sortRelevance);
        sortNewest = root.findViewById(R.id.sortNewest);
        sortPriceLow = root.findViewById(R.id.sortPriceLow);
        sortPriceHigh = root.findViewById(R.id.sortPriceHigh);
        sortRating = root.findViewById(R.id.sortRating);
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(
                new ArrayList<>(),
                new HashMap<>(),
                product -> Toast.makeText(requireContext(), product.getTitle(), Toast.LENGTH_SHORT).show(),
                (product, imageUrl, categoryName) -> openProductDetail(product, imageUrl, categoryName)
        );
        rvSearchProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvSearchProducts.setNestedScrollingEnabled(false);
        rvSearchProducts.setAdapter(productAdapter);
        rvSearchProducts.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(12), true));
    }

    private void setupClicks() {
        btnFilter.setOnClickListener(v -> showFilterBottomSheet());

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
            Map<String, String> images = state.getProductImages();

            rebuildCategoryMap(categories);

            productList.clear();
            if (products != null) {
                productList.addAll(products);
            }

            productImageById.clear();
            if (images != null) {
                productImageById.putAll(images);
            }
            productAdapter.setProductImageMap(new HashMap<>(productImageById));
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
    }

    private void showFilterBottomSheet() {
        SearchFilterBottomSheetFragment bottomSheet = new SearchFilterBottomSheetFragment();
        bottomSheet.setInitialState(minPrice, maxPrice, filterNew, filterUsed);
        bottomSheet.setFilterListener(new SearchFilterBottomSheetFragment.FilterListener() {
            @Override
            public void onApplyFilter(double minPrice, double maxPrice, boolean filterNew,
                                      boolean filterUsed, boolean filterFreeShip, boolean filterFastShip) {
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

    private void highlightSelectedSort(TextView selectedView) {
        resetSortButtons();
        selectedView.setBackgroundResource(R.drawable.bg_sort_chip_selected);
        selectedView.setTextColor(0xFFFFFFFF);
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

    private void updateSearchSummary(int resultCount) {
        String query = etSearchQuery.getText() != null
                ? etSearchQuery.getText().toString().trim() : "";
        if (TextUtils.isEmpty(query)) {
            tvSearchResults.setText("Táº¥t cáº£ sáº£n pháº©m");
        } else {
            tvSearchResults.setText("Káº¿t quáº£ cho \"" + query + "\"");
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
