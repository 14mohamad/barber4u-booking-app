// ==================================
// BarberAppointmentsFragment.java  (Barber)
// ==================================
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BarberAppointmentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progress;
    private TextView tvEmpty;

    private BarberAppointmentsAdapter adapter;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ListenerRegistration appointmentsListener;

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

    private void updateStatus(@Nullable String appointmentId, @NonNull String newStatus) {
        if (appointmentId == null || appointmentId.isEmpty()) return;

        // ✅ מעדכנים גם updatedAt (מועיל לסידור עתידי/לוגיקה)
        db.collection("appointments")
                .document(appointmentId)
                .update(
                        "status", newStatus,
                        "updatedAt", Timestamp.now()
                )
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(),
                            "Failed to update status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recyclerBarberAppointments);
        progress = view.findViewById(R.id.progressBarberAppointments);
        tvEmpty = view.findViewById(R.id.tvEmptyBarberAppointments);

        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        adapter = new BarberAppointmentsAdapter(new BarberAppointmentsAdapter.Listener() {
            @Override
            public void onApprove(@NonNull Appointment appt) {
                updateStatus(appt.getId(), "APPROVED");
            }

            @Override
            public void onCancel(@NonNull Appointment appt) {
                updateStatus(appt.getId(), "CANCELED");
            }

            @Override
            public void onDone(@NonNull Appointment appt) {
                updateStatus(appt.getId(), "DONE");
                adapter.removeById(appt.getId());
            }
        });

        recyclerView.setAdapter(adapter);

        showEmpty(true);
        setLoading(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        startListeningAppointments();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopListeningAppointments();
    }

    private void setLoading(boolean isLoading) {
        if (progress == null) return;
        progress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(boolean show) {
        if (tvEmpty != null) tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    private boolean isTodayOrFuture(@NonNull String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setLenient(false);

            Date apptDate = sdf.parse(dateStr);
            if (apptDate == null) return false;

            Calendar apptCal = Calendar.getInstance();
            apptCal.setTime(apptDate);
            apptCal.set(Calendar.HOUR_OF_DAY, 0);
            apptCal.set(Calendar.MINUTE, 0);
            apptCal.set(Calendar.SECOND, 0);
            apptCal.set(Calendar.MILLISECOND, 0);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            return !apptCal.before(today); // today or future
        } catch (Exception e) {
            return false;
        }
    }
    @NonNull
    private static String safe(@Nullable String s) {
        return (s == null) ? "" : s.trim();
    }

    private void startListeningAppointments() {
        if (auth.getCurrentUser() == null) {
            if (isAdded()) Toast.makeText(getContext(), "Barber not logged in", Toast.LENGTH_SHORT).show();
            adapter.setItems(new ArrayList<>());
            showEmpty(true);
            return;
        }

        String barberUid = auth.getCurrentUser().getUid();

        stopListeningAppointments();

        setLoading(true);
        showEmpty(false);

        // ✅ חדש למעלה לפי createdAt
        appointmentsListener = db.collection("appointments")
                .whereEqualTo("barberId", barberUid)
                .addSnapshotListener((snapshot, e) -> {
                    if (!isAdded()) return;

                    setLoading(false);

                    if (e != null) {
                        Toast.makeText(getContext(),
                                "Failed to load appointments: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        adapter.setItems(new ArrayList<>());
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

                        String status = safe(doc.getString("status"));
                        String date   = safe(doc.getString("date"));

                        // 1️⃣ Never show DONE
                        if (status.equalsIgnoreCase("DONE")) {
                            continue;
                        }

                        // 2️⃣ Hide past appointments that are still pending/approved
                        if ((status.equalsIgnoreCase("PENDING")
                                || status.equalsIgnoreCase("APPROVED"))
                                && !isTodayOrFuture(date)) {
                            continue;
                        }

                        String id = doc.getId();
                        String userEmail = safe(doc.getString("userEmail"));
                        String userId = safe(doc.getString("userId"));
                        String time = safe(doc.getString("time"));
                        String branchName = safe(doc.getString("branchName"));
                        String barberName = safe(doc.getString("barberName"));

                        list.add(new Appointment(
                                id,
                                userEmail,
                                userId,
                                date,
                                time,
                                status,
                                branchName,
                                barberName
                        ));
                    }


                    adapter.setItems(list);
                    showEmpty(list.isEmpty());
                });
    }

    private void stopListeningAppointments() {
        if (appointmentsListener != null) {
            appointmentsListener.remove();
            appointmentsListener = null;
        }
    }
}