package com.example.unimarket.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.unimarket.R;
import com.example.unimarket.MainActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 1001;
    private static final int MIN_PASSWORD_LENGTH = 8;

    // Views
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogleLogin;
    private TextView tvForgotPassword, tvRegister;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupGoogleSignIn();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Auto-redirect nếu đã đăng nhập & đã verify email
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            navigateToMain();
        }
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    private void initViews() {
        tilEmail         = findViewById(R.id.tilEmail);
        tilPassword      = findViewById(R.id.tilPassword);
        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        btnLogin         = findViewById(R.id.btnLogin);
        btnGoogleLogin   = findViewById(R.id.btnGoogleLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister       = findViewById(R.id.tvRegister);
        progressBar      = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // from google-services.json
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });

        // Clear errors on typing
        etEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilEmail.setError(null);
            }
        });
        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilPassword.setError(null);
            }
        });
    }

    // ─── Email/Password Login ─────────────────────────────────────────────────

    private void attemptLogin() {
        if (!validateInputs()) return;

        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            navigateToMain();
                        } else {
                            setLoading(false);
                            // Gửi lại email verify rồi chuyển sang màn hình verify
                            if (user != null) user.sendEmailVerification();
                            navigateToVerifyEmail(email);
                        }
                    } else {
                        setLoading(false);
                        handleLoginError(task.getException());
                    }
                });
    }

    private boolean validateInputs() {
        boolean valid = true;

        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Vui lòng nhập email");
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            valid = false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            tilPassword.setError("Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
            valid = false;
        }

        return valid;
    }

    private void handleLoginError(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException) {
            tilEmail.setError("Email không tồn tại");
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            tilPassword.setError("Mật khẩu không đúng");
        } else {
            Toast.makeText(this, "Đăng nhập thất bại: " + (e != null ? e.getMessage() : ""),
                    Toast.LENGTH_LONG).show();
        }
    }

    // ─── Google Sign-In ───────────────────────────────────────────────────────

    private void signInWithGoogle() {
        setLoading(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                setLoading(false);
                Toast.makeText(this, "Google Sign-In thất bại: " + e.getStatusCode(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        navigateToMain();
                    } else {
                        Toast.makeText(this, "Xác thực Google thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToVerifyEmail(String email) {
        Intent intent = new Intent(this, VerifyEmailActivity.class);
        intent.putExtra("email", email);
        startActivity(intent);
    }

    // ─── UI Helpers ───────────────────────────────────────────────────────────

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        btnGoogleLogin.setEnabled(!isLoading);
        btnLogin.setText(isLoading ? "Đang đăng nhập..." : "Đăng nhập");
    }

    private abstract static class SimpleTextWatcher implements android.text.TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(android.text.Editable s) {}
    }
}