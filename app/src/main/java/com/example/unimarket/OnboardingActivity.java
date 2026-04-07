package com.example.unimarket;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class OnboardingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        Button btnStart = findViewById(R.id.btnStart);
        TextView tvSkip = findViewById(R.id.tvSkip);

        btnStart.setOnClickListener(v -> {
            // Chuyển sang màn hình Đăng nhập hoặc Trang chủ
            Toast.makeText(this, "Bắt đầu!", Toast.LENGTH_SHORT).show();
        });

        tvSkip.setOnClickListener(v -> {
            // Xử lý bỏ qua
        });
    }
}