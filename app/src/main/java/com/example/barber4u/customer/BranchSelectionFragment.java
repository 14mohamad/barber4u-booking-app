package com.example.barber4u.customer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class BranchSelectionFragment extends Fragment implements OnMapReadyCallback {

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

        // Permission launcher (must be created before requesting)
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

        // Attach SupportMapFragment into container
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

        // Enable location (safe)
        requestLocationPermissionIfNeeded();

        // Marker selection
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
            map.setMyLocationEnabled(true); // blue dot
            map.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (SecurityException ignored) {
            // permission might be revoked between check and call
        }
    }

    private void loadBranches() {
        db.collection("branches")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap -> {
                    markerToBranch.clear();

                    // IMPORTANT: don't call map.clear() because it removes the blue-dot layer too.
                    // Instead, just remove markers by tracking them if needed.
                    // Easiest approach: clear then re-enable location.
                    map.clear();
                    enableMyLocationIfPermitted();

                    LatLng first = null;

                    for (QueryDocumentSnapshot doc : snap) {
                        Branch b = doc.toObject(Branch.class);
                        b.setId(doc.getId());

                        // Skip bad coordinates
                        double lat = b.getLat();
                        double lng = b.getLng();
                        if (lat == 0.0 && lng == 0.0) continue;

                        LatLng pos = new LatLng(lat, lng);
                        if (first == null) first = pos;

                        Marker m = map.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(b.getName())
                        );

                        if (m != null) markerToBranch.put(m.getId(), b);
                    }

                    if (first != null) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 12f));
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
