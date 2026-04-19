package com.example.unimarket;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unimarket.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import android.content.Context;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // (Traffic Controller)
        if (!isOnboardingCompleted()) {
            // 1. Chưa xem Onboarding -> Chuyển sang Onboarding
            startActivity(new Intent(this, OnboardingActivity.class));
        } else if (!isUserLoggedIn()) {
            // 2. Đã xem Onboarding nhưng chưa Đăng nhập -> Chuyển sang Login
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            // 3. Đã xong xuôi -> Vào App chính
            startActivity(new Intent(this, Controller.class));
        }
        finish();
    }

    private boolean isOnboardingCompleted() {
        SharedPreferences prefs = getSharedPreferences("UniMarketPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("onboarding_completed", false);
    }

    private boolean isUserLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }
}