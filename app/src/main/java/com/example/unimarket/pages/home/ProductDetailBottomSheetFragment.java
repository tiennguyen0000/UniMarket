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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.Review;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.ReviewService;
import com.example.unimarket.data.service.UserService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ProductDetailBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String ARG_PRODUCT      = "product";
    private static final String ARG_IMAGE_URL    = "image_url";
    private static final String ARG_CATEGORY_NAME = "category_name";

    private Product product;
    private String imageUrl;
    private String categoryName;

    // UI
    private ImageView ivProductDetail, ivSellerAvatar, ivClose;
    private TextView tvProductDetailTitle, tvDetailRating, tvDetailStars, tvDetailReviewCount;
    private TextView tvDetailCondition, tvDetailStatus, tvDetailPrice, tvDetailDescription;
    private TextView tvQuantity, btnApplyCode, btnChat, tvViewAllReviews;
    private ImageView btnQuantityMinus, btnQuantityPlus;
    private EditText etDiscountCode;
    private RecyclerView rvReviews;
    private TextView tvSellerName, tvSellerRating;
    private LinearLayout btnFavorite, btnAddToCart, btnBuyNow;

    private int quantity = 1;
    private ReviewAdapter reviewAdapter;
    private boolean discountApplied = false;
    private double discountAmount = 0;

    // Services
    private final UserService userService = new UserService();
    private final ReviewService reviewService = new ReviewService();
    private final OrderService orderService = new OrderService();

    public static ProductDetailBottomSheetFragment newInstance(Product product, String imageUrl, String categoryName) {
        ProductDetailBottomSheetFragment f = new ProductDetailBottomSheetFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PRODUCT, product);
        args.putString(ARG_IMAGE_URL, imageUrl);
        args.putString(ARG_CATEGORY_NAME, categoryName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            product      = (Product) getArguments().getSerializable(ARG_PRODUCT);
            imageUrl     = getArguments().getString(ARG_IMAGE_URL);
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
        bindProductData();
        setupListeners();
        loadSellerInfo();
        loadReviews();
    }

    private void initViews(View view) {
        ivProductDetail      = view.findViewById(R.id.ivProductDetail);
        tvProductDetailTitle = view.findViewById(R.id.tvProductDetailTitle);
        tvDetailRating       = view.findViewById(R.id.tvDetailRating);
        tvDetailStars        = view.findViewById(R.id.tvDetailStars);
        tvDetailReviewCount  = view.findViewById(R.id.tvDetailReviewCount);
        tvDetailCondition    = view.findViewById(R.id.tvDetailCondition);
        tvDetailStatus       = view.findViewById(R.id.tvDetailStatus);
        tvDetailPrice        = view.findViewById(R.id.tvDetailPrice);
        tvDetailDescription  = view.findViewById(R.id.tvDetailDescription);
        tvQuantity           = view.findViewById(R.id.tvQuantity);
        btnQuantityMinus     = view.findViewById(R.id.btnQuantityMinus);
        btnQuantityPlus      = view.findViewById(R.id.btnQuantityPlus);
        etDiscountCode       = view.findViewById(R.id.etDiscountCode);
        btnApplyCode         = view.findViewById(R.id.btnApplyCode);
        rvReviews            = view.findViewById(R.id.rvReviews);
        tvViewAllReviews     = view.findViewById(R.id.tvViewAllReviews);
        ivSellerAvatar       = view.findViewById(R.id.ivSellerAvatar);
        tvSellerName         = view.findViewById(R.id.tvSellerName);
        tvSellerRating       = view.findViewById(R.id.tvSellerRating);
        btnChat              = view.findViewById(R.id.btnChat);
        btnFavorite          = view.findViewById(R.id.btnFavorite);
        btnAddToCart         = view.findViewById(R.id.btnAddToCart);
        btnBuyNow            = view.findViewById(R.id.btnBuyNow);
        ivClose              = view.findViewById(R.id.ivClose);

        reviewAdapter = new ReviewAdapter();
        rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReviews.setAdapter(reviewAdapter);
        rvReviews.setNestedScrollingEnabled(false);
    }

    private void bindProductData() {
        if (product == null) { dismiss(); return; }

        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_light_grey_rounded)
                    .centerCrop()
                    .into(ivProductDetail);
        }

        tvProductDetailTitle.setText(product.getTitle() != null ? product.getTitle() : "Sản phẩm");
        tvDetailPrice.setText(HomeUiUtils.formatPrice(product.getPrice()));
        tvDetailCondition.setText(HomeUiUtils.formatConditionAndStatus(product.getCondition(), null));
        tvDetailStatus.setText(product.getStatus() != null ? product.getStatus() : "Còn hàng");
        tvDetailDescription.setText(!TextUtils.isEmpty(product.getDescription())
                ? product.getDescription() : "Chưa có mô tả.");

        // Rating placeholder - sẽ tính lại sau khi reviews load về
        tvDetailRating.setText("—");
        tvDetailStars.setText("★★★★★");
        tvDetailReviewCount.setText("(đang tải...)");
    }

    private void loadSellerInfo() {
        if (product == null || TextUtils.isEmpty(product.getSeller_id())) return;

        userService.fetchById(product.getSeller_id(), result -> {
            if (!isAdded()) return;
            if (result.isSuccess() && result.getData() != null) {
                User seller = result.getData();
                tvSellerName.setText(!TextUtils.isEmpty(seller.getFull_name())
                        ? seller.getFull_name() : "UniMarket Seller");

                if (!TextUtils.isEmpty(seller.getAvatar_url())) {
                    Glide.with(this)
                            .load(seller.getAvatar_url())
                            .circleCrop()
                            .placeholder(R.drawable.ic_user_placeholder)
                            .into(ivSellerAvatar);
                }
                tvSellerRating.setText(!TextUtils.isEmpty(seller.getUniversity())
                        ? seller.getUniversity() : "Sinh viên UniMarket");
            } else {
                tvSellerName.setText("UniMarket Seller");
            }
        });
    }

    private void loadReviews() {
        if (product == null || TextUtils.isEmpty(product.getId())) return;

        reviewService.getReviewsByProductId(product.getId(), new com.example.unimarket.data.service.base.AsyncCrudService.ListCallback<Review>() {
            @Override
            public void onSuccess(List<Review> data) {
                if (!isAdded()) return;
                if (data != null && !data.isEmpty()) {
                    updateRatingInfo(data);
                    // Hiển thị tối đa 2 review
                    reviewAdapter.submitList(data.size() > 2 ? data.subList(0, 2) : data);
                } else {
                    reviewAdapter.submitList(new ArrayList<>());
                    tvDetailReviewCount.setText("(chưa có đánh giá)");
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                tvDetailReviewCount.setText("(chưa có đánh giá)");
            }
        });
    }

    private void updateRatingInfo(List<Review> reviews) {
        double total = 0;
        for (Review r : reviews) {
            if (r.getRating() != null) total += r.getRating();
        }
        double avg = total / reviews.size();
        tvDetailRating.setText(String.format(Locale.getDefault(), "%.1f", avg));
        tvDetailStars.setText(starsFor(avg));
        tvDetailReviewCount.setText("(" + reviews.size() + " đánh giá)");
    }

    private String starsFor(double avg) {
        int full = (int) Math.round(avg);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < full ? "★" : "☆");
        return sb.toString();
    }

    private void setupListeners() {
        ivClose.setOnClickListener(v -> dismiss());
        btnQuantityMinus.setOnClickListener(v -> updateQuantity(-1));
        btnQuantityPlus.setOnClickListener(v -> updateQuantity(1));
        btnApplyCode.setOnClickListener(v -> applyDiscountCode());
        btnFavorite.setOnClickListener(v -> Toast.makeText(requireContext(), "Đã lưu sản phẩm", Toast.LENGTH_SHORT).show());
        btnAddToCart.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Đã thêm " + quantity + " sản phẩm vào giỏ", Toast.LENGTH_SHORT).show());
        btnBuyNow.setOnClickListener(v -> buyNow());
        btnChat.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mở chat với người bán", Toast.LENGTH_SHORT).show());
        tvViewAllReviews.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Xem tất cả đánh giá", Toast.LENGTH_SHORT).show());
    }

    private void updateQuantity(int change) {
        quantity = Math.max(1, quantity + change);
        tvQuantity.setText(String.valueOf(quantity));
    }

    private void applyDiscountCode() {
        if (discountApplied) return;
        String code = etDiscountCode.getText().toString().trim().toUpperCase();
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(requireContext(), "Vui lòng nhập mã giảm giá", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (code) {
            case "STUDENT50":
                discountAmount = 50_000;
                discountApplied = true;
                Toast.makeText(requireContext(), "Áp dụng thành công! -50.000đ", Toast.LENGTH_SHORT).show();
                etDiscountCode.setEnabled(false);
                btnApplyCode.setText("Đã áp dụng");
                break;
            case "WELCOME100":
                discountAmount = 100_000;
                discountApplied = true;
                Toast.makeText(requireContext(), "Áp dụng thành công! -100.000đ", Toast.LENGTH_SHORT).show();
                etDiscountCode.setEnabled(false);
                btnApplyCode.setText("Đã áp dụng");
                break;
            default:
                Toast.makeText(requireContext(), "Mã không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Order Flow ──────────────────────────────────────────────────────────

    private void buyNow() {
        String buyerId = FirebaseAuth.getInstance().getUid();
        if (buyerId == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để mua hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (product == null) return;

        double unitPrice = product.getPrice() != null ? product.getPrice() : 0;
        double totalPrice = unitPrice * quantity - discountAmount;
        if (totalPrice < 0) totalPrice = 0;

        Order order = new Order();
        order.setBuyer_id(buyerId);
        order.setSeller_id(product.getSeller_id());
        order.setProduct_id(product.getId());
        order.setProduct_title(product.getTitle());
        // Ảnh đại diện sản phẩm
        if (!TextUtils.isEmpty(imageUrl)) {
            order.setProduct_image_url(imageUrl);
        } else if (product.getImage_urls() != null && !product.getImage_urls().isEmpty()) {
            order.setProduct_image_url(product.getImage_urls().get(0));
        }
        order.setTotal_price(totalPrice);
        order.setStatus("pending");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String now = format.format(new Date());
        order.setCreated_at(now);
        order.setUpdated_at(now);

        // Vô hiệu nút trong khi tạo order
        btnBuyNow.setEnabled(false);
        orderService.save(order, result -> {
            if (!isAdded()) return;
            btnBuyNow.setEnabled(true);
            if (result.isSuccess()) {
                Toast.makeText(requireContext(),
                        "Đặt hàng thành công! Mã đơn: #" + (result.getData() != null
                                ? result.getData().getId().substring(0, 6).toUpperCase()
                                : ""),
                        Toast.LENGTH_LONG).show();
                dismiss();
            } else {
                Toast.makeText(requireContext(), "Đặt hàng thất bại: " + result.getError(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
