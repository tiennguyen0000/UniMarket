package com.example.unimarket.pages.home;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Notification;
import com.example.unimarket.data.model.Order;
import com.example.unimarket.data.model.OrderItem;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.Review;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.model.Wishlist;
import com.example.unimarket.data.service.NotificationService;
import com.example.unimarket.data.service.OrderItemService;
import com.example.unimarket.data.service.OrderService;
import com.example.unimarket.data.service.ReviewService;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.WishlistService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.pages.chat.ChatBottomSheetFragment;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ProductDetailBottomSheetFragment extends BottomSheetDialogFragment {
    private static final String ARG_PRODUCT = "product";
    private static final String ARG_IMAGE_URL = "image_url";
    private static final String ARG_CATEGORY_NAME = "category_name";

    private Product product;
    private String imageUrl;
    private String categoryName;

    private ImageView ivProductDetail, ivSellerAvatar, ivClose;
    private TextView tvProductDetailTitle, tvDetailRating, tvDetailStars, tvDetailReviewCount;
    private TextView tvDetailCondition, tvDetailStatus, tvDetailPrice, tvDetailDescription;
    private TextView tvDetailCategory, tvQuantity, btnQuantityMinus, btnQuantityPlus;
    private TextView btnApplyCode, btnChat, tvViewAllReviews, tvDiscountStatus, tvBottomTotal;
    private TextView tvSellerName, tvSellerRating, tvFavoriteIcon, tvFavoriteLabel;
    private TextView tvAddToCartLabel, tvBuyNowLabel;
    private TextView btnSubmitReview, tvSelectedReviewRating, tvReviewFormStatus;
    private TextView btnReviewStar1, btnReviewStar2, btnReviewStar3, btnReviewStar4, btnReviewStar5;
    private EditText etDiscountCode, etReviewComment;
    private RecyclerView rvReviews;
    private LinearLayout layoutReviewEmpty;
    private LinearLayout btnFavorite, btnAddToCart, btnBuyNow;

    private int quantity = 1;
    private boolean discountApplied = false;
    private String appliedDiscountCode;
    private double discountAmount = 0;
    private boolean favoriteSaved = false;
    private Wishlist currentWishlist;
    private int selectedReviewRating = 5;
    private boolean submittingReview = false;

    private ReviewAdapter reviewAdapter;
    private final List<Review> loadedReviews = new ArrayList<>();

    private final UserService userService = new UserService();
    private final ReviewService reviewService = new ReviewService();
    private final OrderService orderService = new OrderService();
    private final OrderItemService orderItemService = new OrderItemService();
    private final NotificationService notificationService = new NotificationService();
    private final WishlistService wishlistService = new WishlistService();

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
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        bottomSheet.setLayoutParams(params);
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        bindProductData();
        setupListeners();
        loadSellerInfo();
        loadReviews();
        loadWishlistState();
    }

    private void initViews(View view) {
        ivProductDetail = view.findViewById(R.id.ivProductDetail);
        tvProductDetailTitle = view.findViewById(R.id.tvProductDetailTitle);
        tvDetailCategory = view.findViewById(R.id.tvDetailCategory);
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
        tvDiscountStatus = view.findViewById(R.id.tvDiscountStatus);
        rvReviews = view.findViewById(R.id.rvReviews);
        layoutReviewEmpty = view.findViewById(R.id.layoutReviewEmpty);
        tvViewAllReviews = view.findViewById(R.id.tvViewAllReviews);
        btnReviewStar1 = view.findViewById(R.id.btnReviewStar1);
        btnReviewStar2 = view.findViewById(R.id.btnReviewStar2);
        btnReviewStar3 = view.findViewById(R.id.btnReviewStar3);
        btnReviewStar4 = view.findViewById(R.id.btnReviewStar4);
        btnReviewStar5 = view.findViewById(R.id.btnReviewStar5);
        tvSelectedReviewRating = view.findViewById(R.id.tvSelectedReviewRating);
        etReviewComment = view.findViewById(R.id.etReviewComment);
        tvReviewFormStatus = view.findViewById(R.id.tvReviewFormStatus);
        btnSubmitReview = view.findViewById(R.id.btnSubmitReview);
        ivSellerAvatar = view.findViewById(R.id.ivSellerAvatar);
        tvSellerName = view.findViewById(R.id.tvSellerName);
        tvSellerRating = view.findViewById(R.id.tvSellerRating);
        btnChat = view.findViewById(R.id.btnChat);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        tvFavoriteIcon = view.findViewById(R.id.tvFavoriteIcon);
        tvFavoriteLabel = view.findViewById(R.id.tvFavoriteLabel);
        btnAddToCart = view.findViewById(R.id.btnAddToCart);
        btnBuyNow = view.findViewById(R.id.btnBuyNow);
        tvAddToCartLabel = view.findViewById(R.id.tvAddToCartLabel);
        tvBuyNowLabel = view.findViewById(R.id.tvBuyNowLabel);
        tvBottomTotal = view.findViewById(R.id.tvBottomTotal);
        ivClose = view.findViewById(R.id.ivClose);

        reviewAdapter = new ReviewAdapter();
        rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReviews.setAdapter(reviewAdapter);
        rvReviews.setNestedScrollingEnabled(false);
    }

    private void bindProductData() {
        if (product == null) {
            dismissAllowingStateLoss();
            return;
        }

        String resolvedImage = !TextUtils.isEmpty(imageUrl) ? imageUrl : firstProductImage();
        if (!TextUtils.isEmpty(resolvedImage)) {
            Glide.with(this)
                    .load(resolvedImage)
                    .placeholder(R.drawable.bg_light_grey_rounded)
                    .error(R.drawable.ic_user_placeholder)
                    .centerCrop()
                    .into(ivProductDetail);
        } else {
            ivProductDetail.setImageResource(HomeUiUtils.iconResForCategoryName(categoryName));
        }

        tvProductDetailTitle.setText(!TextUtils.isEmpty(product.getTitle()) ? product.getTitle() : "Sản phẩm");
        tvDetailPrice.setText(HomeUiUtils.formatPrice(product.getPrice()));
        tvDetailCategory.setText(!TextUtils.isEmpty(categoryName) ? categoryName : "UniMarket");
        tvDetailCondition.setText(HomeUiUtils.formatConditionAndStatus(product.getCondition(), null));
        tvDetailStatus.setText(statusLabel(product.getStatus()));
        tvDetailDescription.setText(!TextUtils.isEmpty(product.getDescription())
                ? product.getDescription() : "Người bán chưa thêm mô tả cho sản phẩm này.");
        tvDiscountStatus.setVisibility(View.GONE);
        updateRatingEmptyState();
        updateTotalPrice();
        updateOwnProductState();
    }

    private void setupListeners() {
        ivClose.setOnClickListener(v -> dismiss());
        btnQuantityMinus.setOnClickListener(v -> updateQuantity(-1));
        btnQuantityPlus.setOnClickListener(v -> updateQuantity(1));
        btnApplyCode.setOnClickListener(v -> applyDiscountCode());
        btnFavorite.setOnClickListener(v -> toggleWishlist());
        btnAddToCart.setOnClickListener(v -> addToCart());
        btnBuyNow.setOnClickListener(v -> buyNow());
        btnChat.setOnClickListener(v -> openSellerChat());
        tvViewAllReviews.setOnClickListener(v -> showAllReviewsDialog());
        setupReviewForm();
    }

    private void setupReviewForm() {
        setReviewRating(5);
        if (btnReviewStar1 != null) btnReviewStar1.setOnClickListener(v -> setReviewRating(1));
        if (btnReviewStar2 != null) btnReviewStar2.setOnClickListener(v -> setReviewRating(2));
        if (btnReviewStar3 != null) btnReviewStar3.setOnClickListener(v -> setReviewRating(3));
        if (btnReviewStar4 != null) btnReviewStar4.setOnClickListener(v -> setReviewRating(4));
        if (btnReviewStar5 != null) btnReviewStar5.setOnClickListener(v -> setReviewRating(5));
        if (btnSubmitReview != null) btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void setReviewRating(int rating) {
        selectedReviewRating = Math.max(1, Math.min(5, rating));
        TextView[] stars = { btnReviewStar1, btnReviewStar2, btnReviewStar3, btnReviewStar4, btnReviewStar5 };
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] == null) continue;
            stars[i].setText(i < selectedReviewRating ? "★" : "☆");
            stars[i].setTextColor(i < selectedReviewRating ? 0xFFFDB022 : 0xFFD0D5DD);
        }
        if (tvSelectedReviewRating != null) {
            tvSelectedReviewRating.setText(selectedReviewRating + " sao");
        }
    }

    private void loadSellerInfo() {
        if (product == null || TextUtils.isEmpty(product.getSeller_id())) {
            tvSellerName.setText("Người bán UniMarket");
            return;
        }

        userService.fetchById(product.getSeller_id(), result -> {
            if (!isAdded()) return;
            if (result.isSuccess() && result.getData() != null) {
                User seller = result.getData();
                tvSellerName.setText(!TextUtils.isEmpty(seller.getFull_name())
                        ? seller.getFull_name() : "Người bán UniMarket");
                tvSellerRating.setText(!TextUtils.isEmpty(seller.getUniversity())
                        ? seller.getUniversity() : "Sinh viên UniMarket");

                if (!TextUtils.isEmpty(seller.getAvatar_url())) {
                    Glide.with(this)
                            .load(seller.getAvatar_url())
                            .circleCrop()
                            .placeholder(R.drawable.ic_user_placeholder)
                            .error(R.drawable.ic_user_placeholder)
                            .into(ivSellerAvatar);
                }
            } else {
                tvSellerName.setText("Người bán UniMarket");
                tvSellerRating.setText("Sinh viên UniMarket");
            }
        });
    }

    private void loadReviews() {
        if (product == null || TextUtils.isEmpty(product.getId())) {
            updateRatingEmptyState();
            return;
        }

        reviewService.getReviewsByProductId(product.getId(), new AsyncCrudService.ListCallback<Review>() {
            @Override
            public void onSuccess(List<Review> data) {
                if (!isAdded()) return;
                loadedReviews.clear();
                if (data != null) {
                    loadedReviews.addAll(data);
                }

                if (loadedReviews.isEmpty()) {
                    reviewAdapter.submitList(new ArrayList<>());
                    rvReviews.setVisibility(View.GONE);
                    layoutReviewEmpty.setVisibility(View.VISIBLE);
                    updateRatingEmptyState();
                    return;
                }

                layoutReviewEmpty.setVisibility(View.GONE);
                rvReviews.setVisibility(View.VISIBLE);
                updateRatingInfo(loadedReviews);
                int max = Math.min(2, loadedReviews.size());
                reviewAdapter.submitList(new ArrayList<>(loadedReviews.subList(0, max)));
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                loadedReviews.clear();
                reviewAdapter.submitList(new ArrayList<>());
                rvReviews.setVisibility(View.GONE);
                layoutReviewEmpty.setVisibility(View.VISIBLE);
                updateRatingEmptyState();
            }
        });
    }

    private void loadWishlistState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || product == null || TextUtils.isEmpty(product.getId())) {
            updateFavoriteUi(false);
            return;
        }

        wishlistService.getWithFilter("user_id", user.getUid(), new AsyncCrudService.ListCallback<Wishlist>() {
            @Override
            public void onSuccess(List<Wishlist> data) {
                if (!isAdded()) return;
                currentWishlist = findWishlistForProduct(data);
                updateFavoriteUi(currentWishlist != null);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                updateFavoriteUi(false);
            }
        });
    }

    private Wishlist findWishlistForProduct(List<Wishlist> data) {
        if (data == null || product == null) {
            return null;
        }
        for (Wishlist item : data) {
            if (item != null && product.getId().equals(item.getProduct_id())) {
                return item;
            }
        }
        return null;
    }

    private void submitReview() {
        if (submittingReview) {
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showReviewStatus("Vui lòng đăng nhập để gửi đánh giá.", true);
            return;
        }
        if (product == null || TextUtils.isEmpty(product.getId())) {
            showReviewStatus("Không thể đánh giá sản phẩm này.", true);
            return;
        }
        if (user.getUid().equals(product.getSeller_id())) {
            showReviewStatus("Bạn không thể tự đánh giá tin của mình.", true);
            return;
        }

        String content = etReviewComment != null && etReviewComment.getText() != null
                ? etReviewComment.getText().toString().trim() : "";
        if (TextUtils.isEmpty(content)) {
            if (etReviewComment != null) {
                etReviewComment.setError("Vui lòng nhập nội dung đánh giá");
            }
            showReviewStatus("Hãy viết vài dòng cảm nhận trước khi gửi.", true);
            return;
        }
        if (content.length() < 8) {
            if (etReviewComment != null) {
                etReviewComment.setError("Đánh giá nên có ít nhất 8 ký tự");
            }
            showReviewStatus("Đánh giá hơi ngắn, bạn viết thêm một chút nhé.", true);
            return;
        }

        setReviewSubmitting(true);
        orderService.getOrdersByBuyerId(user.getUid(), new AsyncCrudService.ListCallback<Order>() {
            @Override
            public void onSuccess(List<Order> data) {
                if (!isAdded()) return;
                if (hasCompletedOrder(data)) {
                    saveReview(user, content);
                } else {
                    setReviewSubmitting(false);
                    showReviewStatus("Bạn chỉ có thể đánh giá sau khi đơn hàng hoàn thành.", true);
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                setReviewSubmitting(false);
                showReviewStatus("Không thể kiểm tra đơn hàng: " + error, true);
            }
        });
    }

    private boolean hasCompletedOrder(List<Order> orders) {
        if (orders == null || product == null) {
            return false;
        }
        for (Order order : orders) {
            if (order != null
                    && product.getId().equals(order.getProduct_id())
                    && "done".equalsIgnoreCase(order.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private void saveReview(FirebaseUser user, String content) {
        Review review = new Review();
        review.setId(stableDocId("review", product.getId(), user.getUid()));
        review.setProduct_id(product.getId());
        review.setSeller_id(product.getSeller_id());
        review.setReviewer_id(user.getUid());
        review.setReviewer_name(resolveReviewerName(user));
        review.setReviewer_avatar(user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        review.setRating(selectedReviewRating);
        review.setTitle("Đánh giá từ người mua");
        review.setContent(content);
        review.setCreated_at_timestamp(System.currentTimeMillis());
        review.setHelpful_count(0);

        reviewService.save(review, result -> {
            if (!isAdded()) return;
            setReviewSubmitting(false);
            if (result.isSuccess()) {
                if (etReviewComment != null) {
                    etReviewComment.setText("");
                    etReviewComment.setError(null);
                }
                showReviewStatus("Đã gửi đánh giá. Cảm ơn bạn đã chia sẻ!", false);
                loadReviews();
            } else {
                showReviewStatus("Gửi đánh giá thất bại: " + result.getError(), true);
            }
        });
    }

    private String resolveReviewerName(FirebaseUser user) {
        if (user == null) {
            return "Người mua UniMarket";
        }
        if (!TextUtils.isEmpty(user.getDisplayName())) {
            return user.getDisplayName();
        }
        if (!TextUtils.isEmpty(user.getEmail())) {
            String email = user.getEmail();
            int atIndex = email.indexOf('@');
            return atIndex > 0 ? email.substring(0, atIndex) : email;
        }
        return "Người mua UniMarket";
    }

    private void setReviewSubmitting(boolean loading) {
        submittingReview = loading;
        if (btnSubmitReview != null) {
            btnSubmitReview.setEnabled(!loading);
            btnSubmitReview.setAlpha(loading ? 0.65f : 1f);
            btnSubmitReview.setText(loading ? "Đang gửi..." : "Gửi đánh giá");
        }
    }

    private void showReviewStatus(String message, boolean error) {
        if (tvReviewFormStatus == null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            return;
        }
        tvReviewFormStatus.setText(message);
        tvReviewFormStatus.setTextColor(error ? 0xFFB42318 : 0xFF027A48);
    }

    private void updateRatingEmptyState() {
        tvDetailStars.setText("☆☆☆☆☆");
        tvDetailRating.setText("--");
        tvDetailReviewCount.setText("(chưa có đánh giá)");
    }

    private void updateRatingInfo(List<Review> reviews) {
        double total = 0;
        int count = 0;
        for (Review review : reviews) {
            if (review != null && review.getRating() != null) {
                total += review.getRating();
                count++;
            }
        }
        if (count <= 0) {
            updateRatingEmptyState();
            return;
        }
        double avg = total / count;
        tvDetailRating.setText(String.format(Locale.getDefault(), "%.1f", avg));
        tvDetailStars.setText(starsFor(avg));
        tvDetailReviewCount.setText("(" + count + " đánh giá)");
    }

    private String starsFor(double avg) {
        int full = (int) Math.round(avg);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < full ? "★" : "☆");
        }
        return sb.toString();
    }

    private void updateQuantity(int change) {
        quantity = Math.max(1, quantity + change);
        tvQuantity.setText(String.valueOf(quantity));
        updateTotalPrice();
    }

    private void applyDiscountCode() {
        if (discountApplied) {
            showDiscountStatus("Mã giảm giá đã được áp dụng.", false);
            return;
        }

        String code = etDiscountCode.getText() != null
                ? etDiscountCode.getText().toString().trim().toUpperCase(Locale.ROOT) : "";
        if (TextUtils.isEmpty(code)) {
            showDiscountStatus("Vui lòng nhập mã giảm giá.", true);
            return;
        }

        double subtotal = subtotal();
        double requestedDiscount;
        switch (code) {
            case "STUDENT50":
                requestedDiscount = 50_000d;
                break;
            case "WELCOME100":
                requestedDiscount = 100_000d;
                break;
            default:
                showDiscountStatus("Mã không hợp lệ hoặc đã hết hạn.", true);
                return;
        }

        discountAmount = Math.min(requestedDiscount, subtotal);
        discountApplied = true;
        appliedDiscountCode = code;
        etDiscountCode.setEnabled(false);
        btnApplyCode.setText("Đã áp dụng");
        showDiscountStatus("Đã giảm " + HomeUiUtils.formatPrice(discountAmount) + " cho đơn này.", false);
        updateTotalPrice();
    }

    private void showDiscountStatus(String message, boolean error) {
        tvDiscountStatus.setVisibility(View.VISIBLE);
        tvDiscountStatus.setText(message);
        tvDiscountStatus.setTextColor(error ? 0xFFB42318 : 0xFF027A48);
    }

    private void updateTotalPrice() {
        tvBottomTotal.setText(HomeUiUtils.formatPrice(totalPrice()));
    }

    private double subtotal() {
        double unitPrice = product != null && product.getPrice() != null ? product.getPrice() : 0;
        return unitPrice * quantity;
    }

    private double totalPrice() {
        return Math.max(0, subtotal() - discountAmount);
    }

    private void toggleWishlist() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để lưu sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }
        if (product == null || TextUtils.isEmpty(product.getId())) {
            Toast.makeText(requireContext(), "Không thể lưu sản phẩm này", Toast.LENGTH_SHORT).show();
            return;
        }

        setFavoriteLoading(true);
        if (favoriteSaved && currentWishlist != null && !TextUtils.isEmpty(currentWishlist.getId())) {
            wishlistService.deleteById(currentWishlist.getId(), new AsyncCrudService.BooleanCallback() {
                @Override
                public void onSuccess(boolean success) {
                    if (!isAdded()) return;
                    currentWishlist = null;
                    updateFavoriteUi(false);
                    setFavoriteLoading(false);
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    setFavoriteLoading(false);
                    Toast.makeText(requireContext(), "Bỏ lưu thất bại: " + error, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setId(stableDocId("wishlist", user.getUid(), product.getId()));
        wishlist.setUser_id(user.getUid());
        wishlist.setProduct_id(product.getId());
        wishlist.setCreated_at(nowIsoUtc());
        wishlistService.save(wishlist, result -> {
            if (!isAdded()) return;
            setFavoriteLoading(false);
            if (result.isSuccess()) {
                currentWishlist = result.getData();
                updateFavoriteUi(true);
            } else {
                Toast.makeText(requireContext(), "Lưu thất bại: " + result.getError(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addToCart() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để thêm vào giỏ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canPurchase(user.getUid())) {
            return;
        }

        setActionLoading(btnAddToCart, tvAddToCartLabel, true, "Đang thêm...");
        new CartFlow().add(product, quantity, new CartFlow.Callback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                setActionLoading(btnAddToCart, tvAddToCartLabel, false, "Thêm vào giỏ");
                dismiss();
                CartBottomSheetFragment.newInstance()
                        .show(requireActivity().getSupportFragmentManager(), "cart");
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                setActionLoading(btnAddToCart, tvAddToCartLabel, false, "Thêm vào giỏ");
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buyNow() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để mua hàng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canPurchase(user.getUid())) {
            return;
        }

        Order order = buildOrder(user.getUid());
        setActionLoading(btnBuyNow, tvBuyNowLabel, true, "Đang đặt...");
        orderService.save(order, result -> {
            if (!isAdded()) return;
            if (!result.isSuccess() || result.getData() == null) {
                setActionLoading(btnBuyNow, tvBuyNowLabel, false, "Mua ngay");
                Toast.makeText(requireContext(), "Đặt hàng thất bại: " + result.getError(), Toast.LENGTH_SHORT).show();
                return;
            }

            Order savedOrder = result.getData();
            saveOrderItem(savedOrder);
        });
    }

    private Order buildOrder(String buyerId) {
        double unitPrice = product.getPrice() != null ? product.getPrice() : 0;
        Order order = new Order();
        order.setBuyer_id(buyerId);
        order.setSeller_id(product.getSeller_id());
        order.setProduct_id(product.getId());
        order.setProduct_title(product.getTitle());
        order.setProduct_image_url(!TextUtils.isEmpty(imageUrl) ? imageUrl : firstProductImage());
        order.setQuantity(quantity);
        order.setUnit_price(unitPrice);
        order.setDiscount_code(appliedDiscountCode);
        order.setDiscount_amount(discountAmount);
        order.setTotal_price(totalPrice());
        order.setStatus("pending");
        String now = nowIsoUtc();
        order.setCreated_at(now);
        order.setUpdated_at(now);
        return order;
    }

    private void saveOrderItem(Order order) {
        OrderItem item = new OrderItem();
        item.setId(stableDocId("order_item", order.getId(), product.getId()));
        item.setOrder_id(order.getId());
        item.setProduct_id(product.getId());
        item.setSeller_id(product.getSeller_id());
        item.setPrice(product.getPrice() != null ? product.getPrice() : 0);
        item.setQuantity(quantity);

        orderItemService.save(item, result -> {
            if (!isAdded()) return;
            setActionLoading(btnBuyNow, tvBuyNowLabel, false, "Mua ngay");
            if (!result.isSuccess()) {
                orderService.deleteById(order.getId(), new AsyncCrudService.BooleanCallback() {
                    @Override public void onSuccess(boolean success) {}
                    @Override public void onError(String error) {}
                });
                Toast.makeText(requireContext(), "Không thể tạo đơn hàng: " + result.getError(), Toast.LENGTH_SHORT).show();
                return;
            }
            notifySellerNewOrder(order);
            showOrderSuccessDialog(order);
        });
    }

    private void notifySellerNewOrder(Order order) {
        if (order == null || TextUtils.isEmpty(order.getSeller_id()) || TextUtils.isEmpty(order.getId())) {
            return;
        }
        FirebaseUser buyer = FirebaseAuth.getInstance().getCurrentUser();
        if (buyer != null && order.getSeller_id().equals(buyer.getUid())) {
            return;
        }

        Notification notification = new Notification();
        notification.setId(stableDocId("notif", order.getSeller_id(), order.getId(), "new_order"));
        notification.setUser_id(order.getSeller_id());
        notification.setTitle("Có đơn hàng mới");
        notification.setContent("Người mua vừa đặt \"" + safeTitle(order) + "\". Vào Đơn hàng để xác nhận.");
        notification.setType("order");
        notification.setTarget_id(order.getId());
        notification.setIs_read(false);
        notification.setCreated_at(nowIsoUtc());
        notificationService.save(notification, ignored -> {});
    }

    private void showOrderSuccessDialog(Order order) {
        String orderId = order.getId() != null
                ? "#UM" + order.getId().substring(0, Math.min(6, order.getId().length())).toUpperCase(Locale.ROOT)
                : "#UM";
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Đặt hàng thành công")
                .setMessage("Đơn " + orderId + " đã được tạo với tổng " + HomeUiUtils.formatPrice(order.getTotal_price())
                        + ". Bạn có thể theo dõi trạng thái trong tab Đơn hàng.")
                .setNegativeButton("Tiếp tục xem", null)
                .setPositiveButton("Xem đơn hàng", (dialog, which) -> navigateToOrders())
                .show();
    }

    private void showAllReviewsDialog() {
        if (loadedReviews.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Đánh giá")
                    .setMessage("Sản phẩm này chưa có đánh giá từ người mua.")
                    .setPositiveButton("Đóng", null)
                    .show();
            return;
        }

        RecyclerView recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        ReviewAdapter adapter = new ReviewAdapter();
        adapter.submitList(new ArrayList<>(loadedReviews));
        recyclerView.setAdapter(adapter);
        int padding = dpToPx(8);
        recyclerView.setPadding(0, padding, 0, padding);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tất cả đánh giá")
                .setView(recyclerView)
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void openSellerChat() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để nhắn tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (product == null || TextUtils.isEmpty(product.getId()) || TextUtils.isEmpty(product.getSeller_id())) {
            Toast.makeText(requireContext(), "Không thể mở hội thoại cho sản phẩm này", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser.getUid().equals(product.getSeller_id())) {
            Toast.makeText(requireContext(), "Bạn đang xem tin của chính mình", Toast.LENGTH_SHORT).show();
            return;
        }

        String buyerName = !TextUtils.isEmpty(currentUser.getDisplayName())
                ? currentUser.getDisplayName() : currentUser.getEmail();
        String sellerName = tvSellerName != null && !TextUtils.isEmpty(tvSellerName.getText())
                ? tvSellerName.getText().toString() : "Người bán";
        String chatImageUrl = !TextUtils.isEmpty(imageUrl) ? imageUrl : firstProductImage();

        ChatBottomSheetFragment chat = ChatBottomSheetFragment.newProductChat(
                product,
                chatImageUrl,
                currentUser.getUid(),
                buyerName,
                sellerName
        );
        chat.show(getParentFragmentManager(), "product_chat_" + product.getId());
        dismissAllowingStateLoss();
    }

    private boolean canPurchase(String currentUserId) {
        if (product == null || TextUtils.isEmpty(product.getId())) {
            Toast.makeText(requireContext(), "Sản phẩm không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (currentUserId != null && currentUserId.equals(product.getSeller_id())) {
            Toast.makeText(requireContext(), "Bạn không thể mua tin của chính mình", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isProductAvailable()) {
            Toast.makeText(requireContext(), "Sản phẩm này hiện không còn bán", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean isProductAvailable() {
        if (product == null || TextUtils.isEmpty(product.getStatus())) {
            return true;
        }
        String status = product.getStatus().trim().toLowerCase(Locale.ROOT);
        return status.equals("active") || status.equals("available") || status.equals("pending") || status.equals("còn hàng");
    }

    private void updateOwnProductState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean ownProduct = user != null && product != null && user.getUid().equals(product.getSeller_id());
        if (ownProduct) {
            btnAddToCart.setEnabled(false);
            btnBuyNow.setEnabled(false);
            btnAddToCart.setAlpha(0.55f);
            btnBuyNow.setAlpha(0.55f);
            tvAddToCartLabel.setText("Tin của bạn");
            tvBuyNowLabel.setText("Không thể mua");
        }
    }

    private void updateFavoriteUi(boolean saved) {
        favoriteSaved = saved;
        if (tvFavoriteIcon != null) {
            tvFavoriteIcon.setText(saved ? "♥" : "♡");
        }
        if (tvFavoriteLabel != null) {
            tvFavoriteLabel.setText(saved ? "Đã lưu" : "Lưu");
        }
    }

    private void setFavoriteLoading(boolean loading) {
        btnFavorite.setEnabled(!loading);
        btnFavorite.setAlpha(loading ? 0.6f : 1f);
        tvFavoriteLabel.setText(loading ? "Đang lưu" : (favoriteSaved ? "Đã lưu" : "Lưu"));
    }

    private void setActionLoading(LinearLayout button, TextView label, boolean loading, String text) {
        button.setEnabled(!loading);
        button.setAlpha(loading ? 0.65f : 1f);
        label.setText(text);
    }

    private void navigateToOrders() {
        dismissAllowingStateLoss();
        Navigation.findNavController(requireActivity(), R.id.controllerNavHost).navigate(R.id.ordersFragment);
    }

    private String statusLabel(String status) {
        if (TextUtils.isEmpty(status)) {
            return "Còn bán";
        }
        switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "active":
            case "available":
                return "Còn bán";
            case "pending":
                return "Chờ xác nhận";
            case "sold":
                return "Đã bán";
            case "inactive":
                return "Tạm ẩn";
            default:
                return status;
        }
    }

    private String firstProductImage() {
        if (product != null && product.getImage_urls() != null && !product.getImage_urls().isEmpty()) {
            return product.getImage_urls().get(0);
        }
        return null;
    }

    private String safeTitle(Order order) {
        return order != null && !TextUtils.isEmpty(order.getProduct_title())
                ? order.getProduct_title()
                : "sản phẩm";
    }

    private String nowIsoUtc() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    private String stableDocId(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append('_');
            }
            builder.append(part != null ? part : "item");
        }
        return builder.toString().replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

}
