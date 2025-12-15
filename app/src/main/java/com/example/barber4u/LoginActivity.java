package com.example.barber4u;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin, btnGoToRegister;
    private ProgressBar progressBarLogin;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etLoginEmail      = findViewById(R.id.etLoginEmail);
        etLoginPassword   = findViewById(R.id.etLoginPassword);
        btnLogin          = findViewById(R.id.btnLogin);
        btnGoToRegister   = findViewById(R.id.btnGoToRegister);
        progressBarLogin  = findViewById(R.id.progressBarLogin);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> performLogin());

        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String email    = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etLoginEmail.setError("Email is required");
            etLoginEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etLoginPassword.setError("Password is required");
            etLoginPassword.requestFocus();
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, this::handleLoginResult);
    }

    private void handleLoginResult(@NonNull Task<AuthResult> task) {
        if (task.isSuccessful()) {
            String uid = task.getResult().getUser().getUid();
            fetchUserAndRoute(uid);
        } else {
            setLoading(false);
            String message = task.getException() != null
                    ? task.getException().getMessage()
                    : "Login failed";
            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void fetchUserAndRoute(String uid) {
        setLoading(true);

        db.collection("users")      // <-- כאן התיקון: db במקום firestore
                .document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    setLoading(false);

                    if (!task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Failed to load user profile",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    DocumentSnapshot doc = task.getResult();
                    if (doc == null || !doc.exists()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "User profile not found",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String roleString = doc.getString("role");

                    if (roleString == null) {
                        Toast.makeText(
                                LoginActivity.this,
                                "User role is missing",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    User.Role role;
                    try {
                        role = User.Role.valueOf(roleString);
                    } catch (IllegalArgumentException ex) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Unknown role: " + roleString,
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    Toast.makeText(
                            LoginActivity.this,
                            "Logged in as " + role.name(),
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent;
                    switch (role) {
                        case BARBER:
                            intent = new Intent(LoginActivity.this, BarberHomeActivity.class);
                            break;
                        case ADMIN:
                            intent = new Intent(LoginActivity.this, AdminHomeActivity.class);
                            break;
                        case CUSTOMER:
                        default:
                            intent = new Intent(LoginActivity.this, CustomerHomeActivity.class);
                            break;
                    }

                    intent.putExtra("userName", name);
                    intent.putExtra("userEmail", email);

                    startActivity(intent);
                    finish();
                });
    }

    private void setLoading(boolean isLoading) {
        progressBarLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        btnGoToRegister.setEnabled(!isLoading);
    }
}
