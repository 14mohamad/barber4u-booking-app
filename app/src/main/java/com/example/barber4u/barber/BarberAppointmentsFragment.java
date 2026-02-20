package com.example.barber4u.barber;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber4u.R;
import com.example.barber4u.adapters.BarberAppointmentsAdapter;
import com.example.barber4u.barber.mvvm.BarberAppointmentsUiState;
import com.example.barber4u.barber.mvvm.BarberAppointmentsViewModel;
import com.example.barber4u.customer.mvvm.UiEvent;
import com.example.barber4u.models.Appointment;

import java.util.ArrayList;

public class BarberAppointmentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progress;
    private TextView tvEmpty;

    private BarberAppointmentsAdapter adapter;
    private BarberAppointmentsViewModel vm;

    public BarberAppointmentsFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_barber_appointments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(this).get(BarberAppointmentsViewModel.class);

        recyclerView = view.findViewById(R.id.recyclerBarberAppointments);
        progress = view.findViewById(R.id.progressBarberAppointments);
        tvEmpty = view.findViewById(R.id.tvEmptyBarberAppointments);

        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        adapter = new BarberAppointmentsAdapter(new BarberAppointmentsAdapter.Listener() {
            @Override
            public void onApprove(@NonNull Appointment appt) {
                vm.approve(appt);
            }

            @Override
            public void onCancel(@NonNull Appointment appt) {
                vm.cancel(appt);
            }

            @Override
            public void onDone(@NonNull Appointment appt) {
                vm.done(appt);
            }
        });

        recyclerView.setAdapter(adapter);

        adapter.setItems(new ArrayList<>());
        renderEmpty(true);
        renderLoading(false);

        observe();
    }

    private void observe() {
        vm.state.observe(getViewLifecycleOwner(), this::render);

        vm.events.observe(getViewLifecycleOwner(), ev -> {
            if (ev == null) return;
            UiEvent e = ev.getContentIfNotHandled();
            if (e == null) return;

            if (e.type == UiEvent.Type.TOAST) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void render(BarberAppointmentsUiState s) {
        renderLoading(s.loading);
        adapter.setItems(s.items);
        renderEmpty(s.showEmpty);
    }

    private void renderLoading(boolean loading) {
        if (progress != null) progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void renderEmpty(boolean show) {
        if (tvEmpty != null) tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
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
}