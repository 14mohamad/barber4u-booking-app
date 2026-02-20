package com.example.barber4u.barber.mvvm;

import com.example.barber4u.models.Appointment;

import java.util.ArrayList;
import java.util.List;

public class BarberAppointmentsUiState {
    public boolean loading = false;
    public List<Appointment> items = new ArrayList<>();
    public boolean showEmpty = true;

    public BarberAppointmentsUiState copy() {
        BarberAppointmentsUiState s = new BarberAppointmentsUiState();
        s.loading = this.loading;
        s.items = new ArrayList<>(this.items);
        s.showEmpty = this.showEmpty;
        return s;
    }
}