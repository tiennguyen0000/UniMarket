package com.example.unimarket.pages.profile;

import com.example.unimarket.data.model.User;

public class ProfileUiState {
    private final User profile;
    private final boolean saving;

    public ProfileUiState(User profile, boolean saving) {
        this.profile = profile;
        this.saving = saving;
    }

    public static ProfileUiState initial() {
        return new ProfileUiState(null, false);
    }

    public User getProfile() {
        return profile;
    }

    public boolean isSaving() {
        return saving;
    }
}
