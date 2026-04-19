package com.example.unimarket;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.unimarket.auth.LoginActivity;
import com.example.unimarket.auth.OnboardingAdapter;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREF_NAME = "UniMarketPrefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";

    private ViewPager2 viewPager;
    private Button btnStart;
    private TextView btnSkip;
    private View dot1, dot2, dot3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        initViews();
        setupViewPager();
        setupClicks();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        btnStart = findViewById(R.id.btnStart);
        btnSkip = findViewById(R.id.btnSkip);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
    }

    private void setupViewPager() {
        String[] titles = {
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_title_3)
        };
        String[] descs = {
                getString(R.string.onboarding_desc_1),
                getString(R.string.onboarding_desc_2),
                getString(R.string.onboarding_desc_3)
        };
        int[] images = {
                R.drawable.shopping_cart,
                R.drawable.shopping_cart,
                R.drawable.shopping_cart
        };

        OnboardingAdapter adapter = new OnboardingAdapter(this, titles, descs, images);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicators(position);
                if (position == 2) {
                    btnStart.setText(R.string.onboarding_btn_start);
                    btnSkip.setVisibility(View.INVISIBLE);
                } else {
                    btnStart.setText(R.string.onboarding_btn_next);
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void updateIndicators(int position) {
        dot1.setBackgroundResource(position == 0 ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
        dot2.setBackgroundResource(position == 1 ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
        dot3.setBackgroundResource(position == 2 ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
    }

    private void setupClicks() {
        btnStart.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < 2) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                completeOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> completeOnboarding());
    }

    private void completeOnboarding() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply();
        navigateToLogin();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
