package com.example.barber4u.customer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private void requestLocationPermissionIfNeeded() {
        boolean fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
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

        boolean fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            try {
                map.setMyLocationEnabled(true); // shows blue dot + “my location” button
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } catch (SecurityException ignored) {}
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSelectedBranchName = view.findViewById(R.id.tvSelectedBranchName);
        btnSelectBranch = view.findViewById(R.id.btnSelectBranch);

        btnSelectBranch.setEnabled(false);

        btnSelectBranch.setOnClickListener(v -> returnSelectedBranch());

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.branch_map_container, mapFragment)
                .commit();
        locationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if ((fine != null && fine) || (coarse != null && coarse)) {
                        enableMyLocationIfPermitted();
                    } else {
                        Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                    }
                });
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

    private void loadBranches() {
        db.collection("branches")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snap -> {
                    markerToBranch.clear();
                    map.clear();

                    LatLng first = null;

                    for (QueryDocumentSnapshot doc : snap) {
                        Branch b = doc.toObject(Branch.class);
                        b.setId(doc.getId());

                        LatLng pos = new LatLng(b.getLat(), b.getLng());
                        if (first == null) first = pos;

                        Marker m = map.addMarker(new MarkerOptions()
                                .position(pos)
                                .title(b.getName())
                        );

                        if (m != null) {
                            markerToBranch.put(m.getId(), b);
                        }
                    }

                    if (first != null) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(first, 12f));
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
