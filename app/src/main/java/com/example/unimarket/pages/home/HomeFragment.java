package com.example.unimarket.pages.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.ProductImage;
import com.example.unimarket.data.service.base.AsyncCrudService;

import java.util.ArrayList;
import java.util.Comparator;
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

        initViews(view);
        setupRecyclerViews();
        setupUserInfo();
        setupClicks();
        loadHomeData();
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

    private void setupUserInfo() {
        String userName = null;
        String userAvatarUrl = null;

        Bundle args = getArguments();
        if (args != null) {
            userName = args.getString("user_name");
            userAvatarUrl = args.getString("user_avatar");
        }

        if (TextUtils.isEmpty(userName) && requireActivity().getIntent() != null) {
            userName = requireActivity().getIntent().getStringExtra("user_name");
            userAvatarUrl = requireActivity().getIntent().getStringExtra("user_avatar");
        }

        if (TextUtils.isEmpty(userName)) {
            userName = "Nguyen Van A";
        }

        tvUserName.setText(userName);

        if (!TextUtils.isEmpty(userAvatarUrl)) {
            tvAvatar.setVisibility(View.GONE);
            Glide.with(this)
                    .load(userAvatarUrl)
                    .centerCrop()
                    .into(ivAvatar);
        } else {
            tvAvatar.setText(HomeUiUtils.extractInitial(userName));
            tvAvatar.setVisibility(View.VISIBLE);
        }
    }

    private void loadHomeData() {
        loadCategories();
        loadProducts();
        loadProductImages();
    }

    private void loadCategories() {
        AsyncCrudService.getAll(
                "categories",
                Category.class,
                new AsyncCrudService.ListCallback<Category>() {
                    @Override
                    public void onSuccess(List<Category> data) {
                        if (!isAdded()) {
                            return;
                        }

                        categoryList.clear();
                        if (data != null && !data.isEmpty()) {
                            categoryList.addAll(data);
                        } else {
                            categoryList.addAll(buildFallbackCategories());
                        }

                        categoryAdapter.submitList(categoryList);
                        rebuildCategoryMap();
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) {
                            return;
                        }

                        categoryList.clear();
                        categoryList.addAll(buildFallbackCategories());
                        categoryAdapter.submitList(categoryList);
                        rebuildCategoryMap();
                        Toast.makeText(
                                requireContext(),
                                "Khong tai duoc danh muc, dang hien thi du lieu mau",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void loadProducts() {
        AsyncCrudService.getAll(
                "products",
                Product.class,
                new AsyncCrudService.ListCallback<Product>() {
                    @Override
                    public void onSuccess(List<Product> data) {
                        if (!isAdded()) {
                            return;
                        }

                        productList.clear();
                        if (data != null && !data.isEmpty()) {
                            List<Product> sortedProducts = new ArrayList<>(data);
                            sortedProducts.sort(Comparator.comparing((Product product) ->
                                            product != null && product.getId() != null ? product.getId() : "")
                                    .reversed());

                            int maxItems = Math.min(sortedProducts.size(), 12);
                            for (int i = 0; i < maxItems; i++) {
                                productList.add(sortedProducts.get(i));
                            }
                        } else {
                            productList.addAll(buildFallbackProducts());
                        }

                        productAdapter.submitList(productList);
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) {
                            return;
                        }

                        productList.clear();
                        productList.addAll(buildFallbackProducts());
                        productAdapter.submitList(productList);
                        Toast.makeText(
                                requireContext(),
                                "Khong tai duoc san pham, dang hien thi du lieu mau",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void loadProductImages() {
        AsyncCrudService.getAll(
                "product_images",
                ProductImage.class,
                new AsyncCrudService.ListCallback<ProductImage>() {
                    @Override
                    public void onSuccess(List<ProductImage> data) {
                        if (!isAdded()) {
                            return;
                        }

                        productImageById.clear();
                        if (data != null && !data.isEmpty()) {
                            for (ProductImage img : data) {
                                if (img != null && img.getProduct_id() != null && !TextUtils.isEmpty(img.getImage_url())) {
                                    if (!productImageById.containsKey(img.getProduct_id())) {
                                        productImageById.put(img.getProduct_id(), img.getImage_url());
                                    }
                                }
                            }
                        }
                        productAdapter.setProductImageMap(productImageById);
                    }

                    @Override
                    public void onError(String error) {
                        if (!TextUtils.isEmpty(error)) {
                            Log.d("HomeFragment", error);
                        }
                    }
                }
        );
    }

    private void rebuildCategoryMap() {
        categoryNameById.clear();
        for (Category category : categoryList) {
            if (category != null && category.getId() != null && !TextUtils.isEmpty(category.getName())) {
                categoryNameById.put(category.getId(), category.getName());
            }
        }
        productAdapter.setCategoryNameMap(categoryNameById);
    }

    private List<Category> buildFallbackCategories() {
        List<Category> fallback = new ArrayList<>();
        fallback.add(new Category("uuid-1", "Laptop", null));
        fallback.add(new Category("uuid-2", "Dien tu", null));
        fallback.add(new Category("uuid-3", "Sach", null));
        fallback.add(new Category("uuid-4", "Nhu yeu pham", null));
        return fallback;
    }

    private List<Product> buildFallbackProducts() {
        List<Product> fallback = new ArrayList<>();
        fallback.add(createFallbackProduct("uuid-101", "uuid-cat-1", "MacBook Pro M1 2020 8GB/256GB", 15500000d, "used"));
        fallback.add(createFallbackProduct("uuid-102", "uuid-cat-3", "Giao trinh Giai tich 1", 50000d, "good"));
        fallback.add(createFallbackProduct("uuid-103", "uuid-cat-2", "Tai nghe Bluetooth chong on", 890000d, "new"));
        fallback.add(createFallbackProduct("uuid-104", "uuid-cat-4", "Den ban hoc chong can", 120000d, "new"));
        fallback.add(createFallbackProduct("uuid-105", "uuid-cat-2", "Ban phim co mini RGB", 650000d, "used"));
        fallback.add(createFallbackProduct("uuid-106", "uuid-cat-4", "Binh nuoc giu nhiet 500ml", 95000d, "new"));
        return fallback;
    }

    private Product createFallbackProduct(String id, String categoryId, String title, Double price, String condition) {
        Product product = new Product();
        product.setId(id);
        product.setCategory_id(categoryId);
        product.setTitle(title);
        product.setPrice(price);
        product.setCondition(condition);
        product.setStatus("active");
        return product;
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
