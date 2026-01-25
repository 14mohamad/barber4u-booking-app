package com.example.barber4u.common;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.barber4u.R;
import com.example.barber4u.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "barber4u_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    public SettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    private void performLogout() {
        FirebaseAuth.getInstance().signOut();

        Intent i = new Intent(requireContext(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SwitchMaterial switchDarkMode = view.findViewById(R.id.switchDarkMode);
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        boolean isDark = prefs.getBoolean(KEY_DARK_MODE, false);
        switchDarkMode.setChecked(isDark);

        switchDarkMode.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, checked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            requireActivity().recreate();
        });

        btnLogout.setOnClickListener(v -> {
            String uid = FirebaseAuth.getInstance().getUid();

            if (uid == null) {
                performLogout();
                return;
            }

            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(token -> {
                        if (token != null && !token.trim().isEmpty()) {
                            FirebaseFirestore db = FirebaseFirestore.getInstance();

                            // 1️⃣ Remove token from subcollection
                            db.collection("users")
                                    .document(uid)
                                    .collection("fcmTokens")
                                    .document(token)
                                    .delete();

                            // 2️⃣ Optional: remove legacy field
                            db.collection("users")
                                    .document(uid)
                                    .update("fcmToken", FieldValue.delete());
                        }

                        performLogout();
                    })
                    .addOnFailureListener(e -> performLogout());
        });
    }
}