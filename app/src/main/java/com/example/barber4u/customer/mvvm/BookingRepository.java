package com.example.barber4u.customer.mvvm;

import com.example.barber4u.models.Barber;
import com.example.barber4u.models.GalleryItem;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookingRepository {

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(Exception e);
    }

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public BookingRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void loadBarbersForBranch(String branchId, ResultCallback<List<Barber>> cb) {
        db.collection("barbers")
                .whereArrayContains("branchIds", branchId)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Barber> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Barber b = doc.toObject(Barber.class);
                        b.setUid(doc.getId());
                        list.add(b);
                    }

                    Collections.sort(list, Comparator.comparing(x -> {
                        String n = x.getName();
                        return n == null ? "" : n.toLowerCase(Locale.ROOT);
                    }));

                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public void loadGalleryForBarber(String branchId, String barberId, ResultCallback<List<GalleryItem>> cb) {
        db.collection("gallery_items")
                .whereEqualTo("active", true)
                .whereEqualTo("branchId", branchId)
                .whereEqualTo("barberId", barberId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<GalleryItem> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        GalleryItem g = doc.toObject(GalleryItem.class);
                        g.setId(doc.getId());
                        list.add(g);
                    }

                    Collections.sort(list, Comparator.comparing(x -> {
                        String t = x.getTitle();
                        return t == null ? "" : t.toLowerCase(Locale.ROOT);
                    }));

                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onError);
    }

    public void createAppointment(
            String branchId,
            String branchName,
            Barber barber,
            String date,
            String time,
            String notes,
            GalleryItem selectedGalleryItem,
            ResultCallback<Void> cb
    ) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            cb.onError(new IllegalStateException("User not logged in"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUid());
        data.put("userEmail", user.getEmail());

        data.put("branchId", branchId);
        data.put("branchName", branchName);

        data.put("barberId", barber.getUid());
        data.put("barberName", barber.getName());

        data.put("date", date);
        data.put("time", time);
        data.put("notes", notes);

        if (selectedGalleryItem != null) {
            data.put("galleryItemId", selectedGalleryItem.getId());
            data.put("galleryTitle", selectedGalleryItem.getTitle());
            data.put("galleryImageUrl", selectedGalleryItem.getImageUrl());
        }

        data.put("status", "PENDING");
        data.put("createdAt", Timestamp.now());
        data.put("updatedAt", Timestamp.now());

        db.collection("appointments")
                .add(data)
                .addOnSuccessListener(docRef -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }
}