package com.example.smartirrigationmobile;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.smartirrigationmobile.fragment.ControlFragment;
import com.example.smartirrigationmobile.fragment.DashboardFragment;
import com.example.smartirrigationmobile.fragment.HistoryFragment;
import com.example.smartirrigationmobile.fragment.WeatherFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                openFragment(new DashboardFragment());
                return true;
            } else if (itemId == R.id.nav_control) {
                openFragment(new ControlFragment());
                return true;
            } else if (itemId == R.id.nav_weather) {
                openFragment(new WeatherFragment());
                return true;
            } else if (itemId == R.id.nav_history) {
                openFragment(new HistoryFragment());
                return true;
            }

            return false;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
        }
    }

    private void openFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
