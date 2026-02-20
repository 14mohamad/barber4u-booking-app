package com.example.barber4u.customer.mvvm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.barber4u.models.Barber;
import com.example.barber4u.models.GalleryItem;

import java.util.List;

public class BookViewModel extends ViewModel {

    private static final int WORK_START_HOUR = 10;
    private static final int WORK_END_HOUR = 20;

    private final BookingRepository repo;

    private final MutableLiveData<BookUiState> _state = new MutableLiveData<>(new BookUiState());
    public LiveData<BookUiState> state = _state;

    private final MutableLiveData<Event<UiEvent>> _events = new MutableLiveData<>();
    public LiveData<Event<UiEvent>> events = _events;

    public BookViewModel() {
        repo = new BookingRepository();
        recomputeCanBook();
    }

    // --- helpers ---
    private BookUiState s() { return _state.getValue() == null ? new BookUiState() : _state.getValue(); }
    private void set(BookUiState newState) { _state.setValue(newState); }
    private void toast(String msg) { _events.setValue(new Event<>(UiEvent.toast(msg))); }

    private void setLoading(boolean loading) {
        BookUiState st = s().copy();
        st.loading = loading;
        set(st);
        recomputeCanBook();
    }

    private void recomputeCanBook() {
        BookUiState st = s().copy();
        boolean hasBranch = st.selectedBranchId != null;
        boolean hasBarber = st.selectedBarberId != null;
        boolean hasDate = st.date != null && !st.date.trim().isEmpty();
        boolean hasTime = st.time != null && !st.time.trim().isEmpty();
        st.canBook = hasBranch && hasBarber && hasDate && hasTime && !st.loading;
        set(st);
    }

    // --- inputs from UI ---
    public void onBranchSelected(String branchId, String branchName) {
        if (branchId == null) return;

        BookUiState st = s().copy();
        st.selectedBranchId = branchId;
        st.selectedBranchName = branchName;

        // reset dependent data
        st.barbers.clear();
        st.selectedBarberId = null;
        st.gallery.clear();
        st.selectedGalleryItem = null;

        set(st);
        recomputeCanBook();

        loadBarbers(branchId);
    }

    public void onBarberSelected(Barber barber) {
        if (barber == null || barber.getUid() == null) return;

        BookUiState st = s().copy();
        if (barber.getUid().equals(st.selectedBarberId)) return; // same -> ignore

        st.selectedBarberId = barber.getUid();
        st.gallery.clear();
        st.selectedGalleryItem = null;

        set(st);
        recomputeCanBook();

        if (st.selectedBranchId != null) {
            loadGallery(st.selectedBranchId, barber.getUid());
        }
    }

    public void onGalleryItemSelected(GalleryItem item) {
        BookUiState st = s().copy();
        st.selectedGalleryItem = item;
        set(st);
    }

    public void onDateChanged(String date) {
        BookUiState st = s().copy();
        st.date = date == null ? "" : date.trim();
        set(st);
        recomputeCanBook();
    }

    public void onTimeChanged(String time) {
        BookUiState st = s().copy();
        st.time = time == null ? "" : time.trim();
        set(st);
        recomputeCanBook();
    }

    public void onNotesChanged(String notes) {
        BookUiState st = s().copy();
        st.notes = notes == null ? "" : notes;
        set(st);
    }

    // --- loading ---
    private void loadBarbers(String branchId) {
        setLoading(true);
        repo.loadBarbersForBranch(branchId, new BookingRepository.ResultCallback<List<Barber>>() {
            @Override
            public void onSuccess(List<Barber> data) {
                BookUiState st = s().copy();
                st.barbers = data;
                set(st);
                setLoading(false);
                if (data.isEmpty()) toast("No barbers in this branch");
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                toast("Failed to load barbers: " + e.getMessage());
            }
        });
    }

    private void loadGallery(String branchId, String barberId) {
        setLoading(true);
        repo.loadGalleryForBarber(branchId, barberId, new BookingRepository.ResultCallback<List<GalleryItem>>() {
            @Override
            public void onSuccess(List<GalleryItem> data) {
                BookUiState st = s().copy();
                st.gallery = data;
                set(st);
                setLoading(false);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                toast("Failed to load gallery: " + e.getMessage());
            }
        });
    }

    // --- validation ---
    public boolean isWorkHourValid(String time) {
        if (time == null) return false;
        String[] parts = time.split(":");
        if (parts.length != 2) return false;

        try {
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);

            return (h > WORK_START_HOUR && h < WORK_END_HOUR)
                    || (h == WORK_START_HOUR)
                    || (h == WORK_END_HOUR && m == 0);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // --- booking ---
    public void book() {
        BookUiState st = s();

        if (repo.getCurrentUser() == null) { toast("Log in to book"); return; }
        if (st.selectedBranchId == null) { toast("Choose a branch first"); return; }
        if (st.selectedBarberId == null) { toast("Choose a barber"); return; }
        if (st.date == null || st.date.trim().isEmpty()) { toast("Date is required"); return; }
        if (st.time == null || st.time.trim().isEmpty()) { toast("Time is required"); return; }
        if (!isWorkHourValid(st.time)) { toast("השעה חייבת להיות בין 10:00 ל-20:00"); return; }

        Barber selected = null;
        for (Barber b : st.barbers) {
            if (b != null && b.getUid() != null && b.getUid().equals(st.selectedBarberId)) {
                selected = b; break;
            }
        }
        if (selected == null) { toast("Choose a barber"); return; }

        setLoading(true);

        repo.createAppointment(
                st.selectedBranchId,
                st.selectedBranchName,
                selected,
                st.date.trim(),
                st.time.trim(),
                st.notes == null ? "" : st.notes.trim(),
                st.selectedGalleryItem,
                new BookingRepository.ResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        setLoading(false);
                        toast("Request sent");

                        BookUiState ns = s().copy();
                        ns.notes = "";
                        ns.date = "";
                        ns.time = "";
                        ns.selectedGalleryItem = null;
                        set(ns);
                        recomputeCanBook();
                    }

                    @Override
                    public void onError(Exception e) {
                        setLoading(false);
                        toast("Failed to create appointment: " + e.getMessage());
                    }
                }
        );
    }
}