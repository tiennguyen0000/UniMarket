package com.example.unimarket.pages.search;

import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unimarket.R;
import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.pages.home.HomeViewModel;
import com.example.unimarket.pages.home.ProductAdapter;
import com.example.unimarket.pages.home.ProductDetailBottomSheetFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchFragment extends Fragment {

    private EditText etSearchQuery;
    private ImageView btnFilter;
    private TextView tvSearchResults;
    private TextView tvResultCount;
    private TextView sortRelevance, sortNewest, sortPriceLow, sortPriceHigh, sortRating;
    private RecyclerView rvSearchProducts;
    
    private ProductAdapter productAdapter;
    private HomeViewModel homeViewModel;
    
    private final List<Product> productList = new ArrayList<>();
    private final List<Product> filteredProductList = new ArrayList<>();
    private final Map<String, String> productImageById = new HashMap<>();
    private final Map<String, String> categoryNameById = new HashMap<>();
    private String currentSort = "relevance";
    
    // Filter state
    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;
    private boolean filterNew = false;
    private boolean filterUsed = false;
    private boolean filterFreeShip = false;
    private boolean filterFastShip = false;

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
        setupObservers();
        homeViewModel.loadHomeData();
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
        
        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });
        
        // Sort options
        sortRelevance.setOnClickListener(v -> setSortOption("relevance", sortRelevance));
        sortNewest.setOnClickListener(v -> setSortOption("newest", sortNewest));
        sortPriceLow.setOnClickListener(v -> setSortOption("price_low", sortPriceLow));
        sortPriceHigh.setOnClickListener(v -> setSortOption("price_high", sortPriceHigh));
        sortRating.setOnClickListener(v -> setSortOption("rating", sortRating));
    }

    private void setupObservers() {
        homeViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (!isAdded() || state == null) {
                return;
            }

            List<Product> products = state.getProducts();
            Map<String, String> images = state.getProductImages();

            productList.clear();
            if (products != null) {
                productList.addAll(products);
            }
            
            // Initially show all products
            applyFiltersAndSort();

            productImageById.clear();
            if (images != null) {
                productImageById.putAll(images);
            }
            productAdapter.setProductImageMap(new HashMap<>(productImageById));
        });
    }

    private void performSearch() {
        String query = etSearchQuery.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            applyFiltersAndSort();
            return;
        }
        
        String searchLower = query.toLowerCase();
        filteredProductList.clear();
        
        for (Product product : productList) {
            if (product != null && product.getTitle() != null && 
                product.getTitle().toLowerCase().contains(searchLower)) {
                filteredProductList.add(product);
            }
        }
        
        applySort();
    }

    private void applyFiltersAndSort() {
        filteredProductList.clear();
        
        for (Product product : productList) {
            if (product == null) continue;
            
            // Check price filter
            double price = product.getPrice() != null ? product.getPrice() : 0;
            if (price < minPrice || price > maxPrice) continue;
            
            // Check condition filter (status)
            String status = product.getStatus();
            if (filterNew || filterUsed) {
                boolean isNew = "new".equalsIgnoreCase(status);
                if (filterNew && !isNew) continue;
                if (filterUsed && isNew) continue;
            }
            
            filteredProductList.add(product);
        }
        
        applySort();
    }

    private void applySort() {
        List<Product> sortedList = new ArrayList<>(filteredProductList);
        
        switch (currentSort) {
            case "price_low":
                sortedList.sort((a, b) -> {
                    double priceA = a.getPrice() != null ? a.getPrice() : 0;
                    double priceB = b.getPrice() != null ? b.getPrice() : 0;
                    return Double.compare(priceA, priceB);
                });
                break;
            case "price_high":
                sortedList.sort((a, b) -> {
                    double priceA = a.getPrice() != null ? a.getPrice() : 0;
                    double priceB = b.getPrice() != null ? b.getPrice() : 0;
                    return Double.compare(priceB, priceA);
                });
                break;
            case "newest":
                sortedList.sort((a, b) -> {
                    String timeA = a.getCreated_at() != null ? a.getCreated_at() : "";
                    String timeB = b.getCreated_at() != null ? b.getCreated_at() : "";
                    return timeB.compareTo(timeA); // Reverse for newest first
                });
                break;
            case "relevance":
            default:
                // Already in relevance order from query match
                break;
        }
        
        productAdapter.submitList(new ArrayList<>(sortedList));
        tvResultCount.setText("(" + sortedList.size() + ")");
    }

    private void showFilterBottomSheet() {
        SearchFilterBottomSheetFragment bottomSheet = new SearchFilterBottomSheetFragment();
        bottomSheet.setFilterListener(new SearchFilterBottomSheetFragment.FilterListener() {
            @Override
            public void onApplyFilter(double minPrice, double maxPrice, boolean filterNew, boolean filterUsed, boolean filterFreeShip, boolean filterFastShip) {
                SearchFragment.this.minPrice = minPrice;
                SearchFragment.this.maxPrice = maxPrice;
                SearchFragment.this.filterNew = filterNew;
                SearchFragment.this.filterUsed = filterUsed;
                SearchFragment.this.filterFreeShip = filterFreeShip;
                SearchFragment.this.filterFastShip = filterFastShip;
                applyFiltersAndSort();
            }

            @Override
            public void onResetFilter() {
                SearchFragment.this.minPrice = 0;
                SearchFragment.this.maxPrice = Double.MAX_VALUE;
                SearchFragment.this.filterNew = false;
                SearchFragment.this.filterUsed = false;
                SearchFragment.this.filterFreeShip = false;
                SearchFragment.this.filterFastShip = false;
                applyFiltersAndSort();
            }
        });
        bottomSheet.show(getChildFragmentManager(), "search_filter");
    }

    private void setSortOption(String sortOption, TextView selectedView) {
        currentSort = sortOption;
        
        // Reset all
        resetSortButtons();
        
        // Set selected
        selectedView.setBackgroundResource(R.drawable.bg_sort_chip_selected);
        selectedView.setTextColor(0xFFFFFFFF);
        
        // Apply sort immediately
        applySort();
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

    private void openProductDetail(Product product, String imageUrl, String categoryName) {
        ProductDetailBottomSheetFragment bottomSheet = ProductDetailBottomSheetFragment.newInstance(product, imageUrl, categoryName);
        bottomSheet.show(getChildFragmentManager(), "product_detail");
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    // GridSpacingItemDecoration inner class
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
