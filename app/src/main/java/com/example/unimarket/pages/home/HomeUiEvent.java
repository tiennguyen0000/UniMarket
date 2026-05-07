package com.example.unimarket.pages.home;

public class HomeUiEvent {
    private final String message;

    private HomeUiEvent(String message) {
        this.message = message;
    }

    public static HomeUiEvent showMessage(String message) {
        return new HomeUiEvent(message);
    }

    public String getMessage() {
        return message;
    }
}
