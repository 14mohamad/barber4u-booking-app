package com.example.barber4u.barber;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber4u.R;
import com.example.barber4u.adapters.BarberAppointmentsAdapter;
import com.example.barber4u.models.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BarberAppointmentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progress;
    private TextView tvEmpty;

    private BarberAppointmentsAdapter adapter;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public BarberAppointmentsFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_barber_appointments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recyclerBarberAppointments);
        progress = view.findViewById(R.id.progressBarberAppointments);
        tvEmpty = view.findViewById(R.id.tvEmptyBarberAppointments);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BarberAppointmentsAdapter();
        recyclerView.setAdapter(adapter);

        loadAppointmentsForBarber();
    }

    private void setLoading(boolean isLoading) {
        progress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void loadAppointmentsForBarber() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Barber not logged in", Toast.LENGTH_SHORT).show();
            showEmpty(true);
            return;
        }

        String barberUid = auth.getCurrentUser().getUid();

        setLoading(true);
        showEmpty(false);

        db.collection("appointments")
                .whereEqualTo("barberId", barberUid)
                .addSnapshotListener((snapshot, e) -> {
                    setLoading(false);

                    if (e != null) {
                        Toast.makeText(requireContext(), "Failed to load appointments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        showEmpty(true);
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        adapter.setItems(new ArrayList<>());
                        showEmpty(true);
                        return;
                    }

                    List<Appointment> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String id = doc.getId();
                        String userEmail = doc.getString("userEmail");
                        String userId = doc.getString("userId");
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String status = doc.getString("status");
                        String branchName = doc.getString("branchName");
                        String barberName = doc.getString("barberName");

                        list.add(new Appointment(
                                id, userEmail, userId,
                                date, time, status,
                                branchName, barberName
                        ));
                    }

                    adapter.setItems(list);
                    showEmpty(false);
                });
    }
}
