package com.example.unimarket.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.unimarket.MainActivity;
import com.example.unimarket.R;
import com.example.unimarket.data.model.User;
import com.example.unimarket.data.service.UserService;
import com.example.unimarket.data.service.base.AsyncCrudService;
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

        etFullName.addTextChangedListener(clearError(tilFullName));
        etEmail.addTextChangedListener(clearError(tilEmail));
        etPassword.addTextChangedListener(clearError(tilPassword));
        etConfirmPassword.addTextChangedListener(clearError(tilConfirmPassword));
    }

    private void attemptRegister() {
        if (!validateInputs()) return;

        String fullName = etFullName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        setLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser == null) {
                            setLoading(false);
                            return;
                        }

                        // 1. Cập nhật Display Name bên Firebase (fire-and-forget, không block flow)
                        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build();
                        firebaseUser.updateProfile(profileUpdate);

                        // 2. Upsert Profile bên Firestore với Firebase UID
                        User newUser = new User();
                        newUser.setId(firebaseUser.getUid());
                        newUser.setFull_name(fullName);

                        new UserService().upsertProfile(newUser, new AsyncCrudService.ItemCallback<User>() {
                            @Override
                            public void onSuccess(User data) {
                                // 3. Gửi email xác thực sau khi profile đã được tạo
                                firebaseUser.sendEmailVerification();
                                setLoading(false);
                                navigateToVerifyEmail(email);
                            }

                            @Override
                            public void onError(String error) {
                                setLoading(false);
                                Toast.makeText(RegisterActivity.this,
                                        "Không thể tạo hồ sơ: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
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
                String idToken = account.getIdToken();
                if (idToken == null) {
                    setLoading(false);
                    Toast.makeText(this,
                            "Không lấy được token Google. Hãy kiểm tra SHA-1 trong Firebase Console.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                firebaseAuthWithGoogle(idToken);
            } catch (ApiException e) {
                setLoading(false);
                String msg = "Google Sign-In thất bại (mã lỗi: " + e.getStatusCode() + ")";
                if (e.getStatusCode() == 10) {
                    msg = "Cấu hình Google Sign-In chưa đúng. Kiểm tra SHA-1 trong Firebase Console.";
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            syncGoogleProfileAndNavigate(firebaseUser);
                        } else {
                            setLoading(false);
                            Toast.makeText(this, "Xác thực Google thất bại", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Xác thực Google thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void syncGoogleProfileAndNavigate(FirebaseUser firebaseUser) {
        User profile = new User();
        profile.setId(firebaseUser.getUid());
        profile.setFull_name(firebaseUser.getDisplayName());
        profile.setAvatar_url(
                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null);

        new UserService().upsertProfile(profile, new AsyncCrudService.ItemCallback<User>() {
            @Override
            public void onSuccess(User data) {
                setLoading(false);
                navigateToMain();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                navigateToMain();
            }
        });
    }

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

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        btnGoogleRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? "Đang xử lý..." : "Đăng ký");
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
