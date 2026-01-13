package com.example.barber4u.customer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber4u.R;
import com.example.barber4u.adapters.GalleryAdapter;
import com.example.barber4u.models.Barber;
import com.example.barber4u.models.GalleryItem;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookFragment extends Fragment {

    // UI
    private TextView tvSelectedBranch;
    private MaterialButton btnChooseBranch;

    private Spinner spBarber;
    private EditText etDate, etTime, etNotes;
    private Button btnBook;
    private ProgressBar progressBook;

    // Gallery
    private RecyclerView rvGallery;
    private GalleryAdapter galleryAdapter;
    private final List<GalleryItem> galleryItems = new ArrayList<>();
    private GalleryItem selectedGalleryItem = null;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Barbers
    private final List<Barber> barberList = new ArrayList<>();
    private ArrayAdapter<Barber> barberAdapter;
    private String lastLoadedBarberId = null;

    // Selected branch from map
    private String selectedBranchId = null;
    private String selectedBranchName = null;

    public BookFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_book, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind UI (matches the rewritten fragment_book.xml)
        tvSelectedBranch = view.findViewById(R.id.tvSelectedBranch);
        btnChooseBranch = view.findViewById(R.id.btnChooseBranch);

        spBarber = view.findViewById(R.id.spBarber);
        etDate = view.findViewById(R.id.etDate);
        etTime = view.findViewById(R.id.etTime);
        etNotes = view.findViewById(R.id.etNotes);
        btnBook = view.findViewById(R.id.btnBook);
        progressBook = view.findViewById(R.id.progressBook);

        rvGallery = view.findViewById(R.id.rvGallery);

        setupBarberAdapter();
        setupPickers();
        setupGallery();
        setupButtons();
        setupBranchResultListener();

        // initial state
        tvSelectedBranch.setText("No branch selected");
        spBarber.setEnabled(false);
        btnBook.setEnabled(false);
    }

    private void setupButtons() {
        btnChooseBranch.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.roleContainer, new BranchSelectionFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnBook.setOnClickListener(v -> createAppointment());
    }

    private void setupBranchResultListener() {
        getParentFragmentManager().setFragmentResultListener(
                "branch_select_result",
                this,
                (requestKey, bundle) -> {
                    selectedBranchId = bundle.getString("branchId");
                    selectedBranchName = bundle.getString("branchName");

                    if (selectedBranchId == null) return;

                    tvSelectedBranch.setText(selectedBranchName != null ? selectedBranchName : "Selected");

                    // Reset dependent UI/data
                    barberList.clear();
                    barberAdapter.notifyDataSetChanged();
                    lastLoadedBarberId = null;
                    clearGallery();

                    spBarber.setEnabled(true);

                    loadBarbersForBranch(selectedBranchId);
                }
        );
    }

    private void setupBarberAdapter() {
        barberAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                barberList
        );
        barberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBarber.setAdapter(barberAdapter);

        spBarber.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= barberList.size()) return;

                Barber barber = barberList.get(position);
                String barberId = barber.getId();
                if (barberId == null) return;

                if (barberId.equals(lastLoadedBarberId)) return;
                lastLoadedBarberId = barberId;

                clearGallery();

                if (selectedBranchId == null) return;
                loadGalleryForBarber(selectedBranchId, barberId);

                updateBookEnabled();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupGallery() {
        rvGallery.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        galleryAdapter = new GalleryAdapter(item -> selectedGalleryItem = item);
        rvGallery.setAdapter(galleryAdapter);
        clearGallery();
    }

    private void clearGallery() {
        galleryItems.clear();
        if (galleryAdapter != null) {
            galleryAdapter.setItems(galleryItems);
            galleryAdapter.clearSelection();
        }
        selectedGalleryItem = null;
    }

    private void loadBarbersForBranch(String branchId) {
        setLoading(true);

        db.collection("barbers")
                .whereArrayContains("branchIds", branchId)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap -> {
                    barberList.clear();

                    for (QueryDocumentSnapshot doc : snap) {
                        Barber barber = doc.toObject(Barber.class);
                        barber.setId(doc.getId());
                        barberList.add(barber);
                    }

                    Collections.sort(barberList, Comparator.comparing(b -> {
                        String n = b.getName();
                        return n == null ? "" : n.toLowerCase(Locale.ROOT);
                    }));

                    barberAdapter.notifyDataSetChanged();
                    setLoading(false);

                    if (barberList.isEmpty()) {
                        Toast.makeText(requireContext(), "No barbers in this branch", Toast.LENGTH_LONG).show();
                    }

                    updateBookEnabled();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(),
                            "Failed to load barbers: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void loadGalleryForBarber(String branchId, String barberId) {
        setLoading(true);

        db.collection("gallery_items")
                .whereEqualTo("active", true)
                .whereEqualTo("branchId", branchId)
                .whereEqualTo("barberId", barberId)
                .get()
                .addOnSuccessListener(snap -> {
                    galleryItems.clear();

                    for (QueryDocumentSnapshot doc : snap) {
                        GalleryItem item = doc.toObject(GalleryItem.class);
                        item.setId(doc.getId());
                        galleryItems.add(item);
                    }

                    Collections.sort(galleryItems, Comparator.comparing(g -> {
                        String t = g.getTitle();
                        return t == null ? "" : t.toLowerCase(Locale.ROOT);
                    }));

                    galleryAdapter.setItems(galleryItems);
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(),
                            "Failed to load gallery: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setupPickers() {
        etDate.setOnClickListener(v -> openDatePicker());
        etTime.setOnClickListener(v -> openTimePicker());
    }

    private void openDatePicker() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (datePicker, y, m, d) -> {
                    String text = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, (m + 1), y);
                    etDate.setText(text);
                    updateBookEnabled();
                },
                year, month, day
        );
        dialog.show();
    }

    private void openTimePicker() {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (TimePicker timePicker, int h, int m) -> {
                    String text = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                    etTime.setText(text);
                    updateBookEnabled();
                },
                hour, minute, true
        );
        dialog.show();
    }

    private void updateBookEnabled() {
        boolean hasBranch = selectedBranchId != null;
        boolean hasBarber = spBarber.getSelectedItem() != null && !barberList.isEmpty();
        boolean hasDate = !TextUtils.isEmpty(etDate.getText().toString().trim());
        boolean hasTime = !TextUtils.isEmpty(etTime.getText().toString().trim());

        btnBook.setEnabled(hasBranch && hasBarber && hasDate && hasTime && progressBook.getVisibility() != View.VISIBLE);
    }

    private void setLoading(boolean isLoading) {
        progressBook.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnChooseBranch.setEnabled(!isLoading);
        spBarber.setEnabled(!isLoading && selectedBranchId != null);
        rvGallery.setEnabled(!isLoading);
        updateBookEnabled();
    }

    private void createAppointment() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Log in to book", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedBranchId == null) {
            Toast.makeText(requireContext(), "Choose a branch first", Toast.LENGTH_SHORT).show();
            return;
        }

        Barber barber = (Barber) spBarber.getSelectedItem();
        if (barber == null) {
            Toast.makeText(requireContext(), "Choose a barber", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (TextUtils.isEmpty(date)) {
            etDate.setError("Required");
            etDate.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(time)) {
            etTime.setError("Required");
            etTime.requestFocus();
            return;
        }

        setLoading(true);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUid());
        data.put("userEmail", user.getEmail());

        data.put("branchId", selectedBranchId);
        data.put("branchName", selectedBranchName);

        data.put("barberId", barber.getId());
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
                .addOnSuccessListener(docRef -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), "Request sent", Toast.LENGTH_LONG).show();

                    etNotes.setText("");
                    etTime.setText("");
                    etDate.setText("");

                    if (galleryAdapter != null) galleryAdapter.clearSelection();
                    selectedGalleryItem = null;

                    updateBookEnabled();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(),
                            "Failed to create appointment: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
