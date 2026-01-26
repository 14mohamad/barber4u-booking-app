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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber4u.R;
import com.example.barber4u.adapters.MessagesAdapter;

import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment implements MessagesAdapter.Listener {

    private RecyclerView recyclerMessages;
    private ProgressBar progressMessages;
    private TextView tvNoMessages;

    private MessagesAdapter adapter;
    private MessagesViewModel vm;

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

        recyclerMessages = view.findViewById(R.id.recyclerMessages);
        progressMessages = view.findViewById(R.id.progressMessages);
        tvNoMessages = view.findViewById(R.id.tvNoMessages);

        recyclerMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MessagesAdapter(this);
        recyclerMessages.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(MessagesViewModel.class);

        vm.loading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            progressMessages.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        vm.messages().observe(getViewLifecycleOwner(), items -> {
            List<MessageItem> safeItems = (items != null) ? items : new ArrayList<>();
            adapter.setItems(safeItems);

            boolean empty = safeItems.isEmpty();
            tvNoMessages.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerMessages.setVisibility(empty ? View.GONE : View.VISIBLE);

            // Optional: quick debug if you're still seeing "RATE_REQUEST" as the whole message
            // If text == type frequently, it's almost certainly a ViewModel mapping bug.
            // (Leave this commented unless needed)
            /*
            for (MessageItem it : safeItems) {
                if (it != null && it.type != null && it.text != null
                        && it.text.trim().equalsIgnoreCase(it.type.trim())) {
                    Toast.makeText(requireContext(),
                            "DEBUG: message text equals type (check ViewModel mapping)",
                            Toast.LENGTH_SHORT).show();
                    break;
                }
            }
            */
        });

        vm.error().observe(getViewLifecycleOwner(), err -> {
            if (!isAdded()) return;
            if (err != null && !err.trim().isEmpty()) {
                Toast.makeText(requireContext(),
                        "Failed to load messages: " + err,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        vm.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        vm.stop();
    }

    @Override
    public void onDismiss(@NonNull MessageItem item) {
        vm.dismiss(item);
    }

    @Override
    public void onPrimary(@NonNull MessageItem item) {
        if (!isAdded()) return;

        String type = normalizeType(item.type);

        if ("RATE_REQUEST".equals(type)) {
            // Must have appointmentId + barberId to actually rate properly
            if (isBlank(item.appointmentId) || isBlank(item.barberId)) {
                Toast.makeText(requireContext(),
                        "Rating message missing appointment/barber info.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            showRatingDialog(item);
            return;
        }

        // Fallback so it never feels "broken"
        Toast.makeText(requireContext(),
                "Open: " + (type.isEmpty() ? "UNKNOWN" : type),
                Toast.LENGTH_SHORT).show();
    }

    private void showRatingDialog(@NonNull MessageItem msg) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_rating, null);

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1f);

        String barber = (msg.barberName == null || msg.barberName.trim().isEmpty())
                ? "your barber"
                : msg.barberName.trim();

        new AlertDialog.Builder(requireContext())
                .setTitle("Rate " + barber)
                .setView(dialogView)
                .setPositiveButton("Submit", (d, w) -> {
                    int rating = (int) ratingBar.getRating();
                    if (rating < 1) {
                        Toast.makeText(requireContext(),
                                "Please select 1–5 stars",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    vm.submitRating(
                            msg,
                            rating,
                            () -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(),
                                        "Thanks for your rating!",
                                        Toast.LENGTH_SHORT).show();
                            },
                            (err) -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(),
                                        "Failed to submit rating: " + err,
                                        Toast.LENGTH_LONG).show();
                            }
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String normalizeType(@Nullable String type) {
        if (type == null) return "";
        return type.trim().toUpperCase();
    }

    private static boolean isBlank(@Nullable String s) {
        return s == null || s.trim().isEmpty();
    }
}
