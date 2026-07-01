package com.example.unimarket.pages.search;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class SearchStateViewModel extends ViewModel {
    String query = "";
    String sort = "relevance";
    double minPrice = 0;
    double maxPrice = Double.MAX_VALUE;
    boolean filterNew = false;
    boolean filterUsed = false;
    boolean filterSavedOnly = false;
    boolean filterSellerOnly = false;
    boolean showAllProducts = false;
    int scrollY = 0;
    String productBatchSignature = "";
    final List<String> shuffledProductIds = new ArrayList<>();
}
