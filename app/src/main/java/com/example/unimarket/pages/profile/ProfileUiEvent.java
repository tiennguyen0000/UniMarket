package com.example.unimarket.pages.profile;

public class ProfileUiEvent {
    private final String message;
    private final boolean error;

    private ProfileUiEvent(String message, boolean error) {
        this.message = message;
        this.error = error;
    }

    public static ProfileUiEvent success(String message) {
        return new ProfileUiEvent(message, false);
    }

    public static ProfileUiEvent error(String message) {
        return new ProfileUiEvent(message, true);
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return error;
    }
}
