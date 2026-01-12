package com.example.barber4u.auth;

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

import com.example.barber4u.R;
import com.example.barber4u.data.firebase.FirebaseProvider;
import com.example.barber4u.data.repositories.UserRepository;
import com.example.barber4u.main.RoleMainActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public class LoginActivity extends AppCompatActivity {

    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin, btnGoToRegister;
    private ProgressBar progressBarLogin;

    private final UserRepository userRepo = new UserRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        progressBarLogin = findViewById(R.id.progressBarLogin);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> performLogin());
        btnGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
        );
    }

    private void performLogin() {
        String email = etLoginEmail.getText().toString().trim();
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

        FirebaseProvider.auth()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, this::handleLoginResult);
    }

    private void handleLoginResult(@NonNull Task<AuthResult> task) {
        if (!task.isSuccessful()) {
            setLoading(false);
            String message = task.getException() != null
                    ? task.getException().getMessage()
                    : "Login failed";
            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            return;
        }

        String uid = task.getResult().getUser().getUid();
        fetchUserAndGo(uid);
    }

    private void fetchUserAndGo(String uid) {
        userRepo.getUserById(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(@NonNull com.google.firebase.firestore.DocumentSnapshot doc) {
                setLoading(false);

                String name = doc.getString("name");
                String email = doc.getString("email");

                Intent intent = new Intent(LoginActivity.this, RoleMainActivity.class);
                intent.putExtra(RoleMainActivity.EXTRA_USER_NAME, name);
                intent.putExtra(RoleMainActivity.EXTRA_USER_EMAIL, email);

                startActivity(intent);
                finish();
            }

            @Override
            public void onError(@NonNull Exception e) {
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Failed to load user profile: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBarLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        btnGoToRegister.setEnabled(!isLoading);
    }
}