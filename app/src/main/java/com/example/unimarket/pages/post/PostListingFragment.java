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
import com.example.unimarket.data.model.Category;
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
    private List<Category> categoryList = new ArrayList<>();

    // Launcher chọn nhiều ảnh cùng lúc
    private final ActivityResultLauncher<String> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    List<String> uriStrings = new ArrayList<>();
                    for (Uri uri : uris) uriStrings.add(uri.toString());
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

        // Toolbar back — dùng NavController thay cho onBackPressed() deprecated
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack()
        );

        // Handle system back button
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        NavHostFragment.findNavController(PostListingFragment.this).popBackStack();
                    }
                }
        );

        viewModel.loadCategories();
        observeViewModel();
        setupListeners();
    }

    private void observeViewModel() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), cats -> {
            if (cats != null) categoryList = cats;
        });

        viewModel.getSelectedImages().observe(getViewLifecycleOwner(), images -> {
            renderSelectedImages(images);
            tvImageCount.setText(images.size() + "/6 ảnh");
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            btnSubmit.setEnabled(!loading);
            btnSubmit.setText(loading ? "Đang xử lý..." : "Đăng tin ngay");
        });

        viewModel.getPostSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), "Đăng tin thành công!", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty())
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
        });
    }

    private void renderSelectedImages(List<String> images) {
        int childCount = layoutImages.getChildCount();
        if (childCount > 1) layoutImages.removeViews(1, childCount - 1);

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
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void showCategoryDialog() {
        if (categoryList.isEmpty()) {
            Toast.makeText(requireContext(), "Đang tải danh mục...", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[categoryList.size()];
        for (int i = 0; i < categoryList.size(); i++) names[i] = categoryList.get(i).getName();

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn danh mục")
                .setItems(names, (dialog, which) -> {
                    Category selected = categoryList.get(which);
                    selectedCategoryId = selected.getId();
                    tvCategoryLabel.setText(selected.getName());
                    tvCategoryLabel.setTextColor(getResources().getColor(R.color.text_primary));
                })
                .show();
    }

    private void validateAndSubmit() {
        String title = etTitle.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) { etTitle.setError("Vui lòng nhập tiêu đề"); return; }
        if (TextUtils.isEmpty(priceStr)) { etPrice.setError("Vui lòng nhập giá"); return; }
        if (selectedCategoryId == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rgCondition.getCheckedRadioButtonId() == -1) {
            Toast.makeText(requireContext(), "Vui lòng chọn tình trạng", Toast.LENGTH_SHORT).show();
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
        product.setStatus("active");
        product.setCondition(rgCondition.getCheckedRadioButtonId() == R.id.rbNew ? "NEW" : "USED");

        viewModel.submitProduct(product);
    }
}
