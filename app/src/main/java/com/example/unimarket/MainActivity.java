package com.example.unimarket;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.unimarket.auth.LoginActivity;
import com.example.unimarket.test.CrudTestActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivity(new Intent(this, CrudTestActivity.class));



//        if (isUserLoggedIn()) {
//            // Đã đăng nhập -> Vào Home
//            startActivity(new Intent(this, HomeActivity.class));
//        } else {
//            // Chưa đăng nhập -> Vào Login
//            startActivity(new Intent(this, LoginActivity.class));
//        }

        // Quan trọng: Đóng MainActivity để không quay lại đây
        finish();
    }

    private boolean isUserLoggedIn() {
        // Sau này bạn sẽ check SharedPreferences hoặc Firebase ở đây
        // Hiện tại trả về false để test Login, hoặc true để test Home
        return false;
    }
}