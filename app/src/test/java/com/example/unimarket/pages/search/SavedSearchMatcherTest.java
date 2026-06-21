package com.example.unimarket.pages.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.unimarket.data.model.Product;
import com.example.unimarket.data.model.SavedSearch;

import org.junit.Test;

public class SavedSearchMatcherTest {

    @Test
    public void matches_shouldReturnTrue_whenProductFitsSavedFilters() {
        SavedSearch saved = new SavedSearch();
        saved.setQuery("laptop");
        saved.setMin_price(5000000d);
        saved.setMax_price(12000000d);
        saved.setFilter_used(true);

        Product product = new Product();
        product.setTitle("Laptop Dell Latitude");
        product.setDescription("May hoc tap");
        product.setPrice(9000000d);
        product.setCondition("used");
        product.setStatus("active");

        assertTrue(SavedSearchMatcher.matches(saved, product, false));
    }

    @Test
    public void matches_shouldReturnFalse_whenProductDoesNotFitSavedFilters() {
        SavedSearch saved = new SavedSearch();
        saved.setQuery("tai nghe");
        saved.setFilter_new(true);

        Product product = new Product();
        product.setTitle("Laptop Dell Latitude");
        product.setPrice(9000000d);
        product.setCondition("used");
        product.setStatus("active");

        assertFalse(SavedSearchMatcher.matches(saved, product, false));
    }

    @Test
    public void isNewMatch_shouldIgnoreOlderProducts() {
        SavedSearch saved = new SavedSearch();
        saved.setQuery("laptop");
        saved.setLast_seen_product_created_at("2026-06-21T10:00:00Z");

        Product product = new Product();
        product.setTitle("Laptop Dell Latitude");
        product.setCreated_at("2026-06-20T10:00:00Z");
        product.setStatus("active");

        assertFalse(SavedSearchMatcher.isNewMatch(saved, product, false));
    }
}
