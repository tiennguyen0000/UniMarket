package com.example.unimarket.pages.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

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

    private double initialMinPrice;
    private double initialMaxPrice = Double.MAX_VALUE;
    private boolean initialFilterNew;
    private boolean initialFilterUsed;
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPriceFrom = view.findViewById(R.id.etPriceFrom);
        etPriceTo = view.findViewById(R.id.etPriceTo);
        cbNew = view.findViewById(R.id.cbNew);
        cbUsed = view.findViewById(R.id.cbUsed);
        cbFreeShip = view.findViewById(R.id.cbFreeShip);
        cbFastShip = view.findViewById(R.id.cbFastShip);

        bindInitialState();
        disableUnsupportedShippingFilters();

        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        Button btnApplyFilter = view.findViewById(R.id.btnApplyFilter);
        btnApplyFilter.setOnClickListener(v -> applyFilter());

        Button btnResetFilter = view.findViewById(R.id.btnResetFilter);
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
    }

    private void applyFilter() {
        double minPrice = 0;
        double maxPrice = Double.MAX_VALUE;

        try {
            String fromText = etPriceFrom.getText().toString().trim();
            if (!fromText.isEmpty()) {
                minPrice = Double.parseDouble(fromText);
            }

            String toText = etPriceTo.getText().toString().trim();
            if (!toText.isEmpty()) {
                maxPrice = Double.parseDouble(toText);
            }
        } catch (NumberFormatException e) {
            minPrice = 0;
            maxPrice = Double.MAX_VALUE;
        }

        boolean filterNew = cbNew.isChecked();
        boolean filterUsed = cbUsed.isChecked();

        if (filterListener != null) {
            filterListener.onApplyFilter(minPrice, maxPrice, filterNew, filterUsed, false, false);
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
        cbFreeShip.setAlpha(0.55f);
        cbFastShip.setAlpha(0.55f);
        cbFreeShip.setText("Miá»…n phÃ­ váº­n chuyá»ƒn (sáº½ há»— trá»£ sau)");
        cbFastShip.setText("Giao hÃ ng nhanh (sáº½ há»— trá»£ sau)");
    }
}
