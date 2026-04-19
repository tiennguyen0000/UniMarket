package com.example.unimarket.pages.profile;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.base.Result;

public class ProfileViewModel extends ViewModel {
    private final UserService userService = new UserService();

    private final MutableLiveData<ProfileUiState> uiState = new MutableLiveData<>(ProfileUiState.initial());
    private final MutableLiveData<ProfileUiEvent> uiEvent = new MutableLiveData<>();

    public LiveData<ProfileUiState> getUiState() {
        return uiState;
    }

    public LiveData<ProfileUiEvent> getUiEvent() {
        return uiEvent;
    }

    public void loadProfile(String userId, String displayName) {
        if (!TextUtils.isEmpty(displayName)) {
            User initialProfile = new User();
            initialProfile.setId(userId);
            initialProfile.setFull_name(displayName);
            updateState(initialProfile, null);
        }

        userService.fetchById(userId, result -> {
            if (result.isSuccess() && result.getData() != null) {
                updateState(result.getData(), null);
            }
            // Do not block UI when profile is missing or network fails.
        });
    }

    public void saveProfile(String userId, String fullName, String phone, String university) {
        updateState(null, true);

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setFull_name(fullName);
        updatedUser.setPhone(phone);
        updatedUser.setUniversity(university);

        userService.save(updatedUser, result -> {
            updateState(result.isSuccess() ? (result.getData() != null ? result.getData() : updatedUser) : null, false);
            if (result.isSuccess()) {
                uiEvent.setValue(ProfileUiEvent.success("Đã lưu hồ sơ!"));
            } else {
                uiEvent.setValue(ProfileUiEvent.error("Lưu thất bại: " + result.getError()));
            }
        });
    }

    private void updateState(User profile, Boolean saving) {
        ProfileUiState current = uiState.getValue() != null ? uiState.getValue() : ProfileUiState.initial();
        uiState.setValue(new ProfileUiState(
                profile != null ? profile : current.getProfile(),
                saving != null ? saving : current.isSaving()
        ));
    }
}
