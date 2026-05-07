package com.example.unimarket.auth;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OnboardingAdapter extends FragmentStateAdapter {
    // final -> no changes after initialization

    private final String[] titles;
    private final String[] descs;
    private final int[] images;

    // Constructor
    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity, String[] titles, String[] descs, int[] images) {
        super(fragmentActivity);
        this.titles = titles;
        this.descs = descs;
        this.images = images;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return OnboardingPageFragment.newInstance(titles[position], descs[position], images[position]);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }
}
