package com.example.barber4u.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class RoleMainActivity extends AppCompatActivity {

    public static final String EXTRA_USER_NAME = "extra_user_name";
    public static final String EXTRA_USER_EMAIL = "extra_user_email";

    private MaterialToolbar topAppBar;
    private BottomNavigationView bottomNav;

    private final UserRepository userRepo = new UserRepository();

    private String userName;
    private String userEmail;

    private Role role = null;

    private ListenerRegistration badgeListener;

    private enum Role {
        CUSTOMER, BARBER, ADMIN
    }

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
        if (FirebaseProvider.auth().getCurrentUser() == null) {
            goToLogin();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (badgeListener != null) {
            badgeListener.remove();
            badgeListener = null;
        }
    }

    private void loadRoleAndSetupUI(Bundle savedInstanceState) {
        FirebaseUser current = FirebaseProvider.auth().getCurrentUser();
        if (current == null) {
            goToLogin();
            return;
        }

        String uid = current.getUid();

        userRepo.getUserById(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(@NonNull DocumentSnapshot doc) {
                if (isFinishing() || isDestroyed()) return;

                role = parseRoleFromDoc(doc);

                if (userName == null) userName = doc.getString("name");
                if (userEmail == null) userEmail = doc.getString("email");

                setupBottomNavMenu(role);
                setupNavigationListener(role);

                // ✅ Badge listener (messages unseen)
                startMessagesBadgeListener(uid, role);

                if (savedInstanceState == null) {
                    Fragment start = getStartFragment(role);
                    replaceFragment(start);
                    selectStartMenuItem(role);
                    topAppBar.setTitle(getStartTitle(role));
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(RoleMainActivity.this,
                        "Failed to load role: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                goToLogin();
            }
        });
    }

    @NonNull
    private Role parseRoleFromDoc(@NonNull DocumentSnapshot doc) {
        String r = doc.getString("role");
        if (r == null) return Role.CUSTOMER;

        r = r.trim().toUpperCase();
        if (r.equals("ADMIN")) return Role.ADMIN;
        if (r.equals("BARBER")) return Role.BARBER;
        return Role.CUSTOMER;
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavMenu(@NonNull Role role) {
        Menu menu = bottomNav.getMenu();
        menu.clear();

        MenuInflater inflater = getMenuInflater();
        if (role == Role.CUSTOMER) {
            inflater.inflate(R.menu.menu_bottom_customer, menu);
        } else if (role == Role.BARBER) {
            inflater.inflate(R.menu.menu_barber_bottom_nav, menu);
        } else {
            inflater.inflate(R.menu.menu_admin_bottom_nav, menu);
        }
    }

    private void setupNavigationListener(@NonNull Role role) {
        bottomNav.setOnItemSelectedListener(null);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            Fragment selected = null;
            String title = "Barber4U";

            if (role == Role.CUSTOMER) {
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

            } else if (role == Role.BARBER) {
                if (id == R.id.nav_home) {
                    selected = createBarberHome();
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

    @NonNull
    private Fragment getStartFragment(@NonNull Role role) {
        if (role == Role.CUSTOMER) return createCustomerHome();
        if (role == Role.BARBER) return createBarberHome();
        return new AdminDashboardFragment();
    }

    private void selectStartMenuItem(@NonNull Role role) {
        if (role == Role.CUSTOMER) bottomNav.setSelectedItemId(R.id.nav_home);
        else if (role == Role.BARBER) bottomNav.setSelectedItemId(R.id.nav_home);
        else bottomNav.setSelectedItemId(R.id.admin_nav_dashboard);
    }

    @NonNull
    private String getStartTitle(@NonNull Role role) {
        if (role == Role.CUSTOMER) return "Home";
        if (role == Role.BARBER) return "Barber Home";
        return "Admin Home";
    }

    @NonNull
    private Fragment createCustomerHome() {
        HomeCustomerFragment fragment = new HomeCustomerFragment();
        Bundle args = new Bundle();
        args.putString("userName", userName);
        args.putString("userEmail", userEmail);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    private Fragment createBarberHome() {
        BarberDashboardFragment fragment = new BarberDashboardFragment();
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

    // ✅ מאפשר ל־MessagesFragment להעביר למסך תורים
    public void navigateToAppointmentsTab() {
        if (role == Role.CUSTOMER || role == Role.BARBER) {
            bottomNav.setSelectedItemId(R.id.nav_appointments);
        }
    }

    // ✅ Badge שמראה כמה הודעות לא נקראו
    private void startMessagesBadgeListener(@NonNull String uid, @NonNull Role role) {
        int messagesMenuId = (role == Role.ADMIN) ? R.id.admin_nav_messages : R.id.nav_messages;

        Query q = FirebaseProvider.db()
                .collection("users")
                .document(uid)
                .collection("messages")
                .whereEqualTo("seen", false);

        if (badgeListener != null) {
            badgeListener.remove();
            badgeListener = null;
        }

        badgeListener = q.addSnapshotListener((snap, e) -> {
            if (isFinishing() || isDestroyed()) return;

            int count = (snap == null) ? 0 : snap.size();

            BadgeDrawable badge = bottomNav.getOrCreateBadge(messagesMenuId);
            if (count <= 0) {
                badge.setVisible(false);
            } else {
                badge.setVisible(true);
                badge.setNumber(Math.min(count, 99));
            }
        });
    }
}