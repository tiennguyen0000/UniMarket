package com.example.unimarket.auth;


import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unimarket.R;
import com.example.unimarket.MainActivity;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.base.AsyncCrudService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    private static final long RESEND_COOLDOWN_MS = 60_000L; // 60 seconds

    private TextView tvEmailSentTo, tvResendCooldown;
    private MaterialButton btnCheckVerified, btnResendEmail;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private CountDownTimer countDownTimer;
    private boolean isCooldownRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupEmail();
        setupListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    private void initViews() {
        tvEmailSentTo    = findViewById(R.id.tvEmailSentTo);
        tvResendCooldown = findViewById(R.id.tvResendCooldown);
        btnCheckVerified = findViewById(R.id.btnCheckVerified);
        btnResendEmail   = findViewById(R.id.btnResendEmail);
        progressBar      = findViewById(R.id.progressBar);
    }

    private void setupEmail() {
        String email = getIntent().getStringExtra("email");
        if (email == null || email.isEmpty()) {
            FirebaseUser user = mAuth.getCurrentUser();
            email = (user != null) ? user.getEmail() : "";
        }
        tvEmailSentTo.setText("Chúng tôi đã gửi email xác thực đến\n" + email);
    }

    private void setupListeners() {
        btnCheckVerified.setOnClickListener(v -> checkEmailVerified());
        btnResendEmail.setOnClickListener(v -> resendVerificationEmail());

        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // ─── Check Verified ───────────────────────────────────────────────────────

    private void checkEmailVerified() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            navigateToLogin();
            return;
        }

        setLoading(true);

        // Reload user để lấy trạng thái mới nhất từ Firebase
        user.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshedUser = mAuth.getCurrentUser();
            if (refreshedUser != null && refreshedUser.isEmailVerified()) {
                // Đồng bộ trạng thái verified sang Supabase profile
                User updatedProfile = new User();
                updatedProfile.setId(refreshedUser.getUid());
                updatedProfile.setIs_verified(true);

                new UserService().upsertProfile(updatedProfile, new AsyncCrudService.ItemCallback<User>() {
                    @Override
                    public void onSuccess(User data) {
                        setLoading(false);
                        Toast.makeText(VerifyEmailActivity.this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    }

                    @Override
                    public void onError(String error) {
                        // Không block navigation nếu sync Supabase thất bại
                        setLoading(false);
                        Toast.makeText(VerifyEmailActivity.this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    }
                });
            } else {
                setLoading(false);
                Toast.makeText(this,
                        "Email chưa được xác thực. Vui lòng kiểm tra hộp thư.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ─── Resend Email ─────────────────────────────────────────────────────────

    private void resendVerificationEmail() {
        if (isCooldownRunning) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            navigateToLogin();
            return;
        }

        setLoading(true);

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Email xác thực đã được gửi lại!", Toast.LENGTH_SHORT).show();
                        startResendCooldown();
                    } else {
                        Toast.makeText(this, "Gửi email thất bại, vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startResendCooldown() {
        isCooldownRunning = true;
        btnResendEmail.setEnabled(false);
        tvResendCooldown.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(RESEND_COOLDOWN_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvResendCooldown.setText("Gửi lại sau " + seconds + "s");
            }

            @Override
            public void onFinish() {
                isCooldownRunning = false;
                btnResendEmail.setEnabled(true);
                tvResendCooldown.setVisibility(View.GONE);
            }
        }.start();
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ─── UI Helpers ───────────────────────────────────────────────────────────

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCheckVerified.setEnabled(!isLoading);
        if (!isCooldownRunning) btnResendEmail.setEnabled(!isLoading);
    }
}