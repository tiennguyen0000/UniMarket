package com.example.unimarket.pages.profile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ProfileViewModelTest {

    @Test
    public void initialUiState_shouldBeAvailable() {
        ProfileViewModel viewModel = new ProfileViewModel();

        ProfileUiState state = viewModel.getUiState().getValue();

        assertNotNull(state);
        assertFalse(state.isSaving());
    }
}
