package com.example.unimarket;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

public class Controller extends AppCompatActivity {
	private static final int TAB_SELECTED_COLOR = 0xFF21409A;
	private static final int TAB_UNSELECTED_COLOR = 0xFF9CA3AF;

	private NavController navController;

	private android.view.View tabHome;
	private android.view.View tabSearch;
	private android.view.View tabOrders;
	private android.view.View tabProfile;

	private ImageView ivTabHome;
	private ImageView ivTabSearch;
	private ImageView ivTabOrders;
	private ImageView ivTabProfile;

	private TextView tvTabHome;
	private TextView tvTabSearch;
	private TextView tvTabOrders;
	private TextView tvTabProfile;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_controller);

		initViews();

        // NavHostFragment (include fragment Home - Search - Order - Profile)
		NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
				.findFragmentById(R.id.controllerNavHost);

		if (navHostFragment == null) {
			return;
		}

		navController = navHostFragment.getNavController();
		setupClicks();


		navController.addOnDestinationChangedListener((controller, destination, arguments) ->
				updateTabSelection(destination)
		);

        // Setup original tab selections
		NavDestination currentDestination = navController.getCurrentDestination();
		if (currentDestination != null) {
			updateTabSelection(currentDestination);
		}
	}

	private void initViews() {
        // Tab
		tabHome = findViewById(R.id.tabHome);
		tabSearch = findViewById(R.id.tabSearch);
		tabOrders = findViewById(R.id.tabOrders);
		tabProfile = findViewById(R.id.tabProfile);

        // Icon
		ivTabHome = findViewById(R.id.ivTabHome);
		ivTabSearch = findViewById(R.id.ivTabSearch);
		ivTabOrders = findViewById(R.id.ivTabOrders);
		ivTabProfile = findViewById(R.id.ivTabProfile);

        // Text
		tvTabHome = findViewById(R.id.tvTabHome);
		tvTabSearch = findViewById(R.id.tvTabSearch);
		tvTabOrders = findViewById(R.id.tvTabOrders);
		tvTabProfile = findViewById(R.id.tvTabProfile);
	}

	private void setupClicks() {
		tabHome.setOnClickListener(v -> navigateTo(R.id.homeFragment));
		tabSearch.setOnClickListener(v -> navigateTo(R.id.searchFragment));
		tabOrders.setOnClickListener(v -> navigateTo(R.id.ordersFragment));
		tabProfile.setOnClickListener(v -> navigateTo(R.id.profileFragment));
	}

	private void navigateTo(int destinationId) {
		if (navController == null) {
			return;
		}

		NavDestination currentDestination = navController.getCurrentDestination();
		if (currentDestination != null && currentDestination.getId() == destinationId) {
			return;
		}

		navController.navigate(destinationId);
	}

	private void updateTabSelection(NavDestination destination) {
		int destinationId = destination.getId();

		setTabSelected(ivTabHome, tvTabHome, destinationId == R.id.homeFragment);
		setTabSelected(ivTabSearch, tvTabSearch, destinationId == R.id.searchFragment);
		setTabSelected(ivTabOrders, tvTabOrders, destinationId == R.id.ordersFragment);
		setTabSelected(ivTabProfile, tvTabProfile, destinationId == R.id.profileFragment);
	}

	private void setTabSelected(ImageView icon, TextView label, boolean selected) {
		int color = selected ? TAB_SELECTED_COLOR : TAB_UNSELECTED_COLOR;
		icon.setColorFilter(color);
		label.setTextColor(color);
	}
}
