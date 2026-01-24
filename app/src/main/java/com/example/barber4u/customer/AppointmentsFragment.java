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
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    // Cache branchId -> english address
    private final Map<String, String> branchAddressCache = new HashMap<>();

    // Background thread for geocoding (keep for fragment lifetime)
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();

    // Firestore listener registration (MUST remove)
    private ListenerRegistration appointmentsListener;

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

        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        adapter = new CustomerAppointmentsAdapter();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        geocodeExecutor.shutdownNow();
    }

    private void startListeningAppointments() {
        if (auth.getCurrentUser() == null) {
            if (isAdded()) Toast.makeText(getContext(), "Please log in", Toast.LENGTH_SHORT).show();
            showEmpty(true);
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        stopListeningAppointments(); // ensure no duplicate listeners

        setLoading(true);
        showEmpty(false);

        appointmentsListener = db.collection("appointments")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (!isAdded()) return; // <-- CRITICAL: fragment may be detached

                    setLoading(false);

                    if (e != null) {
                        Toast.makeText(getContext(),
                                "Failed to load appointments: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        adapter.setItems(Collections.emptyList());
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

                        String date = safe(doc.getString("date"));
                        if (date.isEmpty() || !isTodayOrFuture(date)) {
                            continue;
                        }
                        String time = safe(doc.getString("time"));
                        String status = safe(doc.getString("status"));

                        String barberName = safe(doc.getString("barberName"));
                        String branchName = safe(doc.getString("branchName"));
                        String branchId = safe(doc.getString("branchId"));

                        CustomerAppointmentItem item = new CustomerAppointmentItem(
                                appointmentId,
                                branchName,
                                barberName,
                                date,
                                time,
                                status,
                                branchId
                        );

                        if (!branchId.isEmpty()) {
                            String cached = branchAddressCache.get(branchId);
                            if (cached != null && !cached.isEmpty()) {
                                item.branchAddressEn = cached;
                            } else {
                                // fetch+geocode async, update adapter row later
                                fetchBranchAddressEnglish(branchId, appointmentId);
                            }
                        }

                        items.add(item);
                    }

                    adapter.setItems(items);
                    showEmpty(items.isEmpty());
                });
    }

    private void stopListeningAppointments() {
        if (appointmentsListener != null) {
            appointmentsListener.remove();
            appointmentsListener = null;
        }
    }

    private void setLoading(boolean isLoading) {
        if (progress == null) return;
        progress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(boolean show) {
        if (tvEmpty != null) tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    /**
     * Fetch branch GeoPoint from Firestore, reverse-geocode to ENGLISH address, cache + update list row.
     * IMPORTANT: No requireContext() inside background thread.
     */
    private void fetchBranchAddressEnglish(@NonNull String branchId, @NonNull String appointmentId) {
        db.collection("branches")
                .document(branchId)
                .get()
                .addOnSuccessListener(branchDoc -> {
                    if (!branchDoc.exists()) return;

                    // Try common field names; adjust to your real field
                    GeoPoint gp = branchDoc.getGeoPoint("location");
                    if (gp == null) gp = branchDoc.getGeoPoint("geoPoint");
                    if (gp == null) gp = branchDoc.getGeoPoint("address"); // if you literally named it "address"

                    if (gp == null) return;

                    final double lat = gp.getLatitude();
                    final double lng = gp.getLongitude();

                    // Capture a SAFE context reference for Geocoder (must be on main thread)
                    final android.content.Context ctx = getContext();
                    if (ctx == null) return;

                    geocodeExecutor.execute(() -> {
                        String addressEn = reverseGeocodeEnglish(ctx, lat, lng);

                        if (addressEn == null || addressEn.isEmpty()) {
                            addressEn = String.format(Locale.ENGLISH, "%.5f, %.5f", lat, lng);
                        }

                        branchAddressCache.put(branchId, addressEn);

                        // Back to UI thread safely
                        if (isAdded() && getActivity() != null) {
                            String finalAddressEn = addressEn;
                            getActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                adapter.updateAddressForAppointment(appointmentId, finalAddressEn);
                            });
                        }
                    });
                });
    }

    /**
     * Best effort English address.
     */
    private String reverseGeocodeEnglish(@NonNull android.content.Context ctx, double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(ctx, Locale.ENGLISH);
            List<Address> results = geocoder.getFromLocation(lat, lng, 1);
            if (results == null || results.isEmpty()) return null;

            Address a = results.get(0);
            String line = a.getAddressLine(0);
            if (line != null && !line.trim().isEmpty()) return line;

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
    // View-model
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
    private boolean isTodayOrFuture(@NonNull String dateStr) {
        try {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            Date appointmentDate = sdf.parse(dateStr);
            if (appointmentDate == null) return false;

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            return !appointmentDate.before(today.getTime());

        } catch (ParseException e) {
            return false; // invalid date format → hide
        }
    }



    // ----------------------------
    // Inline RecyclerView Adapter
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

            String addr = it.branchAddressEn;
            h.tvAddress.setText((addr == null || addr.isEmpty())
                    ? "Address: Loading..."
                    : "Address: " + addr);

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
