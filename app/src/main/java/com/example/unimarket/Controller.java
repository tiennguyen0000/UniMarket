package com.example.unimarket;

import android.graphics.Color;
import android.os.Build;
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

import com.example.unimarket.pages.chat.ChatInboxBottomSheetFragment;

public class Controller extends AppCompatActivity {
	private static final int TAB_SELECTED_COLOR = 0xFF21409A;
	private static final int TAB_UNSELECTED_COLOR = 0xFF9CA3AF;

	private NavController navController;

	// 4 tab (Home - Search - Order - Profile)
	private android.view.View tabHome;
	private android.view.View tabSearch;
	private android.view.View tabOrders;
	private android.view.View tabProfile;
	// tab Posting
	private View imageViewMenu;
	private View fabChat;

	// ImageView
	private ImageView ivTabHome;
	private ImageView ivTabSearch;
	private ImageView ivTabOrders;
	private ImageView ivTabProfile;

	// Text View
	private TextView tvTabHome;
	private TextView tvTabSearch;
	private TextView tvTabOrders;
	private TextView tvTabProfile;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupLightSystemBars();
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

		navController.addOnDestinationChangedListener(
				(controller, destination, arguments) -> updateTabSelection(destination));

		// Setup original tab selections
		NavDestination currentDestination = navController.getCurrentDestination();
		if (currentDestination != null) {
			updateTabSelection(currentDestination);
		}
	}

	private void setupLightSystemBars() {
		getWindow().setStatusBarColor(Color.WHITE);
		getWindow().setNavigationBarColor(Color.WHITE);

		int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
		}
		getWindow().getDecorView().setSystemUiVisibility(flags);
	}

	private void initViews() {
		// Tab
		tabHome    = findViewById(R.id.tabHome);
		tabSearch  = findViewById(R.id.tabSearch);
		tabOrders  = findViewById(R.id.tabOrders);
		tabProfile = findViewById(R.id.tabProfile);

		// FAB "+" giữa bottom bar
		imageViewMenu = findViewById(R.id.imageViewMenu);
		fabChat = findViewById(R.id.fabChat);

		// Icon
		ivTabHome    = findViewById(R.id.ivTabHome);
		ivTabSearch  = findViewById(R.id.ivTabSearch);
		ivTabOrders  = findViewById(R.id.ivTabOrders);
		ivTabProfile = findViewById(R.id.ivTabProfile);

		// Text
		tvTabHome    = findViewById(R.id.tvTabHome);
		tvTabSearch  = findViewById(R.id.tvTabSearch);
		tvTabOrders  = findViewById(R.id.tvTabOrders);
		tvTabProfile = findViewById(R.id.tvTabProfile);
	}

	private void setupClicks() {
		tabHome.setOnClickListener(v -> navigateTo(R.id.homeFragment));
		tabSearch.setOnClickListener(v -> navigateTo(R.id.searchFragment));
		tabOrders.setOnClickListener(v -> navigateTo(R.id.ordersFragment));
		tabProfile.setOnClickListener(v -> navigateTo(R.id.profileFragment));
		imageViewMenu.setOnClickListener(v -> navigateTo(R.id.postListingFragment));
		fabChat.setOnClickListener(v -> showChatBox());
	}

	private void showChatBox() {
		if (getSupportFragmentManager().findFragmentByTag("unimarket_chat") != null) {
			return;
		}
		new ChatInboxBottomSheetFragment().show(getSupportFragmentManager(), "unimarket_chat");
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

		// Hide bottom navigation for specific screens
		View bottomNav = findViewById(R.id.bottomNavCard);
		if (destinationId == R.id.postListingFragment) {
			bottomNav.setVisibility(View.GONE);
			if (imageViewMenu != null) imageViewMenu.setVisibility(View.GONE);
			if (fabChat != null) fabChat.setVisibility(View.GONE);
		} else {
			bottomNav.setVisibility(View.VISIBLE);
			if (imageViewMenu != null) imageViewMenu.setVisibility(View.VISIBLE);
			if (fabChat != null) fabChat.setVisibility(View.VISIBLE);
		}

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
