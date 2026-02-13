package com.example.barber4u.customer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.barber4u.R;
import com.example.barber4u.models.Branch;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class BranchSelectionFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "BranchSelection";

    private GoogleMap map;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private final Map<String, Branch> markerToBranch = new HashMap<>();
    private Branch selectedBranch;

    private TextView tvSelectedBranchName;
    private MaterialButton btnSelectBranch;

    public BranchSelectionFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_branch_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSelectedBranchName = view.findViewById(R.id.tvSelectedBranchName);
        btnSelectBranch = view.findViewById(R.id.btnSelectBranch);

        btnSelectBranch.setEnabled(false);
        btnSelectBranch.setOnClickListener(v -> returnSelectedBranch());

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                    if (fine || coarse) {
                        enableMyLocationIfPermitted();
                    } else {
                        Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.branch_map_container, mapFragment)
                .commit();

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        requestLocationPermissionIfNeeded();

        map.setOnMarkerClickListener(marker -> {
            selectedBranch = markerToBranch.get(marker.getId());
            if (selectedBranch != null) {
                tvSelectedBranchName.setText(selectedBranch.getName());
                btnSelectBranch.setEnabled(true);
                marker.showInfoWindow();
            }
            return false;
        });

        loadBranches();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissionIfNeeded() {
        if (map == null) return;

        if (hasLocationPermission()) {
            enableMyLocationIfPermitted();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void enableMyLocationIfPermitted() {
        if (map == null) return;
        if (!hasLocationPermission()) return;

        try {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (SecurityException ignored) {}
    }

    private void loadBranches() {
        db.collection("branches")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap -> {
                    markerToBranch.clear();

                    map.clear();
                    enableMyLocationIfPermitted();

                    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                    int markerCount = 0;

                    Log.d(TAG, "branches fetched: " + snap.size());

                    for (QueryDocumentSnapshot doc : snap) {
                        Branch b = doc.toObject(Branch.class);
                        b.setId(doc.getId());

                        // ✅ Preferred: GeoPoint field named "location"
                        GeoPoint gp = doc.getGeoPoint("location");
                        double lat;
                        double lng;

                        if (gp != null) {
                            lat = gp.getLatitude();
                            lng = gp.getLongitude();
                        } else {
                            // Fallback if some docs still store lat/lng as doubles
                            lat = b.getLat();
                            lng = b.getLng();
                        }

                        // Skip invalid coords
                        if (lat == 0.0 && lng == 0.0) {
                            Log.w(TAG, "Skipping branch (missing coords): " + doc.getId()
                                    + " name=" + b.getName()
                                    + " lat=" + lat + " lng=" + lng);
                            continue;
                        }

                        LatLng pos = new LatLng(lat, lng);

                        Marker m = map.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(b.getName())
                        );

                        if (m != null) {
                            markerToBranch.put(m.getId(), b);
                            boundsBuilder.include(pos);
                            markerCount++;
                        }
                    }

                    if (markerCount > 0) {
                        // ✅ Show ALL branches on screen
                        try {
                            map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
                        } catch (Exception e) {
                            // If only one marker, bounds can sometimes throw on some devices
                            // fallback to a simple zoom
                            LatLng first = markerToBranch.isEmpty()
                                    ? new LatLng(32.0853, 34.7818) // default (Israel-ish)
                                    : map.getCameraPosition().target;

                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 12f));
                        }
                    } else {
                        Toast.makeText(requireContext(), "No valid branch locations found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Failed to load branches: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void returnSelectedBranch() {
        if (selectedBranch == null) {
            Toast.makeText(requireContext(), "Choose a branch", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle result = new Bundle();
        result.putString("branchId", selectedBranch.getId());
        result.putString("branchName", selectedBranch.getName());

        getParentFragmentManager().setFragmentResult("branch_select_result", result);
        getParentFragmentManager().popBackStack();
    }
}