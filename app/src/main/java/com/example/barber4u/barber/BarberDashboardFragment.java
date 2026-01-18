package com.example.barber4u.barber;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.barber4u.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class BarberDashboardFragment extends Fragment {

    private TextView tvBarberWelcome;
    private TextView tvBarberRating;
    private Button btnOpenAddGallery;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ListenerRegistration ratingListener;

    public BarberDashboardFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_barber_dashboard, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvBarberWelcome = v.findViewById(R.id.tvBarberWelcome);
        tvBarberRating = v.findViewById(R.id.tvBarberRating);
        btnOpenAddGallery = v.findViewById(R.id.btnOpenAddGallery);

        String name = (getArguments() != null) ? getArguments().getString("userName") : null;
        String email = (getArguments() != null) ? getArguments().getString("userEmail") : null;

        String text = "Welcome Barber";
        if (name != null && !name.trim().isEmpty()) text += "\n" + name;
        if (email != null && !email.trim().isEmpty()) text += "\n" + email;
        tvBarberWelcome.setText(text);

        btnOpenAddGallery.setOnClickListener(view -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.roleContainer, new BarberAddGalleryItemFragment())
                    .addToBackStack(null)
                    .commit();
        });

        tvBarberRating.setText("Rating: --");

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        startRatingListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (ratingListener != null) {
            ratingListener.remove();
            ratingListener = null;
        }
    }

    private void startRatingListener() {
        if (auth.getCurrentUser() == null) {
            tvBarberRating.setText("Rating: --");
            return;
        }

        String barberId = auth.getCurrentUser().getUid();

        ratingListener = db.collection("barbers")
                .document(barberId)
                .addSnapshotListener((snapshot, e) -> {
                    if (!isAdded()) return;

                    if (e != null || snapshot == null || !snapshot.exists()) {
                        tvBarberRating.setText("Rating: --");
                        return;
                    }

                    showRatingFromDoc(snapshot);
                });
    }

    private void showRatingFromDoc(@NonNull DocumentSnapshot doc) {
        Long countL = doc.getLong("ratingCount");
        Long sumL = doc.getLong("ratingSum");

        long count = (countL == null) ? 0 : countL;
        long sum = (sumL == null) ? 0 : sumL;

        if (count <= 0) {
            tvBarberRating.setText("No ratings yet");
            return;
        }

        double avg = (double) sum / (double) count;

        // Example: "4.6 ★ (12)"
        String text = String.format(Locale.ENGLISH, "%.1f ★ (%d)", avg, count);
        tvBarberRating.setText(text);
    }
}
