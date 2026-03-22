package com.example.unimarket.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 80, 60, 0);
        layout.setGravity(android.view.Gravity.CENTER);

        TextView tv = new TextView(this);
        tv.setText("Chào mừng đến UniMarket! 🎉");
        tv.setTextSize(20);
        layout.addView(tv);

        Button btnLogout = new Button(this);
        btnLogout.setText("Đăng xuất");
        layout.addView(btnLogout);

        setContentView(layout);

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
