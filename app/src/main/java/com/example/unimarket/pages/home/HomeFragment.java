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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

        // Ưu tiên lấy từ Firebase Auth (luôn có sau khi đăng nhập)
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            userName = firebaseUser.getDisplayName();
            userAvatarUrl = firebaseUser.getPhotoUrl() != null
                    ? firebaseUser.getPhotoUrl().toString() : null;
        }

        // Fallback: đọc từ Fragment args hoặc Activity Intent
        if (TextUtils.isEmpty(userName)) {
            Bundle args = getArguments();
            if (args != null) {
                userName = args.getString("user_name");
                if (userAvatarUrl == null) userAvatarUrl = args.getString("user_avatar");
            }
        }
        if (TextUtils.isEmpty(userName) && requireActivity().getIntent() != null) {
            userName = requireActivity().getIntent().getStringExtra("user_name");
            if (userAvatarUrl == null)
                userAvatarUrl = requireActivity().getIntent().getStringExtra("user_avatar");
        }

        // Fallback cuối cùng
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
                            productImageById.putAll(buildFallbackProductImages());
                            productAdapter.setProductImageMap(productImageById);
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
                        productImageById.putAll(buildFallbackProductImages());
                        productAdapter.setProductImageMap(productImageById);
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
        fallback.add(new Category("fb-cat-1", "Laptop", null));
        fallback.add(new Category("fb-cat-2", "Điện tử", null));
        fallback.add(new Category("fb-cat-3", "Sách", null));
        fallback.add(new Category("fb-cat-4", "Điện thoại", null));
        fallback.add(new Category("fb-cat-5", "Phụ kiện", null));
        fallback.add(new Category("fb-cat-6", "Thể thao", null));
        fallback.add(new Category("fb-cat-7", "Nhà cửa", null));
        fallback.add(new Category("fb-cat-8", "Văn phòng", null));
        return fallback;
    }

    private List<Product> buildFallbackProducts() {
        List<Product> fallback = new ArrayList<>();
        fallback.add(createFallbackProduct("fb-prod-1", "fb-cat-1", "MacBook Pro M1 2020 8GB/256GB", 15_500_000d, "used"));
        fallback.add(createFallbackProduct("fb-prod-2", "fb-cat-3", "Giáo trình Giải tích 1 (Tái bản)", 45_000d, "good"));
        fallback.add(createFallbackProduct("fb-prod-3", "fb-cat-2", "Tai nghe Sony WH-1000XM4", 890_000d, "used"));
        fallback.add(createFallbackProduct("fb-prod-4", "fb-cat-7", "Đèn bàn LED chống cận Xiaomi", 120_000d, "new"));
        fallback.add(createFallbackProduct("fb-prod-5", "fb-cat-5", "Bàn phím cơ Keychron K2 RGB", 650_000d, "used"));
        fallback.add(createFallbackProduct("fb-prod-6", "fb-cat-4", "Samsung Galaxy A54 5G 128GB", 4_200_000d, "used"));
        fallback.add(createFallbackProduct("fb-prod-7", "fb-cat-7", "Bình nước giữ nhiệt 500ml", 95_000d, "new"));
        fallback.add(createFallbackProduct("fb-prod-8", "fb-cat-6", "Vợt cầu lông Yonex Nanoray", 280_000d, "good"));
        return fallback;
    }

    private Map<String, String> buildFallbackProductImages() {
        Map<String, String> images = new HashMap<>();
        images.put("fb-prod-1", "https://picsum.photos/seed/laptop42/400/300");
        images.put("fb-prod-2", "https://picsum.photos/seed/book11/400/300");
        images.put("fb-prod-3", "https://picsum.photos/seed/audio22/400/300");
        images.put("fb-prod-4", "https://picsum.photos/seed/desk33/400/300");
        images.put("fb-prod-5", "https://picsum.photos/seed/keyboard44/400/300");
        images.put("fb-prod-6", "https://picsum.photos/seed/phone55/400/300");
        images.put("fb-prod-7", "https://picsum.photos/seed/bottle66/400/300");
        images.put("fb-prod-8", "https://picsum.photos/seed/sport77/400/300");
        return images;
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
