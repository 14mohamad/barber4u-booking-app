package com.example.barber4u;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CustomerHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private MaterialToolbar topAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);

        topAppBar = findViewById(R.id.topAppBar);
        bottomNav = findViewById(R.id.bottomNav);

        // ברירת מחדל – מסך הבית של הלקוח
        if (savedInstanceState == null) {
            replaceFragment(new HomeCustomerFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
            topAppBar.setTitle("Home");
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            String title = "Barber4U";

            int id = item.getItemId();
            if (id == R.id.nav_home) {
                selected = new HomeCustomerFragment();
                title = "Home";
            } else if (id == R.id.nav_appointments) {
                selected = new AppointmentsFragment();
                title = "My Appointments";
            } else if (id == R.id.nav_book) {
                selected = new BookFragment();      // תור חדש
                title = "Book a Slot";
            } else if (id == R.id.nav_messages) {
                selected = new MessagesFragment();  // מסך הודעות חדש
                title = "Messages";
            } else if (id == R.id.nav_settings) {
                selected = new SettingsFragment();
                title = "Settings";
            }

            if (selected != null) {
                replaceFragment(selected);
                topAppBar.setTitle(title);
                return true;
            }
            return false;
        });
    }

    private void replaceFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
