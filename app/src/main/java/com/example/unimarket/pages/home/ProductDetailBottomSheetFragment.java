package com.example.unimarket.pages.home;

import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.Review;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductDetailBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String ARG_PRODUCT = "product";
    private static final String ARG_IMAGE_URL = "image_url";
    private static final String ARG_CATEGORY_NAME = "category_name";

    private Product product;
    private String imageUrl;
    private String categoryName;

    // UI Components
    private ImageView ivProductDetail;
    private TextView tvProductDetailTitle;
    private TextView tvDetailRating;
    private TextView tvDetailStars;
    private TextView tvDetailReviewCount;
    private TextView tvDetailCondition;
    private TextView tvDetailStatus;
    private TextView tvDetailPrice;
    private TextView tvDetailDescription;
    private TextView tvQuantity;
    private ImageView btnQuantityMinus;
    private ImageView btnQuantityPlus;
    private EditText etDiscountCode;
    private TextView btnApplyCode;
    private RecyclerView rvReviews;
    private TextView tvViewAllReviews;
    private ImageView ivSellerAvatar;
    private TextView tvSellerName;
    private TextView tvSellerRating;
    private TextView btnChat;
    private LinearLayout btnFavorite;
    private LinearLayout btnAddToCart;
    private LinearLayout btnBuyNow;
    private ImageView ivClose;

    private int quantity = 1;
    private ReviewAdapter reviewAdapter;
    private HomeViewModel homeViewModel;

    public static ProductDetailBottomSheetFragment newInstance(Product product, String imageUrl, String categoryName) {
        ProductDetailBottomSheetFragment fragment = new ProductDetailBottomSheetFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PRODUCT, product);
        args.putString(ARG_IMAGE_URL, imageUrl);
        args.putString(ARG_CATEGORY_NAME, categoryName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            product = (Product) getArguments().getSerializable(ARG_PRODUCT);
            imageUrl = getArguments().getString(ARG_IMAGE_URL);
            categoryName = getArguments().getString(ARG_CATEGORY_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_product_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupViewModel();
        bindProductData();
        setupListeners();
        loadReviews();
    }

    private void initViews(View view) {
        ivProductDetail = view.findViewById(R.id.ivProductDetail);
        tvProductDetailTitle = view.findViewById(R.id.tvProductDetailTitle);
        tvDetailRating = view.findViewById(R.id.tvDetailRating);
        tvDetailStars = view.findViewById(R.id.tvDetailStars);
        tvDetailReviewCount = view.findViewById(R.id.tvDetailReviewCount);
        tvDetailCondition = view.findViewById(R.id.tvDetailCondition);
        tvDetailStatus = view.findViewById(R.id.tvDetailStatus);
        tvDetailPrice = view.findViewById(R.id.tvDetailPrice);
        tvDetailDescription = view.findViewById(R.id.tvDetailDescription);
        tvQuantity = view.findViewById(R.id.tvQuantity);
        btnQuantityMinus = view.findViewById(R.id.btnQuantityMinus);
        btnQuantityPlus = view.findViewById(R.id.btnQuantityPlus);
        etDiscountCode = view.findViewById(R.id.etDiscountCode);
        btnApplyCode = view.findViewById(R.id.btnApplyCode);
        rvReviews = view.findViewById(R.id.rvReviews);
        tvViewAllReviews = view.findViewById(R.id.tvViewAllReviews);
        ivSellerAvatar = view.findViewById(R.id.ivSellerAvatar);
        tvSellerName = view.findViewById(R.id.tvSellerName);
        tvSellerRating = view.findViewById(R.id.tvSellerRating);
        btnChat = view.findViewById(R.id.btnChat);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnAddToCart = view.findViewById(R.id.btnAddToCart);
        btnBuyNow = view.findViewById(R.id.btnBuyNow);
        ivClose = view.findViewById(R.id.ivClose);

        // Setup RecyclerView for reviews
        reviewAdapter = new ReviewAdapter();
        rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReviews.setAdapter(reviewAdapter);
    }

    private void setupViewModel() {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
    }

    private void bindProductData() {
        if (product == null) {
            dismiss();
            return;
        }

        // Product Image
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(ivProductDetail);
        }

        // Title
        tvProductDetailTitle.setText(product.getTitle() != null ? product.getTitle() : "Sản phẩm");

        // Price
        tvDetailPrice.setText(HomeUiUtils.formatPrice(product.getPrice()));

        // Condition & Status
        tvDetailCondition.setText(HomeUiUtils.formatConditionAndStatus(product.getCondition(), null));
        tvDetailStatus.setText(product.getStatus() != null ? product.getStatus() : "Còn hàng");

        // Description
        tvDetailDescription.setText(product.getDescription() != null ? product.getDescription() : "");

        // Rating (placeholder - will load from reviews)
        tvDetailRating.setText("4.5");
        tvDetailStars.setText("★★★★☆");
        tvDetailReviewCount.setText("(8 đánh giá)");

        // Seller info (placeholder)
        tvSellerName.setText("Cửa hàng UniMarket");
        tvSellerRating.setText("★★★★★ (4.8) • 250+ bán hàng");
    }

    private void setupListeners() {
        // Close button
        ivClose.setOnClickListener(v -> dismiss());

        // Quantity controls
        btnQuantityMinus.setOnClickListener(v -> updateQuantity(-1));
        btnQuantityPlus.setOnClickListener(v -> updateQuantity(1));

        // Discount code
        btnApplyCode.setOnClickListener(v -> applyDiscountCode());

        // Action buttons
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnAddToCart.setOnClickListener(v -> addToCart());
        btnBuyNow.setOnClickListener(v -> buyNow());

        // Seller chat
        btnChat.setOnClickListener(v -> contactSeller());

        // View all reviews
        tvViewAllReviews.setOnClickListener(v -> viewAllReviews());
    }

    private void loadReviews() {
        // Mock reviews for demo
        List<Review> reviews = new ArrayList<>();

        Review review1 = new Review();
        review1.setId("review_001");
        review1.setReviewer_name("Hoàng Minh");
        review1.setReviewer_avatar("https://i.pravatar.cc/150?img=1");
        review1.setRating(5);
        review1.setTitle("Hàng chất lượng, bao đẹp");
        review1.setContent("Máy đúng như mô tả, pin quả 88%, hôm bao ngay không vấn đề gì. Sẽ mua lại!");
        review1.setCreated_at_timestamp(System.currentTimeMillis() - (4 * 24 * 60 * 60 * 1000)); // 4 days ago
        review1.setHelpful_count(12);
        reviews.add(review1);

        Review review2 = new Review();
        review2.setId("review_002");
        review2.setReviewer_name("Thảo Nguyên");
        review2.setReviewer_avatar("https://i.pravatar.cc/150?img=2");
        review2.setRating(4);
        review2.setTitle("Tốt nhưng pin hơi kém kỳ vọng");
        review2.setContent("Giá rẻ so với thị trường, máy hoạt động ok. Chỉ tắc pin không được như quảng cáo, 85% là tối đa.");
        review2.setCreated_at_timestamp(System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000)); // 5 days ago
        review2.setHelpful_count(8);
        reviews.add(review2);

        reviewAdapter.submitList(reviews);
    }

    private void updateQuantity(int change) {
        quantity += change;
        if (quantity < 1) quantity = 1;
        tvQuantity.setText(String.valueOf(quantity));
    }

    private void applyDiscountCode() {
        String code = etDiscountCode.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(requireContext(), "Vui lòng nhập mã giảm giá", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mock discount validation
        if (code.equals("STUDENT50")) {
            Toast.makeText(requireContext(), "Áp dụng thành công! Tiết kiệm 50.000đ", Toast.LENGTH_SHORT).show();
            etDiscountCode.setEnabled(false);
            btnApplyCode.setText("Đã áp dụng");
        } else if (code.equals("WELCOME100")) {
            Toast.makeText(requireContext(), "Áp dụng thành công! Tiết kiệm 100.000đ", Toast.LENGTH_SHORT).show();
            etDiscountCode.setEnabled(false);
            btnApplyCode.setText("Đã áp dụng");
        } else {
            Toast.makeText(requireContext(), "Mã không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFavorite() {
        Toast.makeText(requireContext(), "Đã lưu sản phẩm", Toast.LENGTH_SHORT).show();
    }

    private void addToCart() {
        Toast.makeText(requireContext(), "Đã thêm " + quantity + " sản phẩm vào giỏ hàng", Toast.LENGTH_SHORT).show();
    }

    private void buyNow() {
        Toast.makeText(requireContext(), "Chuyển đến thanh toán với " + quantity + " sản phẩm", Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private void contactSeller() {
        Toast.makeText(requireContext(), "Mở chat với người bán", Toast.LENGTH_SHORT).show();
    }

    private void viewAllReviews() {
        Toast.makeText(requireContext(), "Xem tất cả đánh giá", Toast.LENGTH_SHORT).show();
    }
}
