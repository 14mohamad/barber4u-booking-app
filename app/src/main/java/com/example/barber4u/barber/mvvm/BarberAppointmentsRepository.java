package com.example.barber4u.barber.mvvm;

import androidx.annotation.Nullable;

import com.example.barber4u.models.Appointment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class BarberAppointmentsRepository {

    public interface AppointmentsCallback {
        void onData(List<Appointment> items);
        void onError(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public @Nullable String getCurrentBarberUid() {
        return auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
    }

    public ListenerRegistration listenAppointments(String barberUid, AppointmentsCallback cb) {
        return db.collection("appointments")
                .whereEqualTo("barberId", barberUid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        cb.onError(e);
                        return;
                    }
                    if (snapshot == null || snapshot.isEmpty()) {
                        cb.onData(new ArrayList<>());
                        return;
                    }

                    List<Appointment> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        list.add(mapDocToAppointment(doc));
                    }
                    cb.onData(list);
                });
    }

    private Appointment mapDocToAppointment(DocumentSnapshot doc) {
        String id = doc.getId();
        String userEmail = safe(doc.getString("userEmail"));
        String userId = safe(doc.getString("userId"));
        String date = safe(doc.getString("date"));
        String time = safe(doc.getString("time"));
        String status = safe(doc.getString("status"));
        String branchName = safe(doc.getString("branchName"));
        String barberName = safe(doc.getString("barberName"));

        return new Appointment(id, userEmail, userId, date, time, status, branchName, barberName);
    }

    public void updateStatus(String appointmentId, String newStatus, SimpleCallback cb) {
        if (appointmentId == null || appointmentId.trim().isEmpty()) return;

        db.collection("appointments")
                .document(appointmentId)
                .update("status", newStatus, "updatedAt", Timestamp.now())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(cb::onError);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}