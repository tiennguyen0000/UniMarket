package com.example.unimarket.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unimarket.R;
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 1002;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private TextInputLayout tilFullName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister, btnGoogleRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupGoogleSignIn();
        setupListeners();
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    private void initViews() {
        tilFullName        = findViewById(R.id.tilFullName);
        tilEmail           = findViewById(R.id.tilEmail);
        tilPassword        = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etFullName         = findViewById(R.id.etFullName);
        etEmail            = findViewById(R.id.etEmail);
        etPassword         = findViewById(R.id.etPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);
        btnRegister        = findViewById(R.id.btnRegister);
        btnGoogleRegister  = findViewById(R.id.btnGoogleRegister);
        tvLogin            = findViewById(R.id.tvLogin);
        progressBar        = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        btnGoogleRegister.setOnClickListener(v -> signInWithGoogle());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Clear errors on type
        etFullName.addTextChangedListener(clearError(tilFullName));
        etEmail.addTextChangedListener(clearError(tilEmail));
        etPassword.addTextChangedListener(clearError(tilPassword));
        etConfirmPassword.addTextChangedListener(clearError(tilConfirmPassword));
    }

    // ─── Email/Password Register ──────────────────────────────────────────────

    private void attemptRegister() {
        if (!validateInputs()) return;

        String fullName = etFullName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Cập nhật display name
                            UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();
                            user.updateProfile(profileUpdate);

                            // Gửi email xác thực
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        setLoading(false);
                                        navigateToVerifyEmail(email);
                                    });
                        }
                    } else {
                        setLoading(false);
                        handleRegisterError(task.getException());
                    }
                });
    }

    private boolean validateInputs() {
        boolean valid = true;

        String fullName       = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email          = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password       = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError("Vui lòng nhập họ và tên");
            valid = false;
        } else if (fullName.length() < 2) {
            tilFullName.setError("Họ và tên phải có ít nhất 2 ký tự");
            valid = false;
        }

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

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            valid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            valid = false;
        }

        return valid;
    }

    private void handleRegisterError(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            tilEmail.setError("Email này đã được sử dụng");
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            tilPassword.setError("Mật khẩu quá yếu");
        } else {
            Toast.makeText(this, "Đăng ký thất bại: " + (e != null ? e.getMessage() : ""),
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
        finish();
    }

    // ─── UI Helpers ───────────────────────────────────────────────────────────

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        btnGoogleRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? "Đang đăng ký..." : "Đăng ký");
    }

    private android.text.TextWatcher clearError(TextInputLayout til) {
        return new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                til.setError(null);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
    }
}