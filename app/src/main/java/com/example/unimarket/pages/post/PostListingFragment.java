package com.example.unimarket.pages.post;

import android.net.Uri;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.unimarket.R;
import com.example.unimarket.data.model.Product;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class PostListingFragment extends Fragment {

    private EditText etTitle, etPrice, etDescription;
    private RadioGroup rgCondition;
    private MaterialButton btnSubmit;
    private View btnUploadImage, btnSelectCategory;
    private TextView tvImageCount, tvCategoryLabel;
    private LinearLayout layoutImages;
    
    private PostListingViewModel viewModel;
    private String selectedCategoryId = null;

    // Launcher để mở Gallery chọn nhiều ảnh cùng lúc
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_listing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PostListingViewModel.class);

        // Bind views
        etTitle = view.findViewById(R.id.etTitle);
        etPrice = view.findViewById(R.id.etPrice);
        etDescription = view.findViewById(R.id.etDescription);
        rgCondition = view.findViewById(R.id.rgCondition);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnUploadImage = view.findViewById(R.id.btnUploadImage);
        btnSelectCategory = view.findViewById(R.id.btnSelectCategory);
        tvImageCount = view.findViewById(R.id.tvImageCount);
        tvCategoryLabel = view.findViewById(R.id.tvCategoryLabel);
        layoutImages = view.findViewById(R.id.layoutImages);

        // Setup toolbar back button
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert); 
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        observeViewModel();
        setupListeners();
    }

    private void observeViewModel() {
        // Quan sát danh sách ảnh để vẽ lại UI
        viewModel.getSelectedImages().observe(getViewLifecycleOwner(), images -> {
            renderSelectedImages(images);
            tvImageCount.setText(images.size() + "/6 ảnh");
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            btnSubmit.setEnabled(!loading);
            btnSubmit.setText(loading ? "Đang xử lý..." : "Đăng tin ngay");
        });

        viewModel.getPostSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(requireContext(), "Đăng tin thành công!", Toast.LENGTH_SHORT).show();
                requireActivity().onBackPressed();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private void renderSelectedImages(List<String> images) {
        // Xóa các ảnh preview cũ (giữ lại nút thêm ảnh ở vị trí 0)
        int childCount = layoutImages.getChildCount();
        if (childCount > 1) {
            layoutImages.removeViews(1, childCount - 1);
        }

        for (String uriString : images) {
            View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_image_preview, layoutImages, false);
            ImageView ivPreview = itemView.findViewById(R.id.ivPreview);
            View btnDelete = itemView.findViewById(R.id.btnDelete);

            Glide.with(this).load(Uri.parse(uriString)).into(ivPreview);
            
            btnDelete.setOnClickListener(v -> {
                viewModel.removeImage(uriString);
            });

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

        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void showCategoryDialog() {
        String[] categories = {"Sách & Giáo trình", "Đồ điện tử", "Thời trang", "Đồ gia dụng", "Khác"};
        String[] categoryIds = {"cat_books", "cat_electronics", "cat_fashion", "cat_home", "cat_other"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn danh mục")
                .setItems(categories, (dialog, which) -> {
                    selectedCategoryId = categoryIds[which];
                    tvCategoryLabel.setText(categories[which]);
                    tvCategoryLabel.setTextColor(getResources().getColor(R.color.text_primary));
                })
                .show();
    }

    private void validateAndSubmit() {
        String title = etTitle.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Vui lòng nhập tiêu đề");
            return;
        }
        if (selectedCategoryId == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Product product = new Product();
        product.setTitle(title);
        product.setDescription(description);
        product.setCategory_id(selectedCategoryId);
        try {
            product.setPrice(Double.parseDouble(priceStr));
        } catch (NumberFormatException e) {
            etPrice.setError("Giá không hợp lệ");
            return;
        }
        
        product.setSeller_id(FirebaseAuth.getInstance().getUid());
        product.setStatus("AVAILABLE");
        product.setCondition(rgCondition.getCheckedRadioButtonId() == R.id.rbNew ? "NEW" : "USED");
        
        viewModel.submitProduct(product);
    }
}