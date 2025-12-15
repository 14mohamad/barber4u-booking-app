package com.example.barber4u;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class BarberHomeActivity extends AppCompatActivity {

    private TextView tvTitle;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_home);

        tvTitle = findViewById(R.id.tvBarberTitle);
        btnLogout = findViewById(R.id.btnBarberLogout);

        tvTitle.setText("Barber Home");

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(BarberHomeActivity.this, LoginActivity.class));
            finish();
        });
    }
}
