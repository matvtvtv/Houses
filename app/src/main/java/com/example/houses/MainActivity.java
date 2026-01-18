package com.example.houses;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.houses.adapter.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        boolean isFirstRun = preferences.getBoolean("isFirstRun", true);
        if (isFirstRun) {
            startActivity(new Intent(this, RegistrationActivity.class));
            editor.putBoolean("isFirstRun", false);
            editor.apply();
        }

        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNavigation);

        viewPager.setAdapter(new ViewPagerAdapter(this));
        viewPager.setOffscreenPageLimit(3);

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_tasks) {
                viewPager.setCurrentItem(0, false);
                return true;
            }
            if (item.getItemId() == R.id.nav_chat) {
                viewPager.setCurrentItem(1, false);
                return true;
            }
            if (item.getItemId() == R.id.nav_settings) {
                viewPager.setCurrentItem(2, false);
                return true;
            }
            return false;
        });

        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        hideKeyboard();
                        if (position == 0) bottomNav.setSelectedItemId(R.id.nav_tasks);
                        if (position == 1) bottomNav.setSelectedItemId(R.id.nav_chat);
                        if (position == 2) bottomNav.setSelectedItemId(R.id.nav_settings);
                    }
                }
        );
    }


    public ViewPager2 getViewPager() {
        return viewPager;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View current = getCurrentFocus();

        if (imm == null) return;

        if (current != null) {
            imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
            current.clearFocus();
        } else {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
    }
}
