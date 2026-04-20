package com.example.unimarket.pages.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    
    private FilterListener filterListener;

    public interface FilterListener {
        void onApplyFilter(double minPrice, double maxPrice, boolean filterNew, boolean filterUsed, boolean filterFreeShip, boolean filterFastShip);
        void onResetFilter();
    }

    public void setFilterListener(FilterListener listener) {
        this.filterListener = listener;
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
        
        ImageView closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());
        
        Button btnApplyFilter = view.findViewById(R.id.btnApplyFilter);
        btnApplyFilter.setOnClickListener(v -> applyFilter());
        
        Button btnResetFilter = view.findViewById(R.id.btnResetFilter);
        btnResetFilter.setOnClickListener(v -> resetFilter());
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
            // Keep default values
        }
        
        boolean filterNew = cbNew.isChecked();
        boolean filterUsed = cbUsed.isChecked();
        boolean filterFreeShip = cbFreeShip.isChecked();
        boolean filterFastShip = cbFastShip.isChecked();
        
        if (filterListener != null) {
            filterListener.onApplyFilter(minPrice, maxPrice, filterNew, filterUsed, filterFreeShip, filterFastShip);
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
}
