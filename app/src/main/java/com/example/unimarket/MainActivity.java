package com.example.unimarket;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Temporary flow: bypass auth/onboarding and open controller directly.
        startActivity(new Intent(this, Controller.class));
        finish();
    }
}