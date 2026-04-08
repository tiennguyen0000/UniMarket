package com.example.unimarket;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Nút mở trình Test CRUD
        findViewById(R.id.btnOpenCrudTest).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.unimarket.test.CrudTestActivity.class);
            startActivity(intent);
        });

        // Giả lập các nút khác (sẽ code sau)
        findViewById(R.id.btnProducts).setOnClickListener(v -> {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });
    }
}
