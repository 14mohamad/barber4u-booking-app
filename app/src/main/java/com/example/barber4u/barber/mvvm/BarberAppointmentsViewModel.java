package com.example.barber4u.barber.mvvm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.barber4u.customer.mvvm.Event;
import com.example.barber4u.customer.mvvm.UiEvent;
import com.example.barber4u.models.Appointment;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BarberAppointmentsViewModel extends ViewModel {

    private final BarberAppointmentsRepository repo = new BarberAppointmentsRepository();
    private ListenerRegistration listener;

    private final MutableLiveData<BarberAppointmentsUiState> _state =
            new MutableLiveData<>(new BarberAppointmentsUiState());
    public LiveData<BarberAppointmentsUiState> state = _state;

    private final MutableLiveData<Event<UiEvent>> _events = new MutableLiveData<>();
    public LiveData<Event<UiEvent>> events = _events;

    private BarberAppointmentsUiState s() {
        return _state.getValue() == null ? new BarberAppointmentsUiState() : _state.getValue();
    }

    private void set(BarberAppointmentsUiState st) {
        _state.setValue(st);
    }

    private void toast(String msg) {
        _events.setValue(new Event<>(UiEvent.toast(msg)));
    }

    private void setLoading(boolean loading) {
        BarberAppointmentsUiState st = s().copy();
        st.loading = loading;
        set(st);
    }

    public void start() {
        String barberUid = repo.getCurrentBarberUid();
        if (barberUid == null) {
            BarberAppointmentsUiState st = s().copy();
            st.items = new ArrayList<>();
            st.showEmpty = true;
            st.loading = false;
            set(st);
            toast("Barber not logged in");
            return;
        }

        stop(); // ensure no duplicate listeners

        setLoading(true);
        listener = repo.listenAppointments(barberUid, new BarberAppointmentsRepository.AppointmentsCallback() {
            @Override
            public void onData(List<Appointment> items) {
                setLoading(false);

                // apply same filtering rules as your fragment
                List<Appointment> filtered = new ArrayList<>();
                for (Appointment a : items) {
                    String status = safe(a.getStatus());
                    String date = safe(a.getDate());

                    // 1) Never show DONE
                    if (status.equalsIgnoreCase("DONE")) continue;

                    // 2) Hide past appointments that are still pending/approved
                    if ((status.equalsIgnoreCase("PENDING") || status.equalsIgnoreCase("APPROVED"))
                            && !isTodayOrFuture(date)) {
                        continue;
                    }

                    filtered.add(a);
                }

                BarberAppointmentsUiState st = s().copy();
                st.items = filtered;
                st.showEmpty = filtered.isEmpty();
                set(st);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                BarberAppointmentsUiState st = s().copy();
                st.items = new ArrayList<>();
                st.showEmpty = true;
                set(st);
                toast("Failed to load appointments: " + e.getMessage());
            }
        });
    }

    public void stop() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    public void approve(Appointment appt) {
        updateStatus(appt, "APPROVED");
    }

    public void cancel(Appointment appt) {
        updateStatus(appt, "CANCELED");
    }

    public void done(Appointment appt) {
        // IMPORTANT: we update DB; UI will refresh via snapshot listener
        updateStatus(appt, "DONE");
    }

    private void updateStatus(Appointment appt, String status) {
        if (appt == null) return;
        String id = appt.getId();
        if (id == null || id.trim().isEmpty()) return;

        repo.updateStatus(id, status, new BarberAppointmentsRepository.SimpleCallback() {
            @Override
            public void onSuccess() { /* no toast needed */ }

            @Override
            public void onError(Exception e) {
                toast("Failed to update status: " + e.getMessage());
            }
        });
    }

    private boolean isTodayOrFuture(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setLenient(false);

            Date apptDate = sdf.parse(dateStr);
            if (apptDate == null) return false;

            Calendar apptCal = Calendar.getInstance();
            apptCal.setTime(apptDate);
            apptCal.set(Calendar.HOUR_OF_DAY, 0);
            apptCal.set(Calendar.MINUTE, 0);
            apptCal.set(Calendar.SECOND, 0);
            apptCal.set(Calendar.MILLISECOND, 0);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            return !apptCal.before(today);
        } catch (Exception e) {
            return false;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    @Override
    protected void onCleared() {
        stop();
        super.onCleared();
    }
}