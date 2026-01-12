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

public class BarberDashboardFragment extends Fragment {

    public BarberDashboardFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_barber_dashboard, container, false);

        TextView tv = v.findViewById(R.id.tvBarberWelcome);
        Button btnOpenAddGallery = v.findViewById(R.id.btnOpenAddGallery);

        String name = (getArguments() != null) ? getArguments().getString("userName") : null;
        String email = (getArguments() != null) ? getArguments().getString("userEmail") : null;

        String text = "Welcome Barber";
        if (name != null && !name.trim().isEmpty()) text += "\n" + name;
        if (email != null && !email.trim().isEmpty()) text += "\n" + email;

        tv.setText(text);

        btnOpenAddGallery.setOnClickListener(view -> {
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.roleContainer, new BarberAddGalleryItemFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return v;
    }
}