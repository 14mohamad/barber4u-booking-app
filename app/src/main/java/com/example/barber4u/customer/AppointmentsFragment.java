package com.example.barber4u.customer;

import android.location.Address;
import android.location.Geocoder;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppointmentsFragment extends Fragment {

    // UI
    private RecyclerView recyclerView;
    private ProgressBar progress;
    private TextView tvEmpty;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Adapter
    private CustomerAppointmentsAdapter adapter;

    // Cache branchId -> address string
    private final Map<String, String> branchAddressCache = new HashMap<>();

    // Background thread for geocoding
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();

    public AppointmentsFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_appointments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recyclerCustomerAppointments);
        progress = view.findViewById(R.id.progressCustomerAppointments);
        tvEmpty = view.findViewById(R.id.tvEmptyCustomerAppointments);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CustomerAppointmentsAdapter();
        recyclerView.setAdapter(adapter);

        loadAppointmentsForUser();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Avoid leaking the executor if fragment is destroyed
        geocodeExecutor.shutdownNow();
    }

    private void setLoading(boolean isLoading) {
        progress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(boolean show) {
        tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void loadAppointmentsForUser() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in", Toast.LENGTH_SHORT).show();
            showEmpty(true);
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        setLoading(true);
        showEmpty(false);

        // Listen in real-time so status updates show automatically
        db.collection("appointments")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((snapshot, e) -> {
                    setLoading(false);

                    if (e != null) {
                        Toast.makeText(requireContext(),
                                "Failed to load appointments: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        showEmpty(true);
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        adapter.setItems(Collections.emptyList());
                        showEmpty(true);
                        return;
                    }

                    List<CustomerAppointmentItem> items = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String appointmentId = doc.getId();

                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String status = doc.getString("status");

                        String barberName = doc.getString("barberName");
                        String branchName = doc.getString("branchName");
                        String branchId = doc.getString("branchId");

                        CustomerAppointmentItem item = new CustomerAppointmentItem(
                                appointmentId,
                                safe(branchName),
                                safe(barberName),
                                safe(date),
                                safe(time),
                                safe(status),
                                safe(branchId)
                        );

                        // If we already have branch address cached, use it
                        if (!item.branchId.isEmpty() && branchAddressCache.containsKey(item.branchId)) {
                            item.branchAddressEn = branchAddressCache.get(item.branchId);
                        } else if (!item.branchId.isEmpty()) {
                            // Fetch branch doc -> get GeoPoint -> reverse geocode English address
                            fetchBranchAddressEnglish(item.branchId, appointmentId);
                        }

                        items.add(item);
                    }

                    // Optional: sort appointments (e.g., by date string)
                    // If you store timestamps, sorting is better. Keeping as-is for now.

                    adapter.setItems(items);
                    showEmpty(items.isEmpty());
                });
    }

    /**
     * Fetches branch's GeoPoint from Firestore and reverse geocodes to an ENGLISH address.
     * Updates the matching appointment row in the adapter.
     */
    private void fetchBranchAddressEnglish(@NonNull String branchId, @NonNull String appointmentId) {
        db.collection("branches")
                .document(branchId)
                .get()
                .addOnSuccessListener(branchDoc -> {
                    if (!branchDoc.exists()) return;

                    // You said "address is a GeoPoint".
                    // We'll try common field names. Adjust if your field is named differently.
                    GeoPoint gp = branchDoc.getGeoPoint("location");
                    if (gp == null) gp = branchDoc.getGeoPoint("geoPoint");
                    //if (gp == null) gp = branchDoc.getGeoPoint("location");

                    if (gp == null) return;

                    final double lat = gp.getLatitude();
                    final double lng = gp.getLongitude();

                    // Reverse-geocode in the background thread
                    geocodeExecutor.execute(() -> {
                        String addressEn = reverseGeocodeEnglish(lat, lng);

                        // Cache it
                        if (addressEn != null && !addressEn.isEmpty()) {
                            branchAddressCache.put(branchId, addressEn);
                        } else {
                            addressEn = String.format(Locale.ENGLISH, "%.5f, %.5f", lat, lng);
                            branchAddressCache.put(branchId, addressEn);
                        }

                        final String finalAddressEn = addressEn;

                        // Update UI on main thread
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> adapter.updateAddressForAppointment(appointmentId, finalAddressEn));
                        }
                    });
                });
    }

    /**
     * Returns an English address (best effort). Might return null/empty if Geocoder fails.
     */
    private String reverseGeocodeEnglish(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.ENGLISH);
            List<Address> results = geocoder.getFromLocation(lat, lng, 1);
            if (results == null || results.isEmpty()) return null;

            Address a = results.get(0);

            // Build a nice English address line
            // You can tweak formatting here.
            String line = a.getAddressLine(0);
            if (line != null && !line.trim().isEmpty()) return line;

            // Fallback composition
            StringBuilder sb = new StringBuilder();
            if (a.getThoroughfare() != null) sb.append(a.getThoroughfare()).append(" ");
            if (a.getSubThoroughfare() != null) sb.append(a.getSubThoroughfare()).append(", ");
            if (a.getLocality() != null) sb.append(a.getLocality()).append(", ");
            if (a.getCountryName() != null) sb.append(a.getCountryName());
            String built = sb.toString().trim();
            return built.isEmpty() ? null : built;

        } catch (IOException | IllegalArgumentException ex) {
            return null;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // ----------------------------
    // Simple view-model for the list
    // ----------------------------
    static class CustomerAppointmentItem {
        final String appointmentId;
        final String branchName;
        final String barberName;
        final String date;
        final String time;
        final String status;
        final String branchId;

        String branchAddressEn = ""; // filled async

        CustomerAppointmentItem(String appointmentId,
                                String branchName,
                                String barberName,
                                String date,
                                String time,
                                String status,
                                String branchId) {
            this.appointmentId = appointmentId;
            this.branchName = branchName;
            this.barberName = barberName;
            this.date = date;
            this.time = time;
            this.status = status;
            this.branchId = branchId;
        }
    }

    // ----------------------------
    // Minimal RecyclerView Adapter (no extra file needed)
    // ----------------------------
    static class CustomerAppointmentsAdapter extends RecyclerView.Adapter<CustomerAppointmentsAdapter.VH> {

        private final List<CustomerAppointmentItem> items = new ArrayList<>();

        void setItems(@NonNull List<CustomerAppointmentItem> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        void updateAddressForAppointment(@NonNull String appointmentId, @NonNull String addressEn) {
            for (int i = 0; i < items.size(); i++) {
                CustomerAppointmentItem it = items.get(i);
                if (appointmentId.equals(it.appointmentId)) {
                    it.branchAddressEn = addressEn;
                    notifyItemChanged(i);
                    return;
                }
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_customer_appointment, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            CustomerAppointmentItem it = items.get(position);

            h.tvBarber.setText("Barber: " + it.barberName);
            h.tvBranch.setText("Branch: " + it.branchName);

            // English address (may load later)
            if (it.branchAddressEn == null || it.branchAddressEn.isEmpty()) {
                h.tvAddress.setText("Address: Loading...");
            } else {
                h.tvAddress.setText("Address: " + it.branchAddressEn);
            }

            h.tvDateTime.setText("Date: " + it.date + " " + it.time);
            h.tvStatus.setText("Status: " + it.status);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvBarber, tvBranch, tvAddress, tvDateTime, tvStatus;

            VH(@NonNull View itemView) {
                super(itemView);
                tvBarber = itemView.findViewById(R.id.tvCustomerApptBarber);
                tvBranch = itemView.findViewById(R.id.tvCustomerApptBranch);
                tvAddress = itemView.findViewById(R.id.tvCustomerApptAddress);
                tvDateTime = itemView.findViewById(R.id.tvCustomerApptDateTime);
                tvStatus = itemView.findViewById(R.id.tvCustomerApptStatus);
            }
        }
    }
}
