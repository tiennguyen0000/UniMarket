package com.example.unimarket.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;


public class ForgotPasswordActivity extends AppCompatActivity {
    EditText etEmail;
    Button btnReset;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout đơn giản tạo bằng code
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 80, 60, 0);

        etEmail = new EditText(this);
        etEmail.setHint("Nhập email của bạn");
        layout.addView(etEmail);

        btnReset = new Button(this);
        btnReset.setText("Gửi email đặt lại mật khẩu");
        layout.addView(btnReset);

        setContentView(layout);

        mAuth = FirebaseAuth.getInstance();

        btnReset.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Nhập email!");
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Đã gửi email! Kiểm tra hộp thư.", Toast.LENGTH_LONG).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });
    }
}
