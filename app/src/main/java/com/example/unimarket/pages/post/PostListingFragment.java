package com.example.unimarket.pages.post;

import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.auth.AccessControl;
import com.example.unimarket.data.model.Category;
import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.ProductService;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.example.unimarket.pages.home.HomeViewModel;
import com.example.unimarket.pages.home.HomeUiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class PostListingFragment extends Fragment {
    public static final String RESULT_LISTING_CREATED = "listing_created";
    public static final String ARG_EDIT_PRODUCT_ID = "edit_product_id";

    private EditText etTitle, etPrice, etQuantity, etDescription;
    private RadioGroup rgCondition;
    private MaterialButton btnSubmit;
    private View btnUploadImage, btnSelectCategory, ivPreview;
    private TextView tvImageCount, tvCategoryLabel, tvCategoryError, tvConditionError;
    private LinearLayout layoutImages;

    private PostListingViewModel viewModel;
    private HomeViewModel catalogViewModel;
    private String selectedCategoryId = null;
    private List<Category> categoryList = new ArrayList<>();
    private boolean canPostListing = false;
    private boolean editMode = false;
    private Product editingProduct;
    private final UserService userService = new UserService();
    private final ProductService productService = new ProductService();

    private final ActivityResultLauncher<String> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    List<String> uriStrings = new ArrayList<>();
                    for (Uri uri : uris) {
                        uriStrings.add(uri.toString());
                    }
                    viewModel.addImages(uriStrings);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_listing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PostListingViewModel.class);
        catalogViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        etTitle = view.findViewById(R.id.etTitle);
        etPrice = view.findViewById(R.id.etPrice);
        etQuantity = view.findViewById(R.id.etQuantity);
        etDescription = view.findViewById(R.id.etDescription);
        rgCondition = view.findViewById(R.id.rgCondition);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnUploadImage = view.findViewById(R.id.btnUploadImage);
        btnSelectCategory = view.findViewById(R.id.btnSelectCategory);
        ivPreview = view.findViewById(R.id.ivPreview);
        tvImageCount = view.findViewById(R.id.tvImageCount);
        tvCategoryLabel = view.findViewById(R.id.tvCategoryLabel);
        tvCategoryError = view.findViewById(R.id.tvCategoryError);
        tvConditionError = view.findViewById(R.id.tvConditionError);
        layoutImages = view.findViewById(R.id.layoutImages);
        btnSubmit.setEnabled(false);

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        setupLightSystemBars();
        toolbar.setNavigationIcon(R.drawable.ic_back);
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.text_primary));
        }
        toolbar.setNavigationContentDescription("Quay lại");
        toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack()
        );

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        NavHostFragment.findNavController(PostListingFragment.this).popBackStack();
                    }
                }
        );

        readEditArgs();
        guardSellAccess();
        observeViewModel();
        catalogViewModel.loadHomeData();
        loadEditingProductIfNeeded();
        setupListeners();
    }

    private void readEditArgs() {
        Bundle args = getArguments();
        editMode = args != null && !TextUtils.isEmpty(args.getString(ARG_EDIT_PRODUCT_ID));
    }

    private void guardSellAccess() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || TextUtils.isEmpty(firebaseUser.getUid())) {
            showSellBlocked("Vui lòng đăng nhập để đăng tin bán hàng.");
            return;
        }

        userService.getProfileById(firebaseUser.getUid(), new AsyncCrudService.ItemCallback<User>() {
            @Override
            public void onSuccess(User data) {
                if (!isAdded()) {
                    return;
                }
                if (AccessControl.canSell(data)) {
                    canPostListing = true;
                    btnSubmit.setEnabled(true);
                } else {
                    showSellBlocked("Tài khoản cần được xác thực sinh viên trước khi đăng bán.");
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    showSellBlocked("Không thể kiểm tra quyền đăng tin. Vui lòng thử lại.");
                }
            }
        });
    }

    private void showSellBlocked(String message) {
        canPostListing = false;
        btnSubmit.setEnabled(false);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chưa thể đăng tin")
                .setMessage(message)
                .setPositiveButton("Đã hiểu", (dialog, which) ->
                        NavHostFragment.findNavController(this).popBackStack())
                .show();
    }

    private void setupLightSystemBars() {
        requireActivity().getWindow().setStatusBarColor(Color.WHITE);
        requireActivity().getWindow().setNavigationBarColor(Color.WHITE);

        int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        requireActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void observeViewModel() {
        catalogViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            List<Category> cats = state != null ? state.getCategories() : null;
            if (cats != null) {
                categoryList = cats;
                bindSelectedCategoryLabel();
            }
        });

        viewModel.getSelectedImages().observe(getViewLifecycleOwner(), images -> {
            renderSelectedImages(images);
            tvImageCount.setText(images.size() + "/6 ảnh");
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            btnSubmit.setEnabled(canPostListing && !loading);
            if (editMode && !loading) {
                btnSubmit.setText("Lưu cập nhật");
            }
            btnSubmit.setText(loading ? "Đang xử lý..." : "Đăng tin ngay");
        });

        viewModel.getPostSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                catalogViewModel.refreshCatalogData();
                getParentFragmentManager().setFragmentResult(RESULT_LISTING_CREATED, new Bundle());
                if (editMode) {
                    NavHostFragment.findNavController(this).popBackStack();
                    return;
                }
                NavHostFragment.findNavController(this).navigate(R.id.profileFragment);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderSelectedImages(List<String> images) {
        int childCount = layoutImages.getChildCount();
        if (childCount > 1) {
            layoutImages.removeViews(1, childCount - 1);
        }

        for (String uriString : images) {
            View itemView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_image_preview, layoutImages, false);
            ImageView ivPreview = itemView.findViewById(R.id.ivPreview);
            View btnDelete = itemView.findViewById(R.id.btnDelete);
            Glide.with(this).load(Uri.parse(uriString)).centerCrop().into(ivPreview);
            btnDelete.setOnClickListener(v -> viewModel.removeImage(uriString));
            layoutImages.addView(itemView);
        }
    }

    private void setupListeners() {
        btnUploadImage.setOnClickListener(v -> {
            List<String> current = viewModel.getSelectedImages().getValue();
            if (current != null && current.size() < 6) {
                pickImagesLauncher.launch("image/*");
            } else {
                Toast.makeText(requireContext(), "Tối đa 6 ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        btnSelectCategory.setOnClickListener(v -> showCategoryDialog());
        ivPreview.setOnClickListener(v -> showListingPreview());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
        rgCondition.setOnCheckedChangeListener((group, checkedId) -> tvConditionError.setVisibility(View.GONE));
    }

    private void loadEditingProductIfNeeded() {
        if (!editMode) {
            return;
        }
        Bundle args = getArguments();
        String productId = args != null ? args.getString(ARG_EDIT_PRODUCT_ID) : null;
        if (TextUtils.isEmpty(productId)) {
            return;
        }
        productService.getById(productId, new AsyncCrudService.ItemCallback<Product>() {
            @Override
            public void onSuccess(Product data) {
                if (!isAdded() || data == null) {
                    return;
                }
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null || !user.getUid().equals(data.getSeller_id())) {
                    showSellBlocked("Bạn chỉ có thể cập nhật tin do chính mình đăng.");
                    return;
                }
                editingProduct = data;
                bindEditingProduct(data);
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Không thể tải tin cần cập nhật.", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(PostListingFragment.this).popBackStack();
                }
            }
        });
    }

    private void bindEditingProduct(Product product) {
        etTitle.setText(product.getTitle() != null ? product.getTitle() : "");
        etPrice.setText(product.getPrice() != null ? String.valueOf(product.getPrice().longValue()) : "");
        etQuantity.setText(product.getQuantity() != null ? String.valueOf(product.getQuantity()) : "");
        etDescription.setText(product.getDescription() != null ? product.getDescription() : "");
        selectedCategoryId = product.getCategory_id();
        bindSelectedCategoryLabel();
        if ("NEW".equalsIgnoreCase(product.getCondition()) || "new".equalsIgnoreCase(product.getCondition())) {
            rgCondition.check(R.id.rbNew);
        } else if (!TextUtils.isEmpty(product.getCondition())) {
            rgCondition.check(R.id.rbUsed);
        }
        btnSubmit.setText("Lưu cập nhật");
        viewModel.setImages(product.getImage_urls());
    }

    private void bindSelectedCategoryLabel() {
        String categoryName = selectedCategoryName();
        if (!TextUtils.isEmpty(categoryName)) {
            tvCategoryLabel.setText(categoryName);
            tvCategoryLabel.setTextColor(getResources().getColor(R.color.text_primary));
        }
    }

    private void showCategoryDialog() {
        if (categoryList.isEmpty()) {
            Toast.makeText(requireContext(), "Đang tải danh mục...", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[categoryList.size()];
        for (int i = 0; i < categoryList.size(); i++) {
            names[i] = categoryList.get(i).getName();
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn danh mục")
                .setItems(names, (dialog, which) -> {
                    Category selected = categoryList.get(which);
                    selectedCategoryId = selected.getId();
                    tvCategoryLabel.setText(selected.getName());
                    tvCategoryLabel.setTextColor(getResources().getColor(R.color.text_primary));
                    tvCategoryError.setVisibility(View.GONE);
                })
                .show();
    }

    private void showListingPreview() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String quantityStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String category = selectedCategoryName();
        String condition = rgCondition.getCheckedRadioButtonId() == R.id.rbNew
                ? "Mới" : rgCondition.getCheckedRadioButtonId() == R.id.rbUsed ? "Đã sử dụng" : "Chưa chọn";
        int imageCount = viewModel.getSelectedImages().getValue() != null
                ? viewModel.getSelectedImages().getValue().size() : 0;

        String price = priceStr.isEmpty() ? "Chưa nhập" : priceStr + " VNĐ";
        try {
            if (!priceStr.isEmpty()) {
                price = HomeUiUtils.formatPrice(Double.parseDouble(priceStr));
            }
        } catch (NumberFormatException ignored) {
            price = priceStr + " VNĐ";
        }

        String message = "Tiêu đề: " + (title.isEmpty() ? "Chưa nhập" : title)
                + "\nGiá: " + price
                + "\nSố lượng: " + (quantityStr.isEmpty() ? "Chưa nhập" : quantityStr)
                + "\nDanh mục: " + (category != null ? category : "Chưa chọn")
                + "\nTình trạng: " + condition
                + "\nẢnh: " + imageCount + "/6"
                + "\n\nMô tả:\n" + (description.isEmpty() ? "Chưa nhập mô tả" : description);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xem trước tin đăng")
                .setMessage(message)
                .setPositiveButton("Tiếp tục chỉnh", null)
                .show();
    }

    private void validateAndSubmit() {
        if (!canPostListing) {
            showSellBlocked("Tài khoản cần được xác thực sinh viên trước khi đăng bán.");
            return;
        }

        String title = etTitle.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String quantityStr = etQuantity.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Vui lòng nhập tiêu đề");
            return;
        }
        if (TextUtils.isEmpty(priceStr)) {
            etPrice.setError("Vui lòng nhập giá");
            return;
        }
        if (TextUtils.isEmpty(quantityStr)) {
            etQuantity.setError("Vui lòng nhập số lượng");
            return;
        }
        if (selectedCategoryId == null) {
            tvCategoryError.setVisibility(View.VISIBLE);
            btnSelectCategory.requestFocus();
            return;
        }
        if (rgCondition.getCheckedRadioButtonId() == -1) {
            tvConditionError.setVisibility(View.VISIBLE);
            rgCondition.requestFocus();
            return;
        }

        Product product = editMode && editingProduct != null ? editingProduct : new Product();
        product.setTitle(title);
        product.setDescription(description);
        product.setCategory_id(selectedCategoryId);
        try {
            product.setPrice(Double.parseDouble(priceStr));
            if (product.getPrice() == null || product.getPrice() <= 0) {
                etPrice.setError("Giá phải lớn hơn 0");
                return;
            }
        } catch (NumberFormatException e) {
            etPrice.setError("Giá không hợp lệ");
            return;
        }
        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                etQuantity.setError("Số lượng phải lớn hơn 0");
                return;
            }
            product.setQuantity(quantity);
        } catch (NumberFormatException e) {
            etQuantity.setError("Số lượng không hợp lệ");
            return;
        }

        String sellerId = FirebaseAuth.getInstance().getUid();
        if (TextUtils.isEmpty(sellerId)) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để đăng tin", Toast.LENGTH_SHORT).show();
            return;
        }
        product.setSeller_id(sellerId);
        product.setStatus("active");
        product.setCondition(rgCondition.getCheckedRadioButtonId() == R.id.rbNew ? "NEW" : "USED");

        viewModel.submitProduct(product);
    }

    private String selectedCategoryName() {
        if (selectedCategoryId == null) {
            return null;
        }
        for (Category category : categoryList) {
            if (category != null && selectedCategoryId.equals(category.getId())) {
                return category.getName();
            }
        }
        return null;
    }
}
