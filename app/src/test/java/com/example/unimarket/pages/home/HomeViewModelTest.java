package com.example.unimarket.pages.home;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class HomeViewModelTest {

    @Test
    public void initialUiState_shouldBeAvailable() {
        HomeViewModel viewModel = new HomeViewModel();

        HomeUiState state = viewModel.getUiState().getValue();

        assertNotNull(state);
        assertFalse(state.isLoading());
        assertNotNull(state.getCategories());
        assertNotNull(state.getProducts());
        assertNotNull(state.getProductImages());
    }
}
