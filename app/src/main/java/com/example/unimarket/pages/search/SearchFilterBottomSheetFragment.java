package com.example.unimarket.pages.search;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.unimarket.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SearchFilterBottomSheetFragment extends BottomSheetDialogFragment {

    private EditText etPriceFrom;
    private EditText etPriceTo;
    private CheckBox cbNew;
    private CheckBox cbUsed;
    private CheckBox cbFreeShip;
    private CheckBox cbFastShip;
    private TextView tvFilterSummary;

    private double initialMinPrice;
    private double initialMaxPrice = Double.MAX_VALUE;
    private boolean initialFilterNew;
    private boolean initialFilterUsed;
    private boolean updatingConditionSelection;
    private FilterListener filterListener;

    public interface FilterListener {
        void onApplyFilter(double minPrice, double maxPrice, boolean filterNew, boolean filterUsed,
                           boolean filterFreeShip, boolean filterFastShip);
        void onResetFilter();
    }

    public void setFilterListener(FilterListener listener) {
        this.filterListener = listener;
    }

    public void setInitialState(double minPrice, double maxPrice, boolean filterNew, boolean filterUsed) {
        initialMinPrice = minPrice;
        initialMaxPrice = maxPrice;
        initialFilterNew = filterNew;
        initialFilterUsed = filterUsed;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_search_filter, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() == null) {
            return;
        }
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPriceFrom = view.findViewById(R.id.etPriceFrom);
        etPriceTo = view.findViewById(R.id.etPriceTo);
        cbNew = view.findViewById(R.id.cbNew);
        cbUsed = view.findViewById(R.id.cbUsed);
        cbFreeShip = view.findViewById(R.id.cbFreeShip);
        cbFastShip = view.findViewById(R.id.cbFastShip);
        tvFilterSummary = view.findViewById(R.id.tvFilterSummary);

        bindInitialState();
        setupLiveSummary();
        disableUnsupportedShippingFilters();
        updateFilterSummary();

        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        TextView btnApplyFilter = view.findViewById(R.id.btnApplyFilter);
        btnApplyFilter.setOnClickListener(v -> applyFilter());

        TextView btnResetFilter = view.findViewById(R.id.btnResetFilter);
        btnResetFilter.setOnClickListener(v -> resetFilter());
    }

    private void bindInitialState() {
        if (initialMinPrice > 0) {
            etPriceFrom.setText(String.valueOf((long) initialMinPrice));
        }
        if (initialMaxPrice < Double.MAX_VALUE) {
            etPriceTo.setText(String.valueOf((long) initialMaxPrice));
        }
        cbNew.setChecked(initialFilterNew);
        cbUsed.setChecked(initialFilterUsed);
        cbFreeShip.setChecked(false);
        cbFastShip.setChecked(false);
    }

    private void setupLiveSummary() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFilterSummary();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        etPriceFrom.addTextChangedListener(watcher);
        etPriceTo.addTextChangedListener(watcher);
        cbNew.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingConditionSelection) {
                return;
            }
            if (isChecked) {
                updatingConditionSelection = true;
                cbUsed.setChecked(false);
                updatingConditionSelection = false;
            }
            updateFilterSummary();
        });
        cbUsed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingConditionSelection) {
                return;
            }
            if (isChecked) {
                updatingConditionSelection = true;
                cbNew.setChecked(false);
                updatingConditionSelection = false;
            }
            updateFilterSummary();
        });
    }

    private void applyFilter() {
        double minPrice = parsePrice(etPriceFrom.getText());
        double maxPrice = parsePrice(etPriceTo.getText());
        if (maxPrice <= 0) {
            maxPrice = Double.MAX_VALUE;
        }
        if (maxPrice < minPrice) {
            double temp = minPrice;
            minPrice = maxPrice;
            maxPrice = temp;
        }

        if (filterListener != null) {
            filterListener.onApplyFilter(minPrice, maxPrice, cbNew.isChecked(), cbUsed.isChecked(), false, false);
        }

        dismiss();
    }

    private void resetFilter() {
        etPriceFrom.setText("");
        etPriceTo.setText("");
        cbNew.setChecked(false);
        cbUsed.setChecked(false);
        cbFreeShip.setChecked(false);
        cbFastShip.setChecked(false);
        updateFilterSummary();

        if (filterListener != null) {
            filterListener.onResetFilter();
        }

        dismiss();
    }

    private void disableUnsupportedShippingFilters() {
        cbFreeShip.setChecked(false);
        cbFastShip.setChecked(false);
        cbFreeShip.setEnabled(false);
        cbFastShip.setEnabled(false);
        cbFreeShip.setText("Miễn phí ship");
        cbFastShip.setText("Giao nhanh");
    }

    private void updateFilterSummary() {
        if (tvFilterSummary == null) {
            return;
        }

        int activeCount = 0;
        if (parsePrice(etPriceFrom.getText()) > 0 || parsePrice(etPriceTo.getText()) > 0) {
            activeCount++;
        }
        if (cbNew != null && cbNew.isChecked()) {
            activeCount++;
        }
        if (cbUsed != null && cbUsed.isChecked()) {
            activeCount++;
        }

        if (activeCount == 0) {
            tvFilterSummary.setText("Tinh chỉnh kết quả theo nhu cầu của bạn");
        } else {
            tvFilterSummary.setText("Đang áp dụng " + activeCount + " bộ lọc");
        }
    }

    private double parsePrice(Editable editable) {
        if (editable == null) {
            return 0;
        }
        String raw = editable.toString().trim();
        if (raw.isEmpty()) {
            return 0;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
