package com.example.unimarket;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unimarket.auth.LoginActivity;
import com.example.unimarket.auth.VerifyEmailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isOnboardingCompleted()) {
            startActivity(new Intent(this, OnboardingActivity.class));
        } else if (!isUserLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
        } else if (shouldCompleteEmailVerification()) {
            startActivity(new Intent(this, VerifyEmailActivity.class));
        } else {
            startActivity(new Intent(this, Controller.class));
        }
        finish();
    }

    private boolean isOnboardingCompleted() {
        SharedPreferences prefs = getSharedPreferences("UniMarketPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("onboarding_completed", false);
    }

    private boolean isUserLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    private boolean shouldCompleteEmailVerification() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        for (UserInfo info : currentUser.getProviderData()) {
            if ("google.com".equals(info.getProviderId())) {
                return false;
            }
        }

        return !currentUser.isEmailVerified();
    }
}
