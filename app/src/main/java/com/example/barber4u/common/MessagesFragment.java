package com.example.barber4u.common;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber4u.R;
import com.example.barber4u.adapters.MessagesAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessagesFragment extends Fragment implements MessagesAdapter.Listener {

    // UI
    private RecyclerView recyclerMessages;
    private ProgressBar progressMessages;
    private TextView tvNoMessages;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Adapter
    private MessagesAdapter adapter;

    // Firestore listener (so we can remove it and avoid "not attached" crashes)
    private ListenerRegistration messagesListener;

    public MessagesFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerMessages = view.findViewById(R.id.recyclerMessages);
        progressMessages = view.findViewById(R.id.progressMessages);
        tvNoMessages = view.findViewById(R.id.tvNoMessages);

        recyclerMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MessagesAdapter(this);
        recyclerMessages.setAdapter(adapter);

        setLoading(false);
        showEmpty(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        // ✅ prevent callbacks after fragment is detached
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    private void startListening() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            adapter.setItems(new ArrayList<>());
            showEmpty(true);
            return;
        }

        setLoading(true);
        showEmpty(false);

        Query q = db.collection("users")
                .document(user.getUid())
                .collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING);

        messagesListener = q.addSnapshotListener((snapshot, e) -> {
            if (!isAdded()) return;

            setLoading(false);

            if (e != null) {
                Toast.makeText(requireContext(),
                        "Failed to load messages: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                adapter.setItems(new ArrayList<>());
                showEmpty(true);
                return;
            }

            if (snapshot == null || snapshot.isEmpty()) {
                adapter.setItems(new ArrayList<>());
                showEmpty(true);
                return;
            }

            List<MessageItem> items = new ArrayList<>();
            snapshot.getDocuments().forEach(doc -> {
                String text = doc.getString("text");
                String appointmentId = doc.getString("appointmentId");
                String barberId = doc.getString("barberId");
                String barberName = doc.getString("barberName");

                items.add(new MessageItem(
                        doc.getId(),
                        text == null ? "" : text,
                        appointmentId == null ? "" : appointmentId,
                        barberId == null ? "" : barberId,
                        barberName == null ? "" : barberName
                ));
            });

            adapter.setItems(items);
            showEmpty(items.isEmpty());
        });
    }

    private void setLoading(boolean isLoading) {
        progressMessages.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(boolean show) {
        tvNoMessages.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerMessages.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // --------------------------
    // Adapter callbacks
    // --------------------------

    @Override
    public void onDismiss(@NonNull MessageItem item) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("messages")
                .document(item.id)
                .delete();
    }

    @Override
    public void onRateNow(@NonNull MessageItem msg) {
        showRatingDialog(msg);
    }
    private void showRatingDialog(@NonNull MessageItem msg) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_rating, null);

        RatingBar ratingBar = view.findViewById(R.id.ratingBar);

        new AlertDialog.Builder(requireContext())
                .setTitle("Rate " + msg.barberName)
                .setView(view)
                .setPositiveButton("Submit", (d, w) -> {
                    int rating = (int) ratingBar.getRating();
                    if (rating > 0) {
                        submitRating(msg, rating);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitRating(@NonNull MessageItem msg, int rating) {
        WriteBatch batch = db.batch();

        DocumentReference apptRef = db.collection("appointments").document(msg.appointmentId);
        batch.update(apptRef, Map.of(
                "rating", rating,
                "ratedAt", FieldValue.serverTimestamp()
        ));

        DocumentReference msgRef = db.collection("users")
                .document(FirebaseAuth.getInstance().getUid())
                .collection("messages")
                .document(msg.id);
        batch.delete(msgRef);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Thanks for your rating!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to submit rating: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
