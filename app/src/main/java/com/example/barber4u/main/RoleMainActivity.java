package com.example.barber4u.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.barber4u.R;
import com.example.barber4u.admin.AdminDashboardFragment;
import com.example.barber4u.admin.AdminManageFragment;
import com.example.barber4u.auth.LoginActivity;
import com.example.barber4u.barber.BarberAppointmentsFragment;
import com.example.barber4u.barber.BarberDashboardFragment;
import com.example.barber4u.common.MessagesFragment;
import com.example.barber4u.common.SettingsFragment;
import com.example.barber4u.customer.AppointmentsFragment;
import com.example.barber4u.customer.BookFragment;
import com.example.barber4u.customer.HomeCustomerFragment;
import com.example.barber4u.data.firebase.FirebaseProvider;
import com.example.barber4u.data.repositories.UserRepository;
import com.example.barber4u.models.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;

public class RoleMainActivity extends AppCompatActivity {

    public static final String EXTRA_USER_NAME = "extra_user_name";
    public static final String EXTRA_USER_EMAIL = "extra_user_email";

    private MaterialToolbar topAppBar;
    private BottomNavigationView bottomNav;

    private final UserRepository userRepo = new UserRepository();

    private String userName;
    private String userEmail;
    private User.Role role = User.Role.CUSTOMER; // default safe

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_main);

        topAppBar = findViewById(R.id.topAppBarRole);
        bottomNav = findViewById(R.id.bottomNavRole);

        userName = getIntent().getStringExtra(EXTRA_USER_NAME);
        userEmail = getIntent().getStringExtra(EXTRA_USER_EMAIL);

        loadRoleAndSetupUI(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // אם עשית Log out וחזרת אחורה — לא לאפשר להישאר כאן
        if (FirebaseProvider.auth().getCurrentUser() == null) {
            goToLogin();
        }
    }

    private void loadRoleAndSetupUI(Bundle savedInstanceState) {
        FirebaseUser current = FirebaseProvider.auth().getCurrentUser();
        if (current == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            goToLogin();
            return;
        }

        String uid = current.getUid();

        userRepo.getUserById(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(@NonNull com.google.firebase.firestore.DocumentSnapshot doc) {
                role = userRepo.parseRole(doc);

                // fallback אם לא הגיעו extras מה-LoginActivity
                if (userName == null) userName = doc.getString("name");
                if (userEmail == null) userEmail = doc.getString("email");

                setupBottomNavMenu(role);

                if (savedInstanceState == null) {
                    Fragment start = getStartFragment(role);
                    replaceFragment(start);

                    selectStartMenuItem(role);
                    topAppBar.setTitle(getStartTitle(role));
                }

                setupNavigationListener(role);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(RoleMainActivity.this,
                        "Failed to load role: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                goToLogin();
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavMenu(User.Role role) {
        Menu menu = bottomNav.getMenu();
        menu.clear();

        MenuInflater inflater = getMenuInflater();
        if (role == User.Role.CUSTOMER) {
            inflater.inflate(R.menu.menu_bottom_customer, menu);
        } else if (role == User.Role.BARBER) {
            inflater.inflate(R.menu.menu_barber_bottom_nav, menu);
        } else { // ADMIN
            inflater.inflate(R.menu.menu_admin_bottom_nav, menu);
        }
    }

    private void setupNavigationListener(User.Role role) {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            Fragment selected = null;
            String title = "Barber4U";

            if (role == User.Role.CUSTOMER) {
                if (id == R.id.nav_home) {
                    selected = createCustomerHome();
                    title = "Home";
                } else if (id == R.id.nav_appointments) {
                    selected = new AppointmentsFragment();
                    title = "My Appointments";
                } else if (id == R.id.nav_book) {
                    selected = new BookFragment();
                    title = "New Appointment";
                } else if (id == R.id.nav_messages) {
                    selected = new MessagesFragment();
                    title = "Messages";
                } else if (id == R.id.nav_settings) {
                    selected = new SettingsFragment();
                    title = "Settings";
                }

            } else if (role == User.Role.BARBER) {
                if (id == R.id.nav_home) {
                    BarberDashboardFragment fragment = new BarberDashboardFragment();
                    Bundle args = new Bundle();
                    args.putString("userName", userName);
                    args.putString("userEmail", userEmail);
                    fragment.setArguments(args);
                    selected = fragment;
                    title = "Barber Home";
                } else if (id == R.id.nav_appointments) {
                    selected = new BarberAppointmentsFragment();
                    title = "Appointments";
                } else if (id == R.id.nav_messages) {
                    selected = new MessagesFragment();
                    title = "Messages";
                } else if (id == R.id.nav_settings) {
                    selected = new SettingsFragment();
                    title = "Settings";
                }

            } else { // ADMIN
                if (id == R.id.admin_nav_dashboard) {
                    selected = new AdminDashboardFragment();
                    title = "Admin Home";
                } else if (id == R.id.admin_nav_manage) {
                    selected = new AdminManageFragment();
                    title = "Manage";
                } else if (id == R.id.admin_nav_messages) {
                    selected = new MessagesFragment();
                    title = "Messages";
                } else if (id == R.id.admin_nav_settings) {
                    selected = new SettingsFragment();
                    title = "Settings";
                }
            }

            if (selected != null) {
                replaceFragment(selected);
                topAppBar.setTitle(title);
                return true;
            }

            return false;
        });
    }

    private Fragment getStartFragment(User.Role role) {
        if (role == User.Role.CUSTOMER) return createCustomerHome();
        if (role == User.Role.BARBER) {
            BarberDashboardFragment fragment = new BarberDashboardFragment();
            Bundle args = new Bundle();
            args.putString("userName", userName);
            args.putString("userEmail", userEmail);
            fragment.setArguments(args);
            return fragment;
        }
        return new AdminDashboardFragment();
    }

    private void selectStartMenuItem(User.Role role) {
        if (role == User.Role.CUSTOMER) bottomNav.setSelectedItemId(R.id.nav_home);
        else if (role == User.Role.BARBER) bottomNav.setSelectedItemId(R.id.nav_home);
        else bottomNav.setSelectedItemId(R.id.admin_nav_dashboard);
    }

    private String getStartTitle(User.Role role) {
        if (role == User.Role.CUSTOMER) return "Home";
        if (role == User.Role.BARBER) return "Barber Home";
        return "Admin Home";
    }

    private Fragment createCustomerHome() {
        HomeCustomerFragment fragment = new HomeCustomerFragment();
        Bundle args = new Bundle();
        args.putString("userName", userName);
        args.putString("userEmail", userEmail);
        fragment.setArguments(args);
        return fragment;
    }

    private void replaceFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.roleContainer, fragment)
                .commit();
    }
}