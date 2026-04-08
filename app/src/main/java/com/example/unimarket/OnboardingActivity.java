package com.example.unimarket;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.unimarket.auth.LoginActivity;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREF_NAME = "UniMarketPrefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kiểm tra nếu đã xem Onboarding thì bỏ qua, vào thẳng phần Đăng nhập
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        Button btnStart = findViewById(R.id.btnStart);

        btnStart.setOnClickListener(v -> navigateToLogin());
    }

    private void navigateToLogin() {
        // Lưu trạng thái đã hoàn thành Onboarding để lần sau mở app không hiện lại nữa
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply();

        // Chuyển sang LoginActivity
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}