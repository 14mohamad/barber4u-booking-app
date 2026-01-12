package com.example.barber4u.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.barber4u.R;
import com.example.barber4u.data.firebase.FirebaseProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardFragment extends Fragment {

    private FirebaseFirestore db;

    private TextView tvBranches, tvBarbers, tvUsers, tvPending;

    public AdminDashboardFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseProvider.db();

        tvBranches = view.findViewById(R.id.tvStatBranches);
        tvBarbers  = view.findViewById(R.id.tvStatBarbers);
        tvUsers    = view.findViewById(R.id.tvStatUsers);
        tvPending  = view.findViewById(R.id.tvStatPending);

        loadStats();
    }

    private void loadStats() {
        db.collection("branches").get()
                .addOnSuccessListener(snap -> tvBranches.setText("Branches: " + snap.size()));

        db.collection("barbers").get()
                .addOnSuccessListener(snap -> tvBarbers.setText("Barbers: " + snap.size()));

        db.collection("users").get()
                .addOnSuccessListener(snap -> tvUsers.setText("Users: " + snap.size()));

        db.collection("appointments")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(snap -> tvPending.setText("Pending appointments: " + snap.size()));
    }
}